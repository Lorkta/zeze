package Zeze.Util;

@FunctionalInterface
public interface Action5<T1, T2, T3, T4, T5> {
	void run(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) throws Exception;
}
