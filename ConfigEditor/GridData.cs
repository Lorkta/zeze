﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor
{
    public class GridData
    {
        /// <summary>
        /// DataGridView 同步不能在后台线程中调用。
        /// 必须在把控制权交给 UI 时设置 DataGridView。
        /// 一般流程是后台线程装载初始化 GridData，准备好以后，交给 UI。
        /// UI 创建 DataGridView 并和 GridData 建立关联。
        /// 自此之后的 GridData 操作就会和 DataGridView 同步。
        /// 但是就不能在其他线程再访问了。
        /// </summary>

        public DataGridView View { get; set; }
        public Document Document { get; }
        public GridData(Document doc)
        {
            Document = doc;
        }

        public class Column
        {
            public string HeaderText { get; set; }
            public string ToolTipText { get; set; }
            public bool ReadOnly { get; set; }
            public ColumnTag ColumnTag { get; set; }
        }

        public class Row
        {
            public GridData GridData { get; }
            public List<Cell> Cells { get; } = new List<Cell>();

            public Row(GridData parent)
            {
                GridData = parent;
                for (int col = 0; col < GridData.ColumnCount; ++col)
                    Cells.Add(new Cell(this));
            }
        }

        public class Cell
        {
            public string Value { get; set; } = "";
            public System.Drawing.Color BackColor { get; set; } = System.Drawing.Color.White;
            public string ToolTipText { get; set; }
            public Row Row { get; }


            /// <summary>
            /// 查找的到cell坐标，仅在上下文得不到坐标时。
            /// </summary>
            public void Invalidate()
            {
                if (null != Row.GridData.View)
                {
                    int col = Row.Cells.IndexOf(this);
                    int row = Row.GridData.IndexOfRow(Row);
                    Row.GridData.View.InvalidateCell(col, row);
                }
            }

            public void Show()
            {
                if (null == Row.GridData.View)
                {
                    FormMain.Instance.OpenGrid(Row.GridData.Document, true);
                }
                FormMain.Instance.Tabs.SelectedTab = Row.GridData.View.Parent as TabPage;
                int col = Row.Cells.IndexOf(this);
                int row = Row.GridData.IndexOfRow(Row);
                var cellView = Row.GridData.View.Rows[row].Cells[col];
                Row.GridData.View.FirstDisplayedCell = cellView;
                Row.GridData.View.CurrentCell = cellView;
            }

            public Cell(Row row)
            {
                Row = row;
            }
        }

        private List<Column> Columns = new List<Column>();
        private List<Row> Rows = new List<Row>();

        public int IndexOfRow(Row row)
        {
            return Rows.IndexOf(row);
        }

        public void SyncToView()
        {
            View.SuspendLayout();
            View.Columns.Clear();
            View.Rows.Clear();
            for (int i = 0; i < Columns.Count; ++i)
            {
                InsertColumnToView(i, Columns[i]);
            }
            // 多加一行，用来添加数据，需要在OnCellValueNeeded,OnCellValuePushed中特殊处理。
            View.RowCount = Rows.Count + 1;
            View.ResumeLayout();
        }

        private void InsertColumnToView(int columnIndex, Column column)
        {
            // 在 Column 里面定义 Width，构造的时候初始化，就不需要下面的处理了。
            // 但是这样在 Width 改变的时候需要同步数据，而且也多定义了一份。
            // 所以这里特殊处理一下。反正这个代码也不是拿来重用的。

            int width = 80;
            if (null != column.ColumnTag)
            {
                switch (column.ColumnTag.Tag)
                {
                    case ColumnTag.ETag.AddVariable:
                    case ColumnTag.ETag.ListStart:
                    case ColumnTag.ETag.ListEnd:
                        width = 20;
                        break;
                    default:
                        width = column.ColumnTag.PathLast.Define.GridColumnValueWidth;
                        break;
                }
            }

            View?.Columns.Insert(columnIndex,
                new DataGridViewColumn(new DataGridViewTextBoxCell())
                {
                    Width = width,
                    HeaderText = column.HeaderText,
                    ReadOnly = column.ReadOnly,
                    ToolTipText = column.ToolTipText,
                    Tag = column.ColumnTag,
                    Frozen = false,
                    AutoSizeMode = DataGridViewAutoSizeColumnMode.None,
                });
        }

        public int ColumnCount => Columns.Count;
        public int RowCount => Rows.Count;

        public Column GetColumn(int columnIndex)
        {
            return Columns[columnIndex];
        }

        public void InsertColumn(int columnIndex, Column column)
        {
            Columns.Insert(columnIndex, column);
            foreach (var row in Rows)
            {
                row.Cells.Insert(columnIndex, new Cell(row));
            }
            InsertColumnToView(columnIndex, column);
        }

        public void RemoveColumn(int columnIndex)
        {
            foreach (var row in Rows)
            {
                row.Cells.RemoveAt(columnIndex);
            }
            Columns.RemoveAt(columnIndex);
            View?.Columns.RemoveAt(columnIndex);
        }

        public void InsertRow(int rowIndex, bool syncToView = false)
        {
            var row = new Row(this);
            Rows.Insert(rowIndex, row);

            // SetSpecialColumnText
            for (int colIndex = 0; colIndex < ColumnCount; ++colIndex)
            {
                Column col = Columns[colIndex];
                switch (col.ColumnTag.Tag)
                {
                    case ColumnTag.ETag.AddVariable:
                        row.Cells[colIndex].Value = ",";
                        break;
                    case ColumnTag.ETag.ListStart:
                        row.Cells[colIndex].Value = "[";
                        break;
                    case ColumnTag.ETag.ListEnd:
                        row.Cells[colIndex].Value = "]";
                        break;
                }
            }

            if (syncToView)
                View?.Rows.Insert(rowIndex, 1);
        }

        public void BuildUniqueIndexOnAddRow(int rowIndex)
        {
            var row = Rows[rowIndex];
            for (int i = 0; i < ColumnCount; ++i)
            {
                ColumnTag tag = Columns[i].ColumnTag;
                switch (tag.Tag)
                {
                    case ColumnTag.ETag.AddVariable:
                    case ColumnTag.ETag.ListStart:
                    case ColumnTag.ETag.ListEnd:
                        continue;
                }
                var cell = row.Cells[i];
                tag.AddUniqueIndex(cell.Value, cell);
            }
        }

        public void RemoveRow(int rowIndex)
        {
            Rows.RemoveAt(rowIndex);
            View?.Rows.RemoveAt(rowIndex);
        }

        public Row GetRow(int rowIndex)
        {
            return Rows[rowIndex];
        }

        public Cell GetCell(int columnIndex, int rowIndex)
        {
            return Rows[rowIndex].Cells[columnIndex];
        }

        public static int FindColumnListEnd(GridData grid, int startColIndex)
        {
            int skipNestListCount = 0;
            for (int c = startColIndex; c < grid.ColumnCount; ++c)
            {
                ColumnTag tag = grid.GetColumn(c).ColumnTag;
                if (skipNestListCount > 0)
                {
                    switch (tag.Tag)
                    {
                        case ColumnTag.ETag.ListEnd:
                            --skipNestListCount;
                            break;
                        case ColumnTag.ETag.ListStart:
                            ++skipNestListCount;
                            break;
                    }
                    continue;
                }
                switch (tag.Tag)
                {
                    case ColumnTag.ETag.ListStart:
                        ++skipNestListCount;
                        break;
                    //case ColumnTag.ETag.AddVariable:
                    //case ColumnTag.ETag.Normal:
                    //    break;
                    case ColumnTag.ETag.ListEnd:
                        return c;
                }
            }
            return -1;
        }

        public void VerifyAll(bool RemoveError)
        {
            try
            {
                if (RemoveError) // 优化，如果外面上下文已经知道重建了 GridData，就需要再次清除了。
                    FormMain.Instance.FormError.RemoveErrorByGrid(this);

                for (int rowIndex = 0; rowIndex < RowCount; ++rowIndex)
                {
                    for (int colIndex = 0; colIndex < ColumnCount; ++colIndex)
                    {
                        ColumnTag tag = Columns[colIndex].ColumnTag;

                        if (tag.Tag != ColumnTag.ETag.Normal)
                            continue;

                        Cell cell = GetCell(colIndex, rowIndex);
                        var param = new Property.VerifyParam()
                        {
                            FormMain = FormMain.Instance,
                            Grid = this,
                            ColumnIndex = colIndex,
                            RowIndex = rowIndex,
                            ColumnTag = tag,
                            NewValue = cell.Value,
                        };

                        foreach (var p in tag.PathLast.Define.PropertiesList)
                        {
                            p.VerifyCell(param);
                        }
                        tag.PathLast.Define.Verify(param);
                    }
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show(ex.ToString());
            }
        }

        public void UpdateWhenAddVariable(VarDefine var)
        {
            for (int c = 0; c < ColumnCount; ++c)
            {
                ColumnTag tagref = Columns[c].ColumnTag;
                if (tagref.Tag == ColumnTag.ETag.AddVariable && tagref.PathLast.Define.Parent == var.Parent)
                {
                    c += var.BuildGridColumns(this, c, tagref.Parent(ColumnTag.ETag.Normal), -1);

                    // 如果是List，第一次加入的时候，默认创建一个Item列。
                    // 但是仍然有问题：如果这个Item没有输入数据，下一次打开时，不会默认创建。需要手动增加Item。
                    if (var.Type == VarDefine.EType.List)
                    {
                        ColumnTag tagListEnd = Columns[c - 1].ColumnTag;
                        ColumnTag tagListEndCopy = tagListEnd.Copy(ColumnTag.ETag.Normal);
                        tagListEndCopy.PathLast.ListIndex = -tagListEnd.PathLast.ListIndex; // 肯定是0，保险写法。
                        --tagListEnd.PathLast.ListIndex;
                        c += var.Reference.BuildGridColumns(this, c - 1, tagListEndCopy, -1);
                    }
                    //((Document)gridref.Tag).IsChanged = true; // 引用的Grid仅更新界面，数据实际上没有改变。
                }
            }
        }

        public void DeleteVariable(VarDefine var)
        {
            View?.SuspendLayout();
            var updateParam = new Bean.UpdateParam() { UpdateType = Bean.EUpdate.DeleteData }; // never change
            for (int c = 0; c < ColumnCount; ++c)
            {
                ColumnTag tagref = Columns[c].ColumnTag;
                if (tagref.PathLast.Define == var)
                {
                    // delete data
                    for (int r = 0; r < RowCount; ++r)
                    {
                        var row = Rows[r];
                        int colref = c;
                        Document.Beans[r].Update(this, row, ref colref, 0, updateParam);
                    }
                    // delete columns
                    switch (tagref.Tag)
                    {
                        case ColumnTag.ETag.Normal:
                            this.RemoveColumn(c);
                            --c;
                            break;
                        case ColumnTag.ETag.ListStart:
                            int colListEnd = GridData.FindCloseListEnd(this, c);
                            while (colListEnd >= c)
                            {
                                RemoveColumn(colListEnd);
                                --colListEnd;
                            }
                            --c;
                            break;
                        default:
                            MessageBox.Show("ListEnd?");
                            break;
                    }
                }
            }
            View?.ResumeLayout();
        }

        public static int FindCloseListEnd(GridData grid, int startColIndex)
        {
            int listStartCount = 1;
            for (int c = startColIndex + 1; c < grid.ColumnCount; ++c)
            {
                ColumnTag tag = grid.GetColumn(c).ColumnTag;
                switch (tag.Tag)
                {
                    case ColumnTag.ETag.ListEnd:
                        --listStartCount;
                        if (listStartCount == 0)
                            return c;
                        break;
                    case ColumnTag.ETag.ListStart:
                        ++listStartCount;
                        break;
                }
            }
            throw new Exception("List Not Closed.");
        }

        public static int FindColumnListStart(GridData grid, int startColIndex)
        {
            int skipNestListCount = 0;
            for (int c = startColIndex; c >= 0; --c)
            {
                ColumnTag tag = grid.GetColumn(c).ColumnTag;
                if (skipNestListCount > 0)
                {
                    switch (tag.Tag)
                    {
                        case ColumnTag.ETag.ListEnd:
                            ++skipNestListCount;
                            break;
                        case ColumnTag.ETag.ListStart:
                            --skipNestListCount;
                            break;
                    }
                    continue;
                }
                switch (tag.Tag)
                {
                    case ColumnTag.ETag.ListStart:
                        return c;

                    case ColumnTag.ETag.ListEnd:
                        ++skipNestListCount;
                        break;
                }
            }
            return -1;
        }


        public static int FindColumnBeanBegin(GridData grid, int startColIndex)
        {
            int skipNestListCount = 0;
            for (int c = startColIndex - 1; c >= 0; --c)
            {
                ColumnTag tag = grid.GetColumn(c).ColumnTag;
                if (skipNestListCount > 0)
                {
                    switch (tag.Tag)
                    {
                        case ColumnTag.ETag.ListEnd:
                            ++skipNestListCount;
                            break;
                        case ColumnTag.ETag.ListStart:
                            --skipNestListCount;
                            break;
                    }
                    continue;
                }
                switch (tag.Tag)
                {
                    case ColumnTag.ETag.AddVariable:
                    case ColumnTag.ETag.ListStart:
                        return c + 1;
                    //case ColumnTag.ETag.Normal:
                    //    break;
                    case ColumnTag.ETag.ListEnd:
                        ++skipNestListCount;
                        break;
                }
            }
            throw new Exception("FindColumnBeanBegin");
        }

        public static int DoActionUntilBeanEnd(GridData grid, int colBeanBegin, int colListEnd, Action<int> action)
        {
            int skipNestListCount = 0;
            for (int c = colBeanBegin; c < colListEnd; ++c)
            {
                action(c);
                ColumnTag tag = grid.GetColumn(c).ColumnTag;
                if (skipNestListCount > 0)
                {
                    switch (tag.Tag)
                    {
                        case ColumnTag.ETag.ListEnd:
                            --skipNestListCount;
                            break;
                        case ColumnTag.ETag.ListStart:
                            ++skipNestListCount;
                            break;
                    }
                    continue;
                }
                switch (tag.Tag)
                {
                    case ColumnTag.ETag.ListStart:
                        ++skipNestListCount;
                        break;
                    case ColumnTag.ETag.AddVariable:
                        return c + 1;
                    //case ColumnTag.ETag.Normal:
                    //    break;
                    case ColumnTag.ETag.ListEnd:
                        throw new Exception("DoActionUntilBeanEnd");
                }
            }
            return colListEnd;
        }

        public void DeleteListItem(int columnIndexSelected)
        {
            int colListEnd = GridData.FindColumnListEnd(this, columnIndexSelected);
            if (colListEnd < 0)
            {
                MessageBox.Show("请选择 List 中间的列。");
                return; // not in list
            }

            ColumnTag tagSelected = Columns[columnIndexSelected].ColumnTag;
            ColumnTag tagListEnd = Columns[colListEnd].ColumnTag;
            int pathEndIndex = tagListEnd.Path.Count - 1;
            int colBeanBegin = GridData.FindColumnBeanBegin(this, columnIndexSelected);
            int listIndex = tagSelected.Path[pathEndIndex].ListIndex;

            // delete data(list item)
            for (int row = 0; row < RowCount; ++row)
            {
                Document.Beans[row].GetVarData(0, tagSelected, pathEndIndex)?.DeleteBeanAt(listIndex);
            }

            if (tagListEnd.PathLast.ListIndex == -1)
            {
                // 只有一个item，仅删除数据，不需要删除Column。需要更新grid。
                for (int row = 0; row < RowCount; ++row)
                {
                    GridData.DoActionUntilBeanEnd(this, colBeanBegin, colListEnd,
                        (int col) =>
                        {
                            switch (Columns[col].ColumnTag.Tag)
                            {
                                case ColumnTag.ETag.Normal:
                                    var cell = GetCell(col, row);
                                    cell.Value = "";
                                    View?.InvalidateCell(col, row);
                                    break;
                            }
                        });
                }
                return;
            }

            {
                // delete column
                List<int> colDelete = new List<int>();
                GridData.DoActionUntilBeanEnd(this, colBeanBegin, colListEnd,
                    (int col) => colDelete.Add(col));
                for (int i = colDelete.Count - 1; i >= 0; --i)
                    RemoveColumn(colDelete[i]);
                colListEnd -= colDelete.Count;
            }

            // reduce ListIndex In Current List after deleted item.
            while (colBeanBegin < colListEnd)
            {
                colBeanBegin = GridData.DoActionUntilBeanEnd(this, colBeanBegin, colListEnd,
                    (int col) =>
                    {
                        ColumnTag tagReduce = Columns[col].ColumnTag;
                        --tagReduce.Path[pathEndIndex].ListIndex;
                    });
            }
            ++tagListEnd.PathLast.ListIndex;
        }
    }
}
