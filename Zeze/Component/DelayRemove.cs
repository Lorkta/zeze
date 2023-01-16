
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
    /**
     * ÿ��ServerId����һ��������GC���С�Server֮�䲻�����������һ��Serverһֱû����������ô����GC��һֱ����ִ�С�
     */
    public class DelayRemove : AbstractDelayRemove
	{
		public async Task RemoveAsync(Table table, object key)
		{
			var value = new BTableKey()
			{
				TableName = table.Name,
				EncodedKey = new Binary(table.EncodeKey(key)),
				EnqueueTime = Util.Time.NowUnixMillis,
			};
			await queue.AddAsync(value);
		}

		private readonly Zeze.Collections.Queue<BTableKey> queue;
        public Zeze.Application Zeze { get; }
        private Util.SchedulerTask Timer;

		public DelayRemove(Zeze.Application zz)
		{
			this.Zeze = zz;
			var serverId = zz.Config.ServerId;
			queue = zz.Queues.Open<BTableKey>("__GCTableQueue#" + serverId);
        }

        public void Start()
        {
            if (null != Timer)
                return;

            // start timer to gc. work on queue.pollNode? peekNode? poll? peek?
            // �������õ�Timer��ʱ�䷶Χ�������Ӿ��������ÿ��Ŀ�ʼʱ�䣬�������ӳ٣�Ȼ��24Сʱ���ִ�С�
            var now = DateTime.Now;
            var at = new DateTime(now.Year, now.Month, now.Day, Zeze.Config.DelayRemoveHourStart, 0, 0);
            var minutes = 60 * (Zeze.Config.DelayRemoveHourEnd - Zeze.Config.DelayRemoveHourStart);
            if (minutes <= 0)
                minutes = 60;
            minutes = Util.Random.Instance.Next(minutes);
            at = at.AddMinutes(minutes);
            if (at.CompareTo(now) < 0)
                at = at.AddDays(1);
            var delay = Util.Time.DateTimeToUnixMillis(at) - Util.Time.DateTimeToUnixMillis(now);
            var period = 24 * 3600 * 1000; // 24 hours
            Timer = Util.Scheduler.Schedule(OnTimer, delay, period);
        }

        public void Stop()
        {
            Timer?.Cancel();
            Timer = null;
        }

        private void OnTimer(SchedulerTask ThisTask)
        {
            // delayRemove������Ҫɾ���ܶ��¼��������һ�����������ȫ��ɾ����
            // ���ﰴÿ���ڵ�ļ�¼��ɾ����һ��������ִ�У��ڵ���ò�ͬ������
            var days = Zeze.Config.DelayRemoveDays;
            if (days < 7)
                days = 7;
            var diffMills = days * 24 * 3600 * 1000;

            var removing = true;
            while (removing)
            {
                Zeze.NewProcedure(async () =>
                {
                    var node = await queue.PollNodeAsync();
                    if (node == null)
                    {
                        removing = false;
                        return 0;
                    }

                    // ���ڵ�ĵ�һ�������ϵģ����Ƿ���Ҫɾ����
                    // �������Ҫ����ô�����ڵ㶼����ɾ���������ж�ѭ����
                    // �����Ҫ����ô�����ڵ㶼ɾ������ʹ�м���һЩû�дﵽ���ڡ�
                    // ���Ǹ�����ȷ��ɾ�����ڵķ�����
                    if (node.Values.Count > 0)
                    {
                        var first = (BTableKey)node.Values[0].Value.Bean;
                        if (diffMills < Util.Time.NowUnixMillis - first.EnqueueTime)
                        {
                            removing = false;
                            return 0;
                        }
                    }

                    // node.getValues().isEmpty����һ�����0��ѭ��������removing.value��������true��
                    // �����սڵ����ǳ��Լ���ɾ����
                    var maxTime = 0L;
                    foreach (var value in node.Values)
                    {
                        var tableKey = (BTableKey)value.Value.Bean;
                        // queue�ǰ�ʱ��˳��ģ���ס���һ�����ɡ�
                        maxTime = tableKey.EnqueueTime;
                        var table = Zeze.GetTableSlow(tableKey.TableName);
                        if (null != table)
                            await table.RemoveAsync(tableKey.EncodedKey);
                    }
                    removing = diffMills < Util.Time.NowUnixMillis - maxTime;
                    return 0;
                }, "delayRemoveProcedure").CallSynchronously();
            }
        }
    }
}
