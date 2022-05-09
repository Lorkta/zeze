
using System.Collections.Concurrent;
using System.Threading.Tasks;
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

		private const int AllocateCount = 1000;

		private readonly Module module;
		private readonly string name;
		private volatile Range range;
		private readonly long logKey;

		private AutoKey(Module module, string name)
		{
			this.module = module;
			this.name = name;
			logKey = Bean.GetNextObjectId();
		}

		public async Task<long> NextIdAsync()
		{
			if (null != range)
			{
				var next = range.TryNextId();
				if (next != null)
					return next.Value; // allocate in range success
			}

			var txn = Transaction.Transaction.Current;
			var log = (RangeLog)txn.GetLog(logKey);
			while (true)
			{
				if (null == log)
				{
					// allocate: ���̣߳������񣬶������������ͬ������zeze��֤��
					var key = await module._tAutoKeys.GetOrAddAsync(name);
					var start = key.NextId;
					var end = start + AllocateCount; // AllocateCount == 0 ����ѭ����
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