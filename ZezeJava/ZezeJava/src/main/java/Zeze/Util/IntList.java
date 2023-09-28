package Zeze.Util;

import java.util.Collection;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IntList implements Comparable<IntList>, Cloneable, Serializable {
	public static final int[] EMPTY = new int[0];
	public static final int DEFAULT_SIZE = 8;

	protected int @NotNull [] _buffer = EMPTY;
	protected int _count;

	public static @NotNull IntList wrap(int @NotNull [] data, int count) {
		IntList il = new IntList();
		il._buffer = data;
		il._count = count > data.length ? data.length : Math.max(count, 0);
		return il;
	}

	public static @NotNull IntList wrap(int @NotNull [] data) {
		IntList il = new IntList();
		il._buffer = data;
		il._count = data.length;
		return il;
	}

	public static @NotNull IntList createSpace(int count) {
		IntList il = new IntList();
		if (count > 0) {
			il._buffer = new int[count];
			il._count = count;
		}
		return il;
	}

	public IntList() {
	}

	public IntList(int count) {
		reserveSpace(count);
	}

	@SuppressWarnings("CopyConstructorMissesField")
	public IntList(@NotNull IntList il) {
		replace(il);
	}

	public IntList(int @NotNull [] data) {
		replace(data);
	}

	public IntList(int @NotNull [] data, int fromIdx, int count) {
		replace(data, fromIdx, count);
	}

	public int @NotNull [] array() {
		return _buffer;
	}

	public boolean isEmpty() {
		return _count <= 0;
	}

	public int size() {
		return _count;
	}

	public int capacity() {
		return _buffer.length;
	}

	public int get(int idx) {
		return _buffer[idx];
	}

	public void set(int idx, int value) {
		_buffer[idx] = value;
	}

	public int addValue(int idx, int value) {
		_buffer[idx] = value += _buffer[idx];
		return value;
	}

	public void clear() {
		_count = 0;
	}

	public void reset() {
		_buffer = EMPTY;
		_count = 0;
	}

	public int @NotNull [] toArray() {
		int n = _count;
		if (n <= 0)
			return EMPTY;
		int[] buf = new int[n];
		System.arraycopy(_buffer, 0, buf, 0, n);
		return buf;
	}

	public int @NotNull [] toArray(int fromIdx, int count) {
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= _count || count <= 0)
			return EMPTY;
		int n = fromIdx + count;
		n = n < 0 || n > _count ? _count - fromIdx : count;
		int[] buf = new int[n];
		System.arraycopy(_buffer, fromIdx, buf, 0, n);
		return buf;
	}

	public @NotNull IntList wraps(int @NotNull [] data, int count) {
		_buffer = data;
		_count = count > data.length ? data.length : Math.max(count, 0);
		return this;
	}

	public @NotNull IntList wraps(int @NotNull [] data) {
		_buffer = data;
		_count = data.length;
		return this;
	}

	public void shrink(int count) {
		int[] buffer;
		int n = _count;
		if (n <= 0) {
			reset();
			return;
		}
		if (count < n)
			count = n;
		if (count >= (buffer = _buffer).length)
			return;
		int[] buf = new int[count];
		System.arraycopy(buffer, 0, buf, 0, n);
		_buffer = buf;
	}

	public void shrink() {
		shrink(0);
	}

	public void reserve(int count) {
		int[] buffer = _buffer;
		if (count > buffer.length) {
			int cap;
			for (cap = DEFAULT_SIZE; count > cap; cap <<= 1) {
				// empty
			}
			int[] buf = new int[cap];
			int n = _count;
			if (n > 0)
				System.arraycopy(buffer, 0, buf, 0, n);
			_buffer = buf;
		}
	}

	public void reserveSpace(int count) {
		if (count > _buffer.length) {
			int cap;
			for (cap = 8; count > cap; cap <<= 1) {
				// empty
			}
			_buffer = new int[cap];
		}
	}

	public void resize(int count) {
		if (count <= 0)
			count = 0;
		else
			reserve(count);
		_count = count;
	}

	public void replace(int @NotNull [] data, int fromIdx, int count) {
		if (count <= 0) {
			_count = 0;
			return;
		}
		int len = data.length;
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= len) {
			_count = 0;
			return;
		}
		if (count > (len -= fromIdx))
			count = len;
		reserveSpace(count);
		System.arraycopy(data, fromIdx, _buffer, 0, count);
		_count = count;
	}

	public void replace(int @NotNull [] data) {
		replace(data, 0, data.length);
	}

	public void replace(@NotNull IntList il) {
		replace(il._buffer, 0, il._count);
	}

	public void swap(@NotNull IntList il) {
		int count = _count;
		_count = il._count;
		il._count = count;
		int[] buf = il._buffer;
		il._buffer = _buffer;
		_buffer = buf;
	}

	public @NotNull IntList add(int value) {
		int n = _count;
		int nNew = n + 1;
		reserve(nNew);
		_buffer[n] = value;
		_count = nNew;
		return this;
	}

	public @NotNull IntList addAll(int @NotNull [] data, int fromIdx, int count) {
		if (count <= 0)
			return this;
		int len = data.length;
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= len)
			return this;
		if (count > (len -= fromIdx))
			count = len;
		int n = _count;
		reserve(n + count);
		System.arraycopy(data, fromIdx, _buffer, n, count);
		_count = n + count;
		return this;
	}

	public @NotNull IntList addAll(int @NotNull [] data) {
		return addAll(data, 0, data.length);
	}

	public @NotNull IntList addAll(@NotNull IntList il) {
		return addAll(il._buffer, 0, il._count);
	}

	public @NotNull IntList addAll(@NotNull Collection<Integer> c) {
		int n = _count;
		reserve(n + c.size());
		int[] buf = _buffer;
		for (Integer v : c)
			buf[n++] = v;
		_count = n;
		return this;
	}

	public @NotNull void addAllTo(@NotNull Collection<Integer> c) {
		int[] buf = _buffer;
		for (int i = 0, n = _count; i < n; i++)
			c.add(buf[i]);
	}

	public @NotNull IntList insert(int fromIdx, int data) {
		int n = _count;
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= n)
			return add(data);
		reserve(n + 1);
		int[] buf = _buffer;
		System.arraycopy(buf, fromIdx, buf, fromIdx + 1, n - fromIdx);
		buf[fromIdx] = data;
		_count = n + 1;
		return this;
	}

	public @NotNull IntList insert(int fromIdx, int @NotNull [] data, int idx, int count) {
		int n = _count;
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= n)
			return addAll(data, idx, count);
		if (count <= 0)
			return this;
		int len = data.length;
		if (idx < 0)
			idx = 0;
		if (idx >= len)
			return this;
		if (count > (len -= idx))
			count = len;
		reserve(n + count);
		int[] buf = _buffer;
		System.arraycopy(buf, fromIdx, buf, fromIdx + count, n - fromIdx);
		System.arraycopy(data, idx, buf, fromIdx, count);
		_count = n + count;
		return this;
	}

	public @NotNull IntList insert(int fromIdx, int @NotNull [] data) {
		return insert(fromIdx, data, 0, data.length);
	}

	public @NotNull IntList insert(int fromIdx, @NotNull IntList il) {
		return insert(fromIdx, il._buffer, 0, il._count);
	}

	public @NotNull IntList remove(int idx) {
		int lastIdx = _count - 1;
		if (idx < 0 || idx > lastIdx)
			return this;
		_count = lastIdx;
		if (idx != lastIdx)
			System.arraycopy(_buffer, idx + 1, _buffer, idx, lastIdx - idx);
		return this;
	}

	public @NotNull IntList removeAndExchangeLast(int idx) {
		int lastIdx = _count - 1;
		if (idx >= 0 && idx <= lastIdx) {
			_count = lastIdx;
			_buffer[idx] = _buffer[lastIdx];
		}
		return this;
	}

	public @NotNull IntList erase(int fromIdx, int toIdx) {
		int n = _count;
		if (fromIdx < 0)
			fromIdx = 0;
		if (fromIdx >= n || fromIdx >= toIdx)
			return this;
		if (toIdx >= n)
			_count = fromIdx;
		else {
			System.arraycopy(_buffer, toIdx, _buffer, fromIdx, n -= toIdx);
			_count = n + fromIdx;
		}
		return this;
	}

	public @NotNull IntList eraseFront(int count) {
		int n = _count;
		if (count >= n)
			_count = 0;
		else if (count > 0) {
			System.arraycopy(_buffer, count, _buffer, 0, n -= count);
			_count = n;
		}
		return this;
	}

	public int indexOf(int value) {
		return indexOf(value, 0);
	}

	public int indexOf(int value, int fromIdx) {
		int[] buf = _buffer;
		int n = _count;
		for (int i = fromIdx; i < n; i++) {
			if (buf[i] != value)
				continue;
			return i;
		}
		return -1;
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public @NotNull IntList clone() {
		return new IntList(this);
	}

	@Override
	public int hashCode() {
		int[] buf = _buffer;
		int n = _count;
		int result = n;
		if (n <= 32) {
			for (int i = 0; i < n; i++)
				result = 31 * result + buf[i];
		} else {
			int i;
			for (i = 0; i < 16; i++)
				result = 31 * result + buf[i];
			for (i = n - 16; i < n; i++)
				result = 31 * result + buf[i];
		}
		return result;
	}

	@Override
	public int compareTo(@Nullable IntList il) {
		if (il == null)
			return 1;
		int n0 = _count;
		int n1 = il._count;
		int n = Math.min(n0, n1);
		int[] buf = _buffer;
		int[] data = il._buffer;
		for (int i = 0; i < n; i++) {
			int c = buf[i] - data[i];
			if (c == 0)
				continue;
			return c;
		}
		return n0 - n1;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (o == this)
			return true;
		if (!(o instanceof IntList))
			return false;
		IntList il = (IntList)o;
		int n = _count;
		if (n != il._count)
			return false;
		int[] buf = _buffer;
		int[] data = il._buffer;
		for (int i = 0; i < n; i++) {
			if (buf[i] == data[i])
				continue;
			return false;
		}
		return true;
	}

	public boolean equals(@Nullable IntList il) {
		if (il == this)
			return true;
		if (il == null)
			return false;
		int n = _count;
		if (n != il._count)
			return false;
		int[] buf = _buffer;
		int[] data = il._buffer;
		for (int i = 0; i < n; i++) {
			if (buf[i] == data[i])
				continue;
			return false;
		}
		return true;
	}

	public void foreach(@NotNull IntConsumer consumer) {
		int[] buf = _buffer;
		int n = _count;
		for (int i = 0; i < n; i++)
			consumer.accept(buf[i]);
	}

	public boolean foreachPred(@NotNull IntPredicate predicate) {
		int[] buf = _buffer;
		int n = _count;
		for (int i = 0; i < n; i++) {
			if (!predicate.test(buf[i]))
				return false;
		}
		return true;
	}

	public @NotNull StringBuilder dump(@NotNull StringBuilder sb) {
		sb.append('[');
		int[] buf = _buffer;
		int n = _count;
		if (n > 0) {
			for (int i = 0; ; ) {
				sb.append(buf[i]);
				if (++i >= n)
					break;
				sb.append(',');
			}
		}
		return sb.append(']');
	}

	public @NotNull String dump() {
		int n = _count;
		return n > 0 ? dump(new StringBuilder(n * 2)).toString() : "[]";
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		int n = _count;
		bb.WriteUInt(n);
		if (n > 0) {
			int[] buf = _buffer;
			for (int i = 0; i < n; i++)
				bb.WriteInt(buf[i]);
		}
	}

	@Override
	public void decode(@NotNull ByteBuffer bb) {
		decode(bb, bb.ReadUInt());
	}

	public void encode(@NotNull ByteBuffer bb, int n) {
		if (_count != n)
			throw new java.util.ConcurrentModificationException(String.valueOf(_count));
		if (n > 0) {
			int[] buf = _buffer;
			for (int i = 0; i < n; i++)
				bb.WriteInt(buf[i]);
		}
	}

	public void decode(@NotNull ByteBuffer bb, int n) {
		reserveSpace(n);
		int[] buf = _buffer;
		for (int i = 0; i < n; i++)
			buf[i] = bb.ReadInt();
		_count = n;
	}

	@Override
	public @NotNull String toString() {
		return "[" + _count + "/" + _buffer.length + "]";
	}
}
