
using System;
using System.Collections.Concurrent;
using System.Net.Http.Headers;
using System.Threading.Tasks;
using Zeze.Builtin.AutoKey;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Transaction;

namespace Zeze.Component
{
	public class AutoKey : AbstractAutoKey
	{
		public class Module : AbstractAutoKey
		{
			private readonly ConcurrentDictionary<string, AutoKey> map = new();
			public Application Zeze { get; }

			// ������Zeze.Application���Զ���ʼ��������ҪӦ�ó�ʼ����
			public Module(Zeze.Application zeze)
			{
				Zeze = zeze;
				RegisterZezeTables(zeze);
			}

			public override void UnRegister()
			{
				UnRegisterZezeTables(Zeze);
			}

			/**
			 * �������ֵ���������Լ�ģ���ڱ���������Ч�ʸ�һЩ��
			 */
			public AutoKey GetOrAdd(string name)
			{
				return map.GetOrAdd(name, name2 => new AutoKey(this, name2));
			}
		}

		private readonly Module module;
		private readonly string name;
		private volatile Range range;
		private readonly long logKey;
		private int allocateCount = 64;
		private long lastAllocateTime = Util.Time.NowUnixMillis;

		private AutoKey(Module module, string name)
		{
			this.module = module;
			this.name = name;

            logKey = Bean.GetNextObjectId();
		}

		public async Task<long> NextIdAsync()
		{
            var bb = await NextByteBufferAsync();
			if (bb.Size > 8)
				throw new Exception("out of range");
			return ByteBuffer.ToLong(bb.Bytes, bb.ReadIndex, bb.Size);
		}

		public async Task<byte[]> NextBytesAsync()
		{
			return (await NextByteBufferAsync()).Copy();
		}

		public async Task<Binary> NextBinaryAsync()
		{
			return new Binary(await NextByteBufferAsync());
		}

		public async Task<string> NextStringAsync()
		{
			var bb = await NextByteBufferAsync();
			return Convert.ToBase64String(bb.Bytes, bb.ReadIndex, bb.Size);
        }

		public async Task<ByteBuffer> NextByteBufferAsync()
		{
			var bb = ByteBuffer.Allocate(16);
			bb.WriteInt(module.Zeze.Config.ServerId);
			bb.WriteLong(await NextSeedAsync());
			return bb;
		}

        private async Task<long> NextSeedAsync()
        {
            if (null != range)
			{
				var next = range.TryNextId();
				if (next != null)
					return next.Value; // allocate in range success
			}

            Transaction.Transaction.WhileCommit(() =>
			{
				// ����������ʱ�ظ����㣬һ���������¼���һ�Σ���һ����Ч��
				var now = Util.Time.NowUnixMillis;
                var diff = now - lastAllocateTime;
                if (diff < 30 * 1000) // 30 seconds
                    allocateCount = allocateCount << 1;
                else if (diff > 120 * 1000 && allocateCount >= 128) // 120 seconds
                    allocateCount = allocateCount >> 1;
                lastAllocateTime = now;
            });
            
			var seedKey = new BSeedKey(module.Zeze.Config.ServerId, name);
			var txn = Transaction.Transaction.Current;
			var log = (RangeLog)txn.GetLog(logKey);
			while (true)
			{
				if (null == log)
				{
					// allocate: ���̣߳������񣬶������������ͬ������zeze��֤��
					var key = await module._tAutoKeys.GetOrAddAsync(seedKey);
					var start = key.NextId;
					var end = start + allocateCount; // AllocateCount == 0 ����ѭ����
					key.NextId = end;
					// create log��������ɼ���
					log = new RangeLog(this, new Range(start, end));
					txn.PutLog(log);
				}
				var tryNext = log.range.TryNextId();
				if (tryNext != null)
					return tryNext.Value;

				// �����ڷ����˳���Range��Χ��id���ٴ�allocate��
				// ����RangeLog�ǿ��Եġ����������ڶ�θı������������Log��������������µġ�
				// �ѷ���ķ�Χ������_AutoKeys���ڣ������ڿ��Լ������䡣
				log = null;
			}
		}

		private class Range
		{
			private readonly Util.AtomicLong atomicNextId;
			private readonly long max;

			public long? TryNextId()
			{
				// ÿ�ζ�������������Χ�Ժ�Ҳ���ָ���
				var next = atomicNextId.IncrementAndGet();
				if (next >= max)
					return null;
				return next;
			}

			public Range(long start, long end)
			{
				atomicNextId = new(start);
				max = end;
			}
		}

		private class RangeLog : Log
		{
			public AutoKey AutoKey;
			internal Range range;

			public RangeLog(AutoKey autoKey, Range range)
			{
				AutoKey = autoKey;
				this.range = range;
			}

            public override long LogKey => AutoKey.logKey;

            public override void Commit()
			{
				// ����ֱ���޸�ӵ���ߵ����ã����ų�ȥ���Ժ�����������ܿ����µ�Range�ˡ�
				// ���������߳�ʵ������ _autokeys ��������ﵽ���⣬commit��ʱ���ǻ�������
				AutoKey.range = range;
			}

            public override void Encode(ByteBuffer bb)
            {
                throw new System.NotImplementedException();
            }

            public override void Decode(ByteBuffer bb)
            {
                throw new System.NotImplementedException();
            }

            internal override void EndSavepoint(Savepoint currentsp)
            {
				currentsp.Logs[LogKey] = this;
			}

            internal override Log BeginSavepoint()
            {
				return this;
            }
        }
	}
}