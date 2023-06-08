@echo off
setlocal
pushd %~dp0

java -Dlogname=ZezeJavaTest -ea -cp .;lib/*;build/libs/* org.junit.runner.JUnitCore ^
Benchmark.ABasicSimpleAddOneThread ^
Benchmark.BBasicSimpleAddConcurrentWithConflict ^
Benchmark.BenchSocket ^
Benchmark.CBasicSimpleAddConcurrent ^
Benchmark.CheckpointFlush ^
Benchmark.DiffLockAndNoLock ^
Benchmark.PMapLogTypeIdHash32Cache ^
Benchmark.TestTaskOneByOne ^
Benchmark.TestToData ^
Dbh2.Dbh2FullTest ^
Dbh2.Dbh2Test ^
Dbh2.TestLocateBucket ^
Dbh2.TestRocksDb ^
Infinite.Simulate ^
RelationalMapping.TestRelationalTableDiff ^
UnitTest.Zeze.Collections.TestBeanFactory ^
UnitTest.Zeze.Collections.TestLinkedMap ^
UnitTest.Zeze.Collections.TestQueue ^
UnitTest.Zeze.Component.TestAutoKey ^
UnitTest.Zeze.Component.TestTimer ^
UnitTest.Zeze.Component.TestToken ^
UnitTest.Zeze.Game.TestBag ^
UnitTest.Zeze.Game.TestRank ^
UnitTest.Zeze.Game.TestTask ^
UnitTest.Zeze.Misc.TestRocketMQ ^
UnitTest.Zeze.Misc.TestServiceManager ^
UnitTest.Zeze.Misc.TestTreeMap ^
UnitTest.Zeze.Net.TestAsyncSocket ^
UnitTest.Zeze.Net.TestCodec ^
UnitTest.Zeze.Net.TestDatagram ^
UnitTest.Zeze.Net.TestRpc ^
UnitTest.Zeze.Netty.TestNettyHttpServer ^
UnitTest.Zeze.Serialize.TestByteBuffer ^
UnitTest.Zeze.Serialize.TestDynamic ^
UnitTest.Zeze.Trans.TestBegin ^
UnitTest.Zeze.Trans.TestChangeListener ^
UnitTest.Zeze.Trans.TestCheckpoint ^
UnitTest.Zeze.Trans.TestCheckpointModeTable ^
UnitTest.Zeze.Trans.TestConcurrentDictionary ^
UnitTest.Zeze.Trans.TestConcurrentStartServer ^
UnitTest.Zeze.Trans.TestConflict ^
UnitTest.Zeze.Trans.TestDatabaseMySql ^
UnitTest.Zeze.Trans.TestDatabaseRocksDB ^
UnitTest.Zeze.Trans.TestDatabaseSqlServer ^
UnitTest.Zeze.Trans.TestDatabaseTikv ^
UnitTest.Zeze.Trans.TestGlobal ^
UnitTest.Zeze.Trans.TestLock ^
UnitTest.Zeze.Trans.TestLostRedo ^
UnitTest.Zeze.Trans.TestNestProcedureModifyMapSet ^
UnitTest.Zeze.Trans.TestProcdure ^
UnitTest.Zeze.Trans.TestProcedureRedo ^
UnitTest.Zeze.Trans.TestTable ^
UnitTest.Zeze.Trans.TestTableKey ^
UnitTest.Zeze.Trans.TestTableNest ^
UnitTest.Zeze.Trans.TestTableNestAction ^
UnitTest.Zeze.Trans.TestTransactionLevelSerialiable ^
UnitTest.Zeze.Trans.TestWalkPage ^
UnitTest.Zeze.Util.TestCert ^
UnitTest.Zeze.Util.TestConsistentHash ^
UnitTest.Zeze.Util.TestFewModifySortedMap ^
UnitTest.Zeze.Util.TestJson ^
UnitTest.Zeze.Util.TestJson5 ^
UnitTest.Zeze.Util.TestJsonWriter ^
UnitTest.Zeze.Util.TestPersistentAtomicLong ^
UnitTest.Zeze.Util.TestSortedMap ^
UnitTest.Zeze.Util.TestStr ^
UnitTest.Zeze.Util.TestTimeThrottle ^
Zezex.ModuleRedirectRank ^
Zezex.TestGameTimer ^
Zezex.TestOnline ^
Zezex.TestRoleTimer

pause
