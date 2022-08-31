
using Java.Security;
using System.Collections.ObjectModel;
using Zeze.Net;
using static Android.App.LauncherActivity;

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
        private ObservableCollection<Friend> ItemsSource { get; set; } = new();
        private ListView ListView { get; set; }

        // �ö����ѵ������档
        // private BTopmostFriend Topmost;

        // ��ʵ�ֽ���β����Ӻ�ɾ���ڵ�ķ�����
        // ֧�ִ�ͷ��ɾ����һ�����ǡ�
        private List<(long, BGetFriendNode)> Nodes { get; } = new();
        private GetFriendNode GetFriendNodePending { get; set; }

        private void TryGetNextFriendNode()
        {
            if (GetFriendNodePending != null)
                return; // done

            GetFriendNodePending = new GetFriendNode();
            GetFriendNodePending.Argument.NodeId = Nodes.Count > 0 ? Nodes[^1].Item2.NextNodeId : 0;
            GetFriendNodePending.Send(App.ClientService.GetSocket(), (p) =>
            {
                GetFriendNodePending = null;
                var r = p as GetFriendNode;
                if (r.ResultCode == 0)
                {
                    UpdateItemsSource(r.Argument.NodeId, r.Result);
                }
                return Task.FromResult(0L);
            });
        }

        private void OnScrolled(object sender, ScrolledEventArgs args)
        {
            if (args.ScrollY > ListView.Height - 80)
                TryGetNextFriendNode();
        }

        public void Bind(ListView view)
        {
            ListView = view;
            view.ItemsSource = ItemsSource;
            view.Scrolled += OnScrolled;

            if (Nodes.Count == 0)
                TryGetNextFriendNode();
        }

        private int IndexOf(long nodeId)
        {
            for (int i = 0; i < Nodes.Count; ++i)
            {
                if (Nodes[i].Item1 == nodeId)
                    return i;
            }
            return -1;
        }

        private Friend FriendToView(long nodeId, BGetFriend friend)
        {
            return new Friend()
            {
                NodeId = nodeId,
                Account = friend.Account,
                Image = "https://www.google.com/images/hpp/Chrome_Owned_96x96.png",
                Nick = friend.Memo + " " + friend.Nick,
                Time = "12:30",
                Message = "",
            };
        }

        private bool FriendMatch(Friend ii, BGetFriend jj)
        {
            // todo �������ݱ仯��顣
            return ii.Nick.Equals(jj.Memo + " " + jj.Nick);
        }

        private void UpdateItemsSource(long nodeId, BGetFriendNode node)
        {
            var indexOf = IndexOf(nodeId);
            if (-1 == indexOf)
            {
                Nodes.Add((nodeId, node));
                foreach (var friend in node.Friends)
                {
                    ItemsSource.Add(FriendToView(nodeId, friend));
                }
            }
            else
            {
                Nodes[indexOf] = (nodeId, node); // replace

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
                    if (ItemsSource[i].NodeId == nodeId)
                        break;
                }
                if (-1 == i)
                    return; // impossible.

                // �Ƚ�friend�����Ƿ�ı�
                int j = node.Friends.Count - 1;
                while (i >= 0 && j >= 0)
                {
                    var ii = ItemsSource[i];
                    if (ii.NodeId != nodeId)
                        break; // view �����ڵ�ǰ�ڵ��item�Ѿ�������

                    var jj = node.Friends[j];

                    if (ii.Account.Equals(jj.Account))
                    {
                        if (FriendMatch(ii, jj))
                        {
                            // ���ݷ����˱����ʹ��ɾ���ٴμ���ķ�ʽ����View��
                            ItemsSource.RemoveAt(i);
                            ItemsSource.Insert(i, FriendToView(nodeId, jj));
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
                    if (ii.NodeId != nodeId)
                        break; // view �����ڵ�ǰ�ڵ��item�Ѿ�������
                    ItemsSource.RemoveAt(i);
                }

                // ���Node��ʣ���friend
                ++i; // ������ʱ��iΪ-1������ָ��ǰ��һ���ڵ�����һ�����ѡ���Ҫ��������濪ʼ����ʣ���friend��
                for (; j >= 0; --j)
                {
                    ItemsSource.Insert(i, FriendToView(nodeId, node.Friends[j]));
                }
            }
        }
    }

    public class Friend
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
