﻿using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor
{
    public partial class FormError : Form
    {
        public FormMain FormMain { get; set; }

        public FormError()
        {
            InitializeComponent();
        }

        private void FormError_FormClosing(object sender, FormClosingEventArgs e)
        {
            e.Cancel = true;
            Hide();
        }

        private void FormError_Load(object sender, EventArgs e)
        {
            if (FormMain.ConfigEditor.FormErrorLocation != null)
                this.Location = FormMain.ConfigEditor.FormErrorLocation;
            if (FormMain.ConfigEditor.FormErrorSize != null)
                this.Size = FormMain.ConfigEditor.FormErrorSize;
            this.WindowState = FormMain.ConfigEditor.FormErrorState;

        }

        public class Error
        {
            public Property.ErrorLevel Level { get; set; }
            public string Description { get; set; }
            public DataGridViewRow Row { get; set; }
        }

        private class IdentityEqualityComparer : IEqualityComparer<object>
        {
            bool IEqualityComparer<object>.Equals(object x, object y)
            {
                return object.ReferenceEquals(x, y);
            }

            int IEqualityComparer<object>.GetHashCode(object obj)
            {
                return System.Runtime.CompilerServices.RuntimeHelpers.GetHashCode(obj);
            }
        }

        private Dictionary<DataGridViewCell, SortedDictionary<string, Error>> Errors
            = new Dictionary<DataGridViewCell, SortedDictionary<string, Error>>(new IdentityEqualityComparer());

        public void AddError(HashSet<DataGridViewCell> cells, Property.IProperty p, Property.ErrorLevel level, string desc)
        {
            foreach (var cell in cells)
                AddError(cell, p, level, desc);
        }

        public void AddError(DataGridViewCell cell, Property.IProperty p, Property.ErrorLevel level, string desc)
        {
            if (false == Errors.TryGetValue(cell, out var errors))
                Errors.Add(cell, errors = new SortedDictionary<string, Error>());

            if (errors.ContainsKey(p.Name)) // 同一个cell相同的prop只报告一次。
                return;

            grid.Rows.Add();
            DataGridViewRow row = grid.Rows[grid.RowCount - 1];
            row.Cells["Level"].Value = System.Enum.GetName(typeof(Property.ErrorLevel), level);
            row.Cells["Level"].Tag = cell;
            row.Cells["Description"].Value = desc;
            row.Cells["File"].Value = cell.DataGridView.Parent.Text;

            errors.Add(p.Name, new Error() { Level = level, Description = desc, Row = row, });
            UpdateErrorCell(cell, errors);
        }

        public void RemoveError(DataGridViewCell cell, Property.IProperty p)
        {
            if (false == Errors.TryGetValue(cell, out var errors))
                return;

            if (false == errors.TryGetValue(p.Name, out var error))
                return;

            errors.Remove(p.Name);
            if (errors.Count == 0)
            {
                Errors.Remove(cell);
                cell.Style.BackColor = Color.White;
                cell.ToolTipText = null;
                grid.Rows.Remove(error.Row);
            }
            else
            {
                UpdateErrorCell(cell, errors);
            }
        }

        private void UpdateErrorCell(DataGridViewCell cell, SortedDictionary<string, Error> errors)
        {
            Property.ErrorLevel max = Property.ErrorLevel.Warn;
            StringBuilder sb = new StringBuilder();
            foreach (var e in errors)
            {
                sb.Append(e.Key).Append(": ").Append(e.Value.Description).Append(Environment.NewLine);
                if (e.Value.Level > max)
                    max = e.Value.Level;
            }
            cell.Style.BackColor = max == Property.ErrorLevel.Error ? Color.Red : Color.Yellow;
            cell.ToolTipText = sb.ToString();
        }

        private void grid_CellDoubleClick(object sender, DataGridViewCellEventArgs e)
        {
            if (e.RowIndex < 0)
                return;

            DataGridViewCell maincell = grid.Rows[e.RowIndex].Cells["Level"].Tag as DataGridViewCell;
            DataGridView maingrid = maincell.DataGridView;
            FormMain.Tabs.SelectedTab = maingrid.Parent as TabPage;
            maingrid.FirstDisplayedCell = maincell;
            maingrid.CurrentCell = maincell;
        }
    }
}
