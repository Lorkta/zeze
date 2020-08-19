﻿using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Transaction
{
    public abstract class Record
    {
        public long Timestamp { get; private set; }
        public Bean Value { get; private set; }

        public Record(long timestamp, Bean value)
        {
            this.Timestamp = timestamp;
            this.Value = value;
        }

        // 时戳生成器，运行时状态，需要持久化时，再考虑保存到数据库。
        // 0 保留给不存在记录的的时戳。
        private static Zeze.Util.AtomicLong _TimestampGen = new Zeze.Util.AtomicLong();
        private static long NextTimestamp => _TimestampGen.IncrementAndGet();

        // XXX 临时写个实现，以后调整。
        internal void Commit(Transaction.CachedRecord cr)
        {
            if (cr.PutValueChannged)
            {
                Value = cr.PutValue;
            }
            Timestamp = NextTimestamp;
        }
    }

    public class Record<K, V> : Record where V : Bean 
    {
        public V ValueTyped => (V)Value;
   
        public Record(long timestamp, V value) : base(timestamp, value)
        {

        }
    }
}
