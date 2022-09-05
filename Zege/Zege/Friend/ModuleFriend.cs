
using System.Collections.ObjectModel;
using Zeze.Net;
using Zeze.Transaction;
using Zeze.Util;

namespace Zege.Friend
{
    public partial class ModuleFriend : AbstractModule
    {
        public void Start(global::Zege.App app)
        {
        }

        public void Stop(global::Zege.App app)
        {
        }

        // ��ListView�ṩ���ݣ������Ǳ���CachedNodes�к��ѵ�һ���֡�
        private ObservableCollection<FriendItem> ItemsSource { get; set; } = new();
        private ListView ListView { get; set; }

        // �ö����ѵ������档
        // private BTopmostFriend Topmost;

        // ��ʵ�ֽ���β����Ӻ�ɾ���ڵ�ķ�����
        // ֧�ִ�ͷ��ɾ����һ�����ǡ�
        private List<BGetFriendNode> Nodes { get; } = new();
        private GetFriendNode GetFriendNodePending { get; set; }

        private GetFriendNode TryNewGetFriendNode(bool forward)
        {
            if (forward)
            {
                if (Nodes.Count > 0)
                {
                    var last = Nodes[^1];
                    if (last.NextNodeId == 0)
                        return null; // �Ѿ������һ���ڵ��ˡ�
                    var rpc = new GetFriendNode();
                    rpc.Argument.NodeId = last.NextNodeId;
                    return rpc;
                }
                // else ���Ի�ȡ��һ���ڵ㣬����û�û���κκ��ѽڵ㣬��һֱ���Ի�ȡ��TODO ����һ�£�
                return new GetFriendNode();
            }

            if (Nodes.Count > 0)
            {
                var last = Nodes[0];
                if (last.PrevNodeId == 0)
                    return null; // �Ѿ������һ���ڵ��ˡ�
                var rpc = new GetFriendNode();
                rpc.Argument.NodeId = last.PrevNodeId;
                return rpc;
            }
            // else ���Ի�ȡ��һ���ڵ㣬����û�û���κκ��ѽڵ㣬��һֱ���Ի�ȡ��TODO ����һ�£�
            return new GetFriendNode();

        }

        private void TryGetFriendNode(bool forward)
        {
            if (GetFriendNodePending != null)
                return; // done

            GetFriendNodePending = TryNewGetFriendNode(forward);
            GetFriendNodePending?.Send(App.ClientService.GetSocket(), ProcessGetFriendNodeResponse);
        }

        [DispatchMode(Mode = DispatchMode.UIThread)]
        private Task<long> ProcessGetFriendNodeResponse(Protocol p)
        {
            GetFriendNodePending = null;
            var r = p as GetFriendNode;
            if (r.ResultCode == 0)
            {
                UpdateItemsSource(r.Result);
            }
            return Task.FromResult(0L);
        }

        private void OnScrolled(object sender, ScrolledEventArgs args)
        {
            if (args.ScrollY > ListView.Height - 80)
                TryGetFriendNode(true);
            else if (args.ScrollY < 80)
                TryGetFriendNode(false);
        }

        public void Bind(ListView view)
        {
            ListView = view;
            view.ItemsSource = ItemsSource;
            view.Scrolled += OnScrolled;
        }

        public void GetFristFriendNodeAsync()
        {
            if (Nodes.Count == 0)
                TryGetFriendNode(true);
        }

        private int IndexOf(long nodeId)
        {
            for (int i = 0; i < Nodes.Count; ++i)
            {
                if (Nodes[i].NodeId == nodeId)
                    return i;
            }
            return -1;
        }

        private FriendItem FriendToItem(long nodeId, BGetFriend friend)
        {
            return new FriendItem()
            {
                NodeId = nodeId,
                Account = friend.Account,
                Image = "https://www.google.com/images/hpp/Chrome_Owned_96x96.png",
                Nick = friend.Memo + " " + friend.Nick,
                Time = "12:30",
                Message = "",
            };
        }

