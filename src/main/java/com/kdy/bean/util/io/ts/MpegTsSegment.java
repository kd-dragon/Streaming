package com.kdy.bean.util.io.ts;

import org.apache.mina.core.buffer.IoBuffer;

import com.kdy.bean.util.common.hex.HexDump;

public class MpegTsSegment {

	// name of the segment
	private String name;

	// segment seq number
	private int sequence;

	// creation time
	private long created = System.currentTimeMillis();

	// queue for holding data if using memory mapped i/o
	//private volatile IoBuffer buffer;
	private IoBuffer buffer;

	// lock used when writing or slicing the buffer
	//private volatile ReentrantLock lock = new ReentrantLock();

	// whether or not the segment is closed
	//private volatile boolean closed = false;
	private boolean closed = false;

	private String encKey;

	private byte[] encKeyBytes;

	public MpegTsSegment(String name, int sequence) {
		this.name = name;
		this.sequence = sequence;
		buffer = IoBuffer.allocate(1024 * 1024);
		buffer.setAutoExpand(true);
		//buffer = UnpooledByteBufAllocator.DEFAULT.heapBuffer();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getSequence() {
		return sequence;
	}

	public void setSequence(int sequence) {
		this.sequence = sequence;
	}

	public long getCreated() {
		return created;
	}

	public IoBuffer getBuffer() {
		return buffer;
	}

	public void setBuffer(IoBuffer buf) {
		buffer = buf;
	}

	public String getEncKey() {
		return encKey;
	}

	public void setEncKey(String encKey) {
		this.encKey = encKey;
		if (encKey != null) {
			this.encKeyBytes = HexDump.decodeHexString(encKey);
		}
	}

	public byte[] getEncKeyBytes() {
		return encKeyBytes;
	}
	
	public boolean close() {
		boolean result = false;
		if (buffer != null) {
			//lock.lock();
			closed = true;
			try {
				//buffer.flip();
				buffer.clear();
				result = true;
			} finally {
				//lock.unlock();
			}
		}
		return result;
	}

	/**
	 * Should be called only when we are completely finished with this segment
	 * and no longer want it to be available.
	 */
	public void dispose() {
		if (buffer != null) {
			//buffer.free();
			buffer = null;
		}
	}

	public boolean isClosed() {
		return closed;
	}
}
