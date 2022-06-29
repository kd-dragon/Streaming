package com.kdy.bean.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kdy.dto.NettyVO;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

@Component
public class NettyServer {
	
	private final static Logger log = LoggerFactory.getLogger(NettyServer.class);
	
	@Autowired
	private NettyVO vo;
	
	public void service(ChannelHandler handler, int PORT, String type) {
		log.debug("NettyServer service()");
		
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);
		
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.handler(new LoggingHandler(LogLevel.DEBUG))
				.option(ChannelOption.SO_BACKLOG, 1024)
				//.option(ChannelOption.SO_REUSEADDR, true)
				.childOption(ChannelOption.TCP_NODELAY, true)
				.childOption(ChannelOption.SO_LINGER, 0)
				.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
				//.childOption(ChannelOption.SO_KEEPALIVE, true)
				.childHandler(handler);

			b.bind(PORT).sync().channel().closeFuture().sync();
		} catch (Exception e) {
			for(StackTraceElement st : e.getStackTrace()) {
				if(st.toString().startsWith("com.kdy")) {
					log.error(st.toString());
				}
			}
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
			log.info("NettyServer close");
			vo.setThread(type, null);
		}
	}
}
