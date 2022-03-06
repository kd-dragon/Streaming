package com.kdy.bean.handler.live.IF;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public interface FileHandlerInterface {
	public ByteBuf getByteBuf(String key, ByteBufAllocator alloc);
}
