package Zeze.World;

import java.io.Closeable;
import java.io.IOException;
import java.util.SortedMap;
import Zeze.Builtin.World.Move;
import Zeze.Collections.BeanFactory;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Data;

public class World extends AbstractWorld {
	private final static BeanFactory beanFactory = new BeanFactory();

	public static long getSpecialTypeIdFromBean(Bean bean) {
		return bean.typeId();
	}

	public static long getSpecialTypeIdFromBean(Data data) {
		return data.typeId();
	}

	public static Bean createBeanFromSpecialTypeId(long typeId) {
		return beanFactory.createBeanFromSpecialTypeId(typeId);
	}

	public static Data createDataFromSpecialTypeId(long typeId) {
		return beanFactory.createDataFromSpecialTypeId(typeId);
	}

	@Override
	protected long ProcessMove(Move p) throws Exception {
		return 0;
	}
}