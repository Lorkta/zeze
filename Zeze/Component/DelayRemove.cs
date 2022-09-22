
using Org.BouncyCastle.Asn1.Cms;
using System;
using System.Collections.Concurrent;
using System.Globalization;
using System.Threading.Tasks;
using Zeze.Builtin.DelayRemove;
using Zeze.Net;
using Zeze.Transaction;
using Zeze.Util;

namespace Zeze.Component
{
	public class DelayRemove : AbstractDelayRemove
	{
		/**
		 * ÿ��ServerId����һ��������GC���С�Server֮�䲻�����������һ��Serverһֱû����������ô����GC��һֱ����ִ�С�
		 */
		private static ConcurrentDictionary<int, DelayRemove> delays = new();

		public static async Task RemoveAsync(Table table, object key)
		{
			var serverId = table.Zeze.Config.ServerId;
			var delay = delays.GetOrAdd(serverId, (_key_) => new DelayRemove(table.Zeze));
			var value = new BTableKey()
			{
				TableName = table.Name,
				EncodedKey = new Binary(table.EncodeKey(key)),
				EnqueueTime = Util.Time.NowUnixMillis,
			};
			await delay.queue.AddAsync(value);
		}

		private Zeze.Collections.Queue<BTableKey> queue;
        public Zeze.Application Zeze { get; }
        public const string eTimerNamePrefix = "Zeze.Component.DelayRemove.";

		private DelayRemove(Zeze.Application zz)
		{
			this.Zeze = zz;
			var serverId = zz.Config.ServerId;
			queue = zz.Queues.Open<BTableKey>("__GCTableQueue#" + serverId);

            // TODO start timer to gc. work on queue.pollNode? peekNode? poll? peek?
            var name = eTimerNamePrefix + serverId;

            //zz.Timer.AddHandle(name, (context) => OnTimer(serverId));

            // �������õ�Timer��ʱ�䷶Χ�������Ӿ��������ÿ��Ŀ�ʼʱ�䣬�������ӳ٣�Ȼ��24Сʱ���ִ�С�
            var now = new DateTime();
            var at = new DateTime(now.Year, now.Month, now.Day, Zeze.Config.DelayRemoveHourStart, 0, 0);
            var minutes = 60 * (Zeze.Config.DelayRemoveHourEnd - Zeze.Config.DelayRemoveHourStart);
            if (minutes <= 0)
                minutes = 60;
            minutes = Util.Random.Instance.Next(minutes);
            at.AddMinutes(minutes);
            if (at.CompareTo(now) < 0)
                at = at.AddDays(1);
            var delay = Util.Time.DateTimeToUnixMillis(at) - Util.Time.DateTimeToUnixMillis(now);
            var period = 24 * 3600 * 1000; // 24 hours
            //zz.Timer.ScheduleNamed(name, delay, period, name, null);
        }

        private void OnTimer(int serverId)
        {
            // delayRemove������Ҫɾ���ܶ��¼����Ƕ��Timer���������µ��߳�ִ���µ�����
            // ���������Timer�Ĵ�����
            // ÿ���ڵ�ļ�¼ɾ��һ������ִ�С�
            _ = Mission.CallAsync(Zeze.NewProcedure(() => RunDelayRemove(serverId), "delayRemoveProcedure"));
        }

        private async Task<long> RunDelayRemove(int serverId)
        {
            // �Ѿ����������ˡ�
            var days = Zeze.Config.DelayRemoveDays;
            if (days < 7)
                days = 7;
            var diffMills = days * 24 * 3600 * 1000;

            var maxTime = 0L; // �ŵ�������Դ��������node.getValues().isEmpty()�������
            var node = await queue.PollNodeAsync();
            foreach (var value in node.Values)
            {
                var tableKey = (BTableKey)value.Value.Bean;
                // queue�ǰ�ʱ��˳��ģ���ס���һ�����ɣ�����д����Ӧ����˳��ġ�
                maxTime = Math.Max(maxTime, tableKey.EnqueueTime);
                var table = Zeze.GetTableSlow(tableKey.TableName);
                if (null != table)
                    await table.RemoveAsync(tableKey.EncodedKey);
            }
            if (diffMills < Util.Time.NowUnixMillis - maxTime)
                OnTimer(serverId); // �������ϵģ��ٴγ���ɾ����
            return 0;
        }
    }
}
