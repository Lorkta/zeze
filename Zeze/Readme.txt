<!--
Module ���� Table Ĭ��˽�У�����ͨ�����ӷ������ز���¶��ȥ
Module ʹ�� partical �����ɲ��ַŵ�gen��
Module ���� IModule �� Module������IModule�ӿڷ���������ɾ��������������ȣ����������Ա���

Э���������ֵĬ�ϴ��ڵ���0��֧�ָ�����Ҫ��ȷ������
Config ����
ByteBuffer.SkipUnknownField �Ƶ������ط�
XXX ͳһ bean �� xbean ����Ļ�����ô�������ɣ��� table ���õ� bean �Զ�����һ�ݵ�ĳ���ر��Ŀ¼
ͳһ���ɹ��ߣ�Ӧ�ÿ�ܺ����ݴ洢��ܶ���ֿ������ߵ�����bean�Ķ���Ӧ���ǲ�һ���ģ����õĿ����Ժ�С��
	��Ҫ��� application ���ݿⶨ�� database. database.module ������ application �д��ڡ�

ȥ�� xbean.Const xbean.Data
ȥ�� select����������ֱ���� bean duplicate ֧��
managed
���ݱ��?
Net.Manager ��ô���¶��壿�������������

GameServer.TableCache
    ConcurrentDictionary<K, Record<K, V>> cache;
    ��¼��Cache�е�����״̬��
    M:Modify
    S:Share
    I:Invalid(Not In Cache)

Record<K, V> LoadRecordToCache(K key)
{
    Record r = cache.GetOrAdd(key, new Record<K, V>(I)); // ʹ��factory���Ա���ÿ�������˷�һ��new����

    loch(r) // ��ֹͬһ����¼�������ʡ�
    {
        if (r.State is S or M)
	    return r;

        // is I
        Global.AcquireShare();
        r.Bean = Storage.Load();
        r.State = s;
        return r;
    }
}

class GlobalCacheManager ȫ�ֻ���״̬����.
{
    ConcurrentDictionary<TableKey, CacheState> global;
}

class Global.CacheState
{
    List<GameServer> shareOccupant;
    GameServer modifyOccupant; // дӵ���ߣ�ֻ��һ���������Ϊnullʱ��shareOccupant�϶��ǿյġ�
    // List<GameServerRequest> waitQueue; // ����ȴ����а�˳��share,modify������һ���첽��ʱ����Ҫ�ɣ�û��á�
}

State Global.AcquireShare(sender, tableKey)
{
    CacheState state = global.GetOrAdd(tableKey, new CacheState()); // factory
    lock(state) // ��ֹͬһ����¼�������ʡ�
    {
        if (state.modifyOccupant != null)
	{
	    if (state.modifyOccupant == sender)
	    	return Success With State M; // �Ѿ���M״̬�ˡ�

	    modifyOccupant.ReduceToS();
	    modifyOccupant = null;
	    shareOccupant.Add(sender)
	    return Success With State S;
	}

	shareOccupant.Add(sender)
	return Success With State S;
    }
}

GameServer.ReduceToS()
{
    Record r = cache.Get(key);
    if (null == r) // I
        return Success With Do Nothing. ����϶������ˡ�

    lockey.EnterWriteLock(); // ��ס���ؼ�¼���������񲢷���������Ҫ����ûϸ�롣
    try
    {
        lock (r)
        {
            switch (r.state)
            {
	        case S:
		    return Success With S; // �Ѿ���S״̬���϶���������ˡ�
		case M:
		    r.State = S; // �����޸�״̬����������Ƕ���û�����⣬�����д�������µ�AcquireModify������global�ϡ�
		    Checkpoint.AsyncStartWithPendingAction(r -> S); // �ѵ�ǰ��¼����Pending����������Checkpoint��
		    return No Result; // ����� Checkpoint ���ͣ��첽�ġ�
	    }
        }
    }
    finally
    {
    	lockey.ExitWriteLock();
    }
}

GameServer.ReadLock
    lockey.EnterLock(false);
    lock(originRecord) // ??? û���
    {
        switch (originRecord.state)
	{
		case I: // ��¼�����ڣ�������ȡ��ʱ�������LoadRecordToCache.
			return Redo Transaction;
		case M:
			return Success;
		case S:
			return Success;
	}
    }

GameServer.WriteLock // ����д������
    lockey.EnterLock(true);
    lock(originRecord) // ??? û���
    {
        switch (originRecord.state)
	{
		case I: // ��¼�����ڣ�������ȡ��ʱ�������LoadRecordToCache.
			return Redo Transaction;
		case M:
			return Success;
		case S:
			Global.AcquireModify();
			originRecord.state = M;
			return Success;
	}
    }

Global.AcquireModify(sender, tableKey)
{
    CacheState state = global.GetOrAdd(tableKey, new CacheState()); // factory
    lock(state) // ��ֹͬһ����¼�������ʡ�
    {
        if (state.modifyOccupant != null)
	{
	    if (state.modifyOccupant == sender)
		return Success: Your State Already is M;

	    modifyOccupant.ReduceToI();
	    modifyOccupant = sender;
	    return Success With State M;
	}

	foreach (var share in shareOccupant)
	{
		if (share == sender)
			continue;
		share.RecureToI();
	}
	shareOccupant.Clear();
	modifyOccupant = sender;
	return Success With State M;
    }
}

GameServer.ReduceToI()
{
    Record r = cache.Get(key);
    if (null == r) // I
        return Success With Do Nothing. ����϶������ˡ�

    lockey.EnterWriteLock(); // ��ס���ؼ�¼���������񲢷���������Ҫ����ûϸ�롣
    try
    {
        lock (r)
        {
            switch (r.state)
            {
	        case S:
		    cache.Remove(key); // S ״̬������ɾ��������в������񣬻�������
		    return Success With I;
		case M:
		    // ����ɾ����Checkpont����ȥCache�в��Ҷ��������������⡣
		    // ����ܻᵼ���µ�LoadRecordToCache���ջᱻ����Global��lock(cachestate)�ϡ�
		    cache.Remove(I);
		    Checkpoint.AsyncStartWithPendingAction(r -> I); // �ѵ�ǰ��¼����Pending����������Checkpoint��
		    return No Result; // ����� Checkpoint ����������첽�ġ�
	    }
        }
    }
    finally
    {
    	lockey.ExitWriteLock();
    }
}

Checkpoint.AsyncStartWithPendingAction(r -> S or I)
{
	// TODO ϸ�ڻ���Ҫ����
	1 ��������ȫд��������ݿ������޸�global�ļ�¼״̬��
}

���� 
1 lockey��д�� �� lock(record) �Ĺ�ϵҪ��һ�¡�
2 ���ڵ����̶���ͬ���ģ�������첽�������Ӻܶ�״̬�����úܸ��ӡ�
  ������ʵ��ͬ���汾��
3 Cache.Remove ҲҪͬ��״̬��
