package Zeze.Util;

import java.util.Arrays;
import java.util.function.IntConsumer;

public class IntHashSet implements Cloneable {
	private int size;
	private int[] keyTable;
	private boolean hasZeroKey;
	private final float loadFactor;
	private int threshold;
	private int mask;
	private int shift;

	public IntHashSet() {
		this(2, 0.8f);
	}

	public IntHashSet(int cap) {
		this(cap, 0.8f);
	}

	public IntHashSet(int cap, float loadFactor) {
		if (loadFactor <= 0 || loadFactor >= 1)
			throw new IllegalArgumentException("invalid loadFactor: " + loadFactor);
		this.loadFactor = loadFactor;
		int tableSize = tableSize(Math.max(cap, 0));
		threshold = (int)((float)tableSize * loadFactor);
		mask = tableSize - 1;
		shift = Long.numberOfLeadingZeros(mask);
		keyTable = new int[tableSize];
	}

	public IntHashSet(IntHashSet set) {
		size = set.size;
		keyTable = set.keyTable.clone();
		hasZeroKey = set.hasZeroKey;
		loadFactor = set.loadFactor;
		threshold = set.threshold;
		mask = set.mask;
		shift = set.shift;
	}

	private int tableSize(int cap) {
		cap = Math.min(Math.max((int)Math.ceil((float)cap / loadFactor), 2), 0x40000000);
		return 1 << 32 - Integer.numberOfLeadingZeros(cap - 1);
	}

	private int hash(int key) {
		return (int)(key * 0x9E3779B97F4A7C15L >>> shift);
	}

	public int[] getKeyTable() {
		return keyTable;
	}

	public boolean hasZeroKey() {
		return hasZeroKey;
	}

	public float getLoadFactor() {
		return loadFactor;
	}

	public int capacity() {
		return mask + 1;
	}

	public int size() {
		return size;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public boolean contains(int key) {
		if (key == 0)
			return hasZeroKey;
		int[] kt = keyTable;
		int m = mask;
		int i = hash(key);
		int k;
		while ((k = kt[i]) != key) {
			if (k == 0)
				return false;
			i = (i + 1) & m;
		}
		return true;
	}

	public boolean add(int key) {
		if (key == 0) {
			if (hasZeroKey)
				return false;
			hasZeroKey = true;
			size++;
			return true;
		}
		int[] kt = keyTable;
		int m = mask;
		int i = hash(key);
		for (; ; ) {
			int k;
			if ((k = kt[i]) == 0) {
				kt[i] = key;
				if (++size >= threshold)
					resize(kt.length << 1);
				return true;
			}
			if (k == key)
				return false;
			i = (i + 1) & m;
		}
	}

	public void addAll(IntHashSet set) {
		if (set.hasZeroKey)
			hasZeroKey = true;
		for (int k : set.keyTable) {
			if (k == 0)
				continue;
			add(k);
		}
	}

	public boolean remove(int key) {
		int k;
		if (key == 0) {
			if (!hasZeroKey)
				return false;
			hasZeroKey = false;
			size--;
			return true;
		}
		int[] kt = keyTable;
		int m = mask;
		int i = hash(key);
		while ((k = kt[i]) != key) {
			if (k == 0)
				return false;
			i = (i + 1) & m;
		}
		int j = (i + 1) & m;
		while ((key = kt[j]) != 0) {
			int h = hash(key);
			if (((j - h) & m) > ((i - h) & m)) {
				kt[i] = key;
				i = j;
			}
			j = (j + 1) & m;
		}
		kt[i] = 0;
		size--;
		return true;
	}

	public void clear() {
		if (size == 0)
			return;
		size = 0;
		hasZeroKey = false;
		Arrays.fill(keyTable, 0);
	}

	public void clear(int maxCap) {
		int tableSize = tableSize(Math.max(maxCap, 0));
		if (tableSize >= keyTable.length) {
			clear();
			return;
		}
		size = 0;
		hasZeroKey = false;
		resize(tableSize);
	}

	public void shrink(int maxCap) {
		int tableSize = tableSize(Math.max(maxCap, size));
		if (tableSize < keyTable.length)
			resize(tableSize);
	}

	public void ensureCapacity(int cap) {
		int tableSize = tableSize(Math.max(cap, 0));
		if (tableSize > keyTable.length)
			resize(tableSize);
	}

	private void resize(int newSize) {
		int m;
		threshold = (int)(newSize * loadFactor);
		mask = m = newSize - 1;
		shift = Long.numberOfLeadingZeros(m);
		int[] kt = new int[newSize];
		if (size != 0) {
			block0:
			for (int k : keyTable) {
				if (k == 0)
					continue;
				int i = hash(k);
				for (; ; ) {
					if (kt[i] == 0) {
						kt[i] = k;
						continue block0;
					}
					i = (i + 1) & m;
				}
			}
		}
		keyTable = kt;
	}

	public void foreach(IntConsumer consumer) {
		if (hasZeroKey)
			consumer.accept(0);
		for (int k : keyTable) {
			if (k != 0)
				consumer.accept(k);
		}
	}

	public interface IntSetPredicate {
		boolean test(IntHashSet var1, int var2);
	}

	public boolean foreachTest(IntSetPredicate tester) {
		if (hasZeroKey && !tester.test(this, 0))
			return false;
		for (int k : keyTable) {
			if (k == 0 || tester.test(this, k))
				continue;
			return false;
		}
		return true;
	}

	public final class Iterator {
		private int idx = -2;

		public boolean moveToNext() {
			if (idx == -2) {
				idx = -1;
				if (hasZeroKey)
					return true;
			}
			final int[] kt = keyTable;
			for (final int lastIdx = kt.length - 1; idx < lastIdx; ) {
				if (kt[++idx] != 0)
					return true;
			}
			return false;
		}

		public int value() {
			return idx >= 0 ? keyTable[idx] : 0;
		}
	}

	public Iterator iterator() {
		return new Iterator();
	}

	@Override
	public IntHashSet clone() throws CloneNotSupportedException {
		IntHashSet set = (IntHashSet)super.clone();
		set.keyTable = keyTable.clone();
		return set;
	}

	@Override
	public String toString() {
		if (size == 0)
			return "{}";
		StringBuilder sb = new StringBuilder(32).append('{');
		int[] kt = keyTable;
		int n = Math.min(kt.length, 20);
		int i = 0, k;
		if (hasZeroKey)
			sb.append('0');
		else {
			while (i < n) {
				if ((k = kt[i++]) == 0)
					continue;
				sb.append(k);
				break;
			}
		}
		while (i < n) {
			k = kt[i];
			if (k != 0)
				sb.append(',').append(k);
			i++;
		}
		if (n != kt.length)
			sb.append(",...");
		return sb.append('}').toString();
	}
}
