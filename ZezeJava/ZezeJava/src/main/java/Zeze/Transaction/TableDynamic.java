package Zeze.Transaction;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;
import Zeze.Application;
import Zeze.Serialize.SQLStatement;
import Zeze.Util.Factory;
import Zeze.Serialize.ByteBuffer;

public class TableDynamic<K extends Comparable<K>, V extends Bean> extends TableX<K, V> {
	private final int id;
	private final Function<K, ByteBuffer> keyEncoder;
	private final Function<ByteBuffer, K> keyDecoder;
	private final Factory<V> valueFactory;
	private final boolean isAutoKey;

	/**
	 * 创建动态表。
	 * 创建之后就能使用。
	 * 动态表的 schemas 检查由母表完成。具体做法是正常定义母表，然后动态表使用相同的K,V创建。
	 *
	 * @param zeze          zeze
	 * @param tableName     table name
	 * @param keyEncoder    table key encoder
	 * @param keyDecoder    table key decoder
	 * @param valueFactory  table value factory
	 * @param confTableName table conf name, 如果为null，则使用name作为配置名。
	 */
	public TableDynamic(Application zeze, String tableName,
						Function<K, ByteBuffer> keyEncoder,
						Function<ByteBuffer, K> keyDecoder,
						Factory<V> valueFactory,
						boolean isAutoKey,
						String confTableName) {
		super(tableName);

		id = Bean.hash32(tableName);
		this.keyEncoder = keyEncoder;
		this.keyDecoder = keyDecoder;
		this.valueFactory = valueFactory;
		this.isAutoKey = isAutoKey;

		confTableName = confTableName == null ? tableName : confTableName;
		zeze.openDynamicTable(zeze.getConfig().getTableConf(confTableName).getDatabaseName(), this);
	}

	@Override
	public ByteBuffer encodeKey(K key) {
		return keyEncoder.apply(key);
	}

	@Override
	public K decodeKey(ByteBuffer bb) {
		return keyDecoder.apply(bb);
	}

	@Override
	public K decodeKeyResultSet(ResultSet rs) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void encodeKeySQLStatement(SQLStatement st, K _v_) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V newValue() {
		return valueFactory.create();
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public boolean isRelationalMapping() {
		return false;
	}

	@Override
	public boolean isAutoKey() {
		return isAutoKey;
	}

	public void dropTable() {
		var databaseTable = getStorage().getDatabaseTable();
		if (databaseTable instanceof DatabaseMySql.TableMysql) {
			var tableMysql = (DatabaseMySql.TableMysql)databaseTable;
			tableMysql.drop();
			return; // done
		}
		throw new UnsupportedOperationException();
	}
}