        private bool FriendMatch(FriendItem ii, BGetFriend jj)
        {
            // todo �������ݱ仯��顣
            return ii.Nick.Equals(jj.Memo + " " + jj.Nick);
        }

        private void UpdateItemsSource(BGetFriendNode node)
        {
            var indexOf = IndexOf(node.NodeId);
            if (-1 == indexOf)
            {
                Nodes.Add(node);
                foreach (var friend in node.Friends)
                {
                    ItemsSource.Add(FriendToItem(node.NodeId, friend));
                }
            }
            else
            {
                Nodes[indexOf] = node; // replace

                // ���½ڵ��еĺ��ѵ�View�С�
                // ����ֱ���޸ģ�ObservableCollection[i].Nick = "New Nick"��������ʽӦ����û��֪ͨView���µġ�
                // ���������Լ��Ƚϣ��������Ż���ȥ����View�ĸ��¡�
                // �㷨������
                // node�к������ڻ�����ܻᱻ��������ͷ�����ܲ�������ڵ㣬�ᵽ�˸�ǰ��Ľڵ㣩��
                // �����β�Ϳ�ʼ�Ƚϣ�������ѭ����ɾ����
                // �Ƚ���ȫƥ�䣬������ItemsSource��
                // �Ƚϲ�ƥ��ʱ����ItemsSource��ɾ����ǰ�

                // reverse walk. maybe RemoveAt in loop.
                // ��λItemsSource���Ǳ��ڵ���ѵ����һ����
                int i = ItemsSource.Count - 1;
                for (; i >= 0; --i)
                {
                    if (ItemsSource[i].NodeId == node.NodeId)
                        break;
                }
                if (-1 == i)
                    return; // impossible.

                // �Ƚ�friend�����Ƿ�ı�
                int j = node.Friends.Count - 1;
                while (i >= 0 && j >= 0)
                {
                    var ii = ItemsSource[i];
                    if (ii.NodeId != node.NodeId)
                        break; // view �����ڵ�ǰ�ڵ��item�Ѿ�������

                    var jj = node.Friends[j];

                    if (ii.Account.Equals(jj.Account))
                    {
                        if (FriendMatch(ii, jj))
                        {
                            // ���ݷ����˱����ʹ��ɾ���ٴμ���ķ�ʽ����View��
                            ItemsSource.RemoveAt(i);
                            ItemsSource.Insert(i, FriendToItem(node.NodeId, jj));
                        }
                        // ��ͬ�ĺ��ѣ�������ɣ�����ǰ�ƽ���
                        --i;
                        --j;
                    }
                    else
                    {
                        // ��ͬ�ĺ��ѣ�ɾ��View����ǰ�ƽ���Node�еĺ��ѱ��ֲ��䡣
                        ItemsSource.RemoveAt(i);
                        --i;
                    }
                }

                // ɾ��ItemsSource�ж������item
                for (; i >= 0; --i)
                {
                    var ii = ItemsSource[i];
                    if (ii.NodeId != node.NodeId)
                        break; // view �����ڵ�ǰ�ڵ��item�Ѿ�������
                    ItemsSource.RemoveAt(i);
                }

                // ���Node��ʣ���friend
                ++i; // ������ʱ��iΪ-1������ָ��ǰ��һ���ڵ�����һ�����ѡ���Ҫ��������濪ʼ����ʣ���friend��
                for (; j >= 0; --j)
                {
                    ItemsSource.Insert(i, FriendToItem(node.NodeId, node.Friends[j]));
                }
            }
        }
    }

    public class FriendItem
    {
        // Basic
        public long NodeId { get; set; }
        public string Account { get; set; }

        // Bind To View
        public string Image { get; set; }
        public string Nick { get; set; }
        public string Time { get; set; }
        public string Message { get; set; }
    }
}
