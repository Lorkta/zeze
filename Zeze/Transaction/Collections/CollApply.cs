﻿using System;
using System.Collections.Generic;
using System.Linq;

namespace Zeze.Transaction.Collections
{
    // for conf+cs+net
    public class CollApply
    {
        public static void ApplyList1<E>(List<E> _list, Log _log)
        {
            var log = (LogList1<E>)_log;
            foreach (var opLog in log.OpLogs)
            {
                switch (opLog.op)
                {
                    case LogList1<E>.OpLog.OP_MODIFY:
                        _list[opLog.index] = opLog.value;
                        break;
                    case LogList1<E>.OpLog.OP_ADD:
                        _list.Insert(opLog.index, opLog.value);
                        break;
                    case LogList1<E>.OpLog.OP_REMOVE:
                        _list.RemoveAt(opLog.index);
                        break;
                    case LogList1<E>.OpLog.OP_CLEAR:
                        _list.Clear();
                        break;
                }
            }
        }

        public static void ApplyList2<E>(List<E> _list, Log _log)
#if USE_CONFCS
			where E : Util.ConfBean, new()
#else
            where E : Bean, new()
#endif
        {
            var log = (LogList2<E>)_log;
            var newest = new HashSet<int>();
            foreach (var opLog in log.OpLogs)
            {
                switch (opLog.op)
                {
                    case LogList1<E>.OpLog.OP_MODIFY:
                        _list[opLog.index] = opLog.value;
                        newest.Add(opLog.index);
                        break;
                    case LogList1<E>.OpLog.OP_ADD:
                        _list.Insert(opLog.index, opLog.value);
                        newest.Add(opLog.index);
                        break;
                    case LogList1<E>.OpLog.OP_REMOVE:
                        _list.RemoveAt(opLog.index);
                        break;
                    case LogList1<E>.OpLog.OP_CLEAR:
                        _list.Clear();
                        break;
                }
            }

            // apply changed
            foreach (var e in log.Changed)
            {
                if (newest.Contains(e.Value.Value))
                    continue;
                _list[e.Value.Value].FollowerApply(e.Key);
            }
        }

        public static void ApplyMap1<K, V>(Dictionary<K, V> _map, Log _log)
        {
            var log = (LogMap1<K, V>)_log;
            foreach (var pair in log.Replaced)
                _map[pair.Key] = pair.Value;
            foreach (var key in log.Removed)
                _map.Remove(key);
        }

        public static void ApplyMap2<K, V>(Dictionary<K, V> _map, Log _log)
#if USE_CONFCS
			where V : Util.ConfBean, new()
#else
            where V : Bean, new()
#endif
        {
            var log = (LogMap2<K, V>)_log;

            foreach (var pair in log.Replaced)
                _map[pair.Key] = pair.Value;
            foreach (var key in log.Removed)
                _map.Remove(key);

            // apply changed
            foreach (var e in log.ChangedWithKey)
            {
                if (_map.TryGetValue(e.Key, out var value))
                {
                    value.FollowerApply(e.Value);
                }
            }
        }

        public static void ApplySet1<E>(HashSet<E> _set, Log _log)
        {
            var log = (LogSet1<E>)_log;
            _set.Union(log.Added);
            _set.Except(log.Removed);
        }

        public static void ApplyOne<V>(ref V value, Log _log)
#if USE_CONFCS
			where V : Util.ConfBean, new()
#else
            where V : Bean, new()
#endif
        {
            var log = (LogOne<V>)_log;
            if (null != log.Value)
            {
                value = log.Value;
            }
            else if (null != log.LogBean)
            {
                value.FollowerApply(log.LogBean);
            }

        }
    }
}
