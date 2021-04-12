﻿using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Util
{
    /// <summary>
    /// 这个类恐怕没什么用。写在这里主要是为了一个建议：
    /// 即事件应该在新的事务中执行。不要嵌套到触发者的事务中，否则可能无法控制。
    /// </summary>
    public class EventDispatcher
    {
        private ConcurrentDictionary<string, Func<object, EventArgs, int>> Handles { get; }
            = new ConcurrentDictionary<string, Func<object, EventArgs, int>>();

        public void AddEventHandle(Func<object, EventArgs, int> handle, string name = null)
        {
            if (null == name)
            {
                name = handle.Method.Name;
            }
            if (false == Handles.TryAdd(name, handle))
                throw new Exception($"Handle for '{name}' exist.");
        }

        public void RemoveEventHandle(Func<object, EventArgs, int> handle, string name = null)
        {
            if (null == name)
            {
                name = handle.Method.Name;
            }
            Handles.TryRemove(KeyValuePair.Create(name, handle));
        }

        public void Dispatch(object sender, EventArgs args)
        {
            var app = new Zeze.Application(null);
            foreach (var e in Handles)
            {
                Zeze.Util.Task.Run(app.NewProcedure(() => e.Value(sender, args), e.Key));
            }
        }
    }
}
