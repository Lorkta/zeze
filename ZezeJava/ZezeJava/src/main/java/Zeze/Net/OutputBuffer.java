package Zeze.Net;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;

interface ByteBufferAllocator {
	public static final int DEFAULT_SIZE = 32 * 1024;

	default ByteBuffer alloc() {
		return ByteBuffer.allocateDirect(DEFAULT_SIZE);
	}

	default void free(@SuppressWarnings("unused") ByteBuffer bb) {
	}
}

// 非线程安全,通常只能在selector线程调用
final class OutputBuffer implements Codec, Closeable {
	private final ByteBufferAllocator allocator;
	private final ArrayDeque<ByteBuffer> buffers = new ArrayDeque<>();
	private final ByteBuffer[] outputs = new ByteBuffer[2];
	private ByteBuffer head, tail; // head <- buffers <- tail
	private int tailPos;
	private int size;

	public OutputBuffer(ByteBufferAllocator allocator) {
		this.allocator = allocator;
		tail = allocator.alloc();
	}

	@Override
	public void close() {
		if (head != null) {
			allocator.free(head);
			head = null;
		}
		for (ByteBuffer bb; (bb = buffers.pollFirst()) != null; )
			allocator.free(bb);
		allocator.free(tail);
		tail = null;
	}

	public int size() {
		return size;
	}

	public void put(byte[] src) {
		put(src, 0, src.length);
	}

	public void put(byte[] src, int offset, int length) {
		if (length > 0) {
			for (size += length; ; ) {
				int left = tail.remaining();
				if (left >= length) {
					tail.put(src, offset, length);
					break;
				}
				tail.put(src, offset, left);
				tail.limit(tail.position());
				tail.position(tailPos);
				buffers.addLast(tail);
				tail = allocator.alloc();
				tailPos = 0;
				offset += left;
				length -= left;
			}
		}
	}

	public long writeTo(SocketChannel channel) throws IOException {
		long r;
		if (head == null && (head = buffers.pollFirst()) == null) { // head和队列都没有buffer了,只需要输出tail
			var tail = this.tail;
			int writePos = tail.position();
			if (writePos <= tailPos) // tail没有数据
				return 0;
			tail.limit(writePos);
			tail.position(tailPos);
			r = channel.write(tail);
			int newTailPos = tail.position();
			if (newTailPos >= writePos) // tail全部输出完
				newTailPos = writePos = 0;
			tailPos = newTailPos;
			tail.position(writePos);
			tail.limit(tail.capacity());
		} else {
			var next = buffers.peekFirst();
			if (next == null) {
				var tail = this.tail;
				if (tail.position() == 0) { // 队列只有对头,且tail没有数据
					r = channel.write(head);
					if (!head.hasRemaining()) { // 队头已经输出完
						allocator.free(head);
						head = null;
					}
				} else { // 队列只有对头,且tail有数据
					outputs[0] = head;
					outputs[1] = tail;
					int writePos = tail.position();
					tail.limit(writePos);
					tail.position(0);
					r = channel.write(outputs);
					if (!head.hasRemaining()) { // 队头已经输出完
						allocator.free(head);
						head = null;
						int newTailPos = tail.position();
						if (newTailPos >= writePos) // tail全部输出完
							newTailPos = writePos = 0;
						tailPos = newTailPos;
						tail.position(writePos);
					} else
						tail.position(writePos);
					tail.limit(tail.capacity());
				}
			} else { // 输出head和队头的2个buffers
				outputs[0] = head;
				outputs[1] = next;
				r = channel.write(outputs);
				if (!head.hasRemaining()) { // 第1个输出完了
					allocator.free(head);
					head = null;
					if (!next.hasRemaining()) { // 第2个也输出完了
						allocator.free(next);
						buffers.removeFirst();
					}
				}
			}
		}
		if (r > 0)
			size -= r;
		return r;
	}

	@Override
	public void update(byte c) throws CodecException {
		put(new byte[]{ c }, 0, 1);
	}

	@Override
	public void update(byte[] data, int off, int len) throws CodecException {
		put(data, off, len);
	}

	@Override
	public void flush() throws CodecException {

	}
}
