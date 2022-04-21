
using System.Collections.Concurrent;
using System.Threading.Tasks;
using Zeze.Builtin.DelayRemove;
using Zeze.Net;
using Zeze.Transaction;

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
			};
			await delay.queue.AddAsync(value);
		}

		private Zeze.Collections.Queue<BTableKey> queue;

		private DelayRemove(Zeze.Application zz)
		{
			var serverId = zz.Config.ServerId;
			queue = zz.Queues.Open<BTableKey>("__GCTableQueue#" + serverId);

			// TODO start timer to gc. work on queue.pollNode? peekNode? poll? peek?
		}
	}
}
