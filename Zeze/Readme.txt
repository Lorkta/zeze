TODO 
Э���������ֵĬ�ϴ��ڵ���0��֧�ָ�����Ҫ��ȷ������
���ݱ��?

GameServer.TableCache
    ConcurrentDictionary<K, Record<K, V>> cache;
    ��¼��Cache�е�����״̬��
    M:Modify
    S:Share
    I:Invalid(Not In Cache)

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
4 Checkpoing�ռ����Reduce��������ʹ��flushReadLock����flushWriteLock֮�����ټ����µ�����
