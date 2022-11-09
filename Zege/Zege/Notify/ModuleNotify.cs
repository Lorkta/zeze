
using Zege.Friend;
using Zeze.Builtin.Collections.LinkedMap;
using Zeze.Serialize;
using Zeze.Transaction.Collections;
using Zeze.Transaction;
using Zeze.Util;
using System.Collections.ObjectModel;
using Zeze.Net;

namespace Zege.Notify
{
    public partial class ModuleNotify : AbstractModule
    {
        public List<BGetNotifyNode> Nodes { get; } = new();
        internal ObservableCollection<NotifyItem> ItemsSource { get; } = new();
        public NotifyPage NotifyPage { get; private set; }
        public GetNotifyNode GetNotifyNodePending { get; set; }


        public void Start(global::Zege.App app)
        {
        }

        public void Stop(global::Zege.App app)
        {
        }

        public void GetFirstNode()
        {
            if (Nodes.Count == 0)
                TryGetFriendNode(true);
        }

        internal GetNotifyNode TryNewGetFriendNode(bool forward)
        {
            if (forward)
            {
                if (Nodes.Count > 0)
                {
                    var last = Nodes[^1];
                    if (last.Node.NextNodeId == 0)
                        return null; // �Ѿ������һ���ڵ��ˡ�
                    var rpc = new GetNotifyNode();
                    rpc.Argument.NodeId = last.Node.NextNodeId;
                    return rpc;
                }
                else
                {
                    var rpc = new GetNotifyNode();
                    rpc.Argument.NodeId = 0;
                    return rpc;
                }
            }

            if (Nodes.Count > 0)
            {
                var last = Nodes[0];
                if (last.Node.PrevNodeId == 0)
                    return null; // �Ѿ������һ���ڵ��ˡ�
                var rpc = new GetNotifyNode();
                rpc.Argument.NodeId = last.Node.PrevNodeId;
                return rpc;
            }
            else
            {
                var rpc = new GetNotifyNode();
                rpc.Argument.NodeId = 0;
                return rpc;
            }

        }

        public bool TryGetFriendNode(bool forward)
        {
            if (GetNotifyNodePending != null)
                return true; // done

            GetNotifyNodePending = TryNewGetFriendNode(forward);
            GetNotifyNodePending?.Send(App.ClientService.GetSocket(), ProcessGetNotifyNodeResponse);
            return GetNotifyNodePending != null;
        }

        // ��ѯ���ѽ����������
        [DispatchMode(Mode = DispatchMode.UIThread)]
        private Task<long> ProcessGetNotifyNodeResponse(Protocol p)
        {
            var r = p as GetNotifyNode;
            if (r.ResultCode == 0)
            {
                GetNotifyNodePending = null;
                var indexOf = IndexOf(r.Result.NodeKey.NodeId);
                if (indexOf >= 0)
                {
                    Nodes[indexOf] = r.Result;
                    UpdateItemsSource(UpdateType.Update, r.Result);
                }
                else
                {
                    Nodes.Add(r.Result);
                    UpdateItemsSource(UpdateType.InsertTail, r.Result);
                }
            }
            return Task.FromResult(0L);
        }

        public void SetNotifyPage(NotifyPage page)
        {
            if (null != NotifyPage)
                NotifyPage.NotifyListView.ItemsSource = null; // detach
            NotifyPage = page;
            if (null != NotifyPage)
                NotifyPage.NotifyListView.ItemsSource = ItemsSource; // attach
        }

        protected override Task<long> ProcessNotifyNodeLogBeanNotify(Zeze.Net.Protocol _p)
        {
            var p = _p as NotifyNodeLogBeanNotify;

            var bb = ByteBuffer.Wrap(p.Argument.ChangeLog);
            var _ = bb.ReadString(); // Read TableName. Skip.
            var key = new BLinkedMapNodeKey();
            key.Decode(bb.ReadByteBuffer());
            FollowerApply(key, bb);
            return Task.FromResult(ResultCode.Success);
        }

        public void FollowerApply(BLinkedMapNodeKey key, ByteBuffer bb)
        {
            int state;
            state = bb.ReadInt();
            switch (state)
            {
                case ChangesRecord.Remove:
                    {
                        var indexOf = IndexOf(key.NodeId);
                        if (indexOf >= 0)
                        {
                            Nodes.RemoveAt(indexOf);
                            OnRemoveNode(key);
                        }
                    }
                    break;

                case ChangesRecord.Put:
                    {
                        var value = new BLinkedMapNode();
                        value.Decode(bb);
                        var node = new BGetNotifyNode();
                        node.NodeKey = key;
                        node.Node = value;
                        // �½ڵ���룺��Ӻ���ʱ��ͷ���ڵ����ˣ����߻�Ծ�ĺ�������������ͷ���ڵ������
                        Nodes.Insert(0, node);
                        UpdateItemsSource(UpdateType.InsertHead, node);
                    }
                    break;

                case ChangesRecord.Edit:
                    {
                        var logBean = new HashSet<LogBean>();
                        bb.Decode(logBean);
                        var it = logBean.GetEnumerator();
                        if (it.MoveNext())
                        {
                            var indexOf = IndexOf(key.NodeId);
                            if (indexOf < 0)
                                return;
                            Nodes[indexOf].Node.FollowerApply(it.Current);
                            UpdateItemsSource(UpdateType.Update, Nodes[indexOf]);
                        }
                    }
                    break;
            }
        }

        public int IndexOf(long nodeId)
        {
            for (int i = 0; i < Nodes.Count; ++i)
            {
                if (Nodes[i].NodeKey.NodeId == nodeId)
                    return i;
            }
            return -1;
        }

        private void OnRemoveNode(BLinkedMapNodeKey nodeKey)
        {
            int i = ItemsSource.Count - 1;

            // �Ӻ��濪ʼ��������ڵ����
            for (; i >= 0; i--)
            {
                var item = ItemsSource[i];
                if (item.NodeKey.Equals(nodeKey))
                    break;
            }

            // ɾ���ڵ��е���
            for (; i >= 0; i--)
            {
                var item = ItemsSource[i];
                if (false == item.NodeKey.Equals(nodeKey))
                    break;
                ItemsSource.RemoveAt(i);
            }
        }

        public bool NotifyMatch(NotifyItem ii, BLinkedMapNodeValue jj)
        {
            // todo �������ݱ仯��顣��Ҫ���User.Nick��
            var cur = (BNotify)jj.Value;
            var nick = string.IsNullOrEmpty(cur.Title) ? jj.Id : cur.Title;
            return ii.Title.Equals(nick);
        }

        public NotifyItem ToNotifyItem(BLinkedMapNodeKey nodeKey, BLinkedMapNodeValue nodeValue)
        {
            var notify = (BNotify)nodeValue.Value;
            return new NotifyItem()
            {
                NodeKey = nodeKey,
                Id = nodeValue.Id,
                Title = notify.Title,
            };
        }

        public enum UpdateType
        {
            Update, // ����
            InsertTail, // Nodes.Last�����һ��֮��ʼInsert
            InsertHead, // Nodes.First�ĵ�һ��ǰ���index��Ϊ��ʼλ�ÿ�ʼInsert
        }

        private void InsertItemsSource(int insertIndex, BGetNotifyNode node)
        {
            var itemsSource = ItemsSource;
            foreach (var friend in node.Node.Values)
            {
                itemsSource.Insert(insertIndex++, ToNotifyItem(node.NodeKey, friend));
            }
        }

        private void UpdateItemsSource(BGetNotifyNode node)
        {
            // �ڵ���£��ڵ��ڵ�����˱䶯��
            // ���½ڵ��еĺ��ѵ�View�С�
            // ����ֱ���޸ģ�ObservableCollection[i].Nick = "New Nick"��������ʽӦ����û��֪ͨView���µġ�
            // �����Լ��Ƚϣ��������Ż���ȥ����View�ĸ��¡�
            // �㷨������
            // node�к������ڻ�����ܻᱻ��������ͷ�����ܲ�������ڵ㣬�ᵽ�˸�ǰ��Ľڵ㣩��
            // �����β�Ϳ�ʼ�Ƚϣ�������ѭ����ɾ����
            // �Ƚ���ȫƥ�䣬������ItemsSource��
            // �Ƚϲ�ƥ��ʱ����ItemsSource��ɾ����ǰ�

            // reverse walk. maybe RemoveAt in loop.
            // ��λItemsSource���Ǳ��ڵ���ѵ����һ����
            var itemsSource = ItemsSource;
            int i = itemsSource.Count - 1;
            for (; i >= 0; --i)
            {
                var item = itemsSource[i];
                if (item.NodeKey.Equals(node.NodeKey))
                    break;
            }
            if (-1 == i)
                return; // impossible.

            // �Ƚ�friend�����Ƿ�ı�
            int j = node.Node.Values.Count - 1;
            while (i >= 0 && j >= 0)
            {
                var ii = itemsSource[i];
                if (false == ii.NodeKey.Equals(node.NodeKey))
                    break; // view �����ڵ�ǰ�ڵ��item�Ѿ�������

                var jj = node.Node.Values[j];

                if (ii.Id.Equals(jj.Id))
                {
                    if (false == NotifyMatch(ii, jj))
                    {
                        // ���ݷ����˱����ʹ��ɾ���ٴμ���ķ�ʽ����View��
                        //ItemsSource.RemoveAt(i);
                        //ItemsSource.Insert(i, FriendToItem(node.NodeId, jj));

                        // Replaced ������ȷ֪ͨView������Ҫɾ���ټ��롣
                        itemsSource[i] = ToNotifyItem(node.NodeKey, jj);
                    }
                    // ��ͬ�ĺ��ѣ�������ɣ�����ǰ�ƽ���
                    --i;
                    --j;
                }
                else
                {
                    // ��ͬ�ĺ��ѣ�ɾ��View����ǰ�ƽ���Node�еĺ��ѱ��ֲ��䡣
                    itemsSource.RemoveAt(i);
                    --i;
                }
            }

            // ɾ��ItemsSource�ж������item
            for (; i >= 0; --i)
            {
                var ii = itemsSource[i];
                if (false == ii.NodeKey.Equals(node.NodeKey))
                    break; // view �����ڵ�ǰ�ڵ��item�Ѿ�������
                itemsSource.RemoveAt(i);
            }

            // ���Node��ʣ���friend
            ++i; // ������ʱ��iΪ-1������ָ��ǰ��һ���ڵ�����һ�����ѡ���Ҫ��������濪ʼ����ʣ���friend��
            for (; j >= 0; --j)
            {
                itemsSource.Insert(i, ToNotifyItem(node.NodeKey, node.Node.Values[j]));
            }
        }

        private void UpdateItemsSource(UpdateType updateType, BGetNotifyNode node)
        {
            switch (updateType)
            {
                case UpdateType.InsertTail:
                    InsertItemsSource(ItemsSource.Count, node);
                    break;

                case UpdateType.InsertHead:
                    InsertItemsSource(0, node);
                    break;

                case UpdateType.Update:
                    UpdateItemsSource(node);
                    break;
            }
        }
    }

    public class NotifyItem
    {
        // Basic
        public BLinkedMapNodeKey NodeKey { get; set; }
        public string Id { get; set; }

        // Bind To View
        public string Title { get; set; }
        public string ExpireTime { get; set; }
    }
}
