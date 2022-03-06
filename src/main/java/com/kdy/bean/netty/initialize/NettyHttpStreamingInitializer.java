package com.kdy.bean.netty.initialize;

import java.io.File;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kdy.bean.netty.handler.HTTPLiveStreamingHandler;
import com.kdy.bean.netty.handler.HTTPVodStreamingHandler;
import com.kdy.dto.NettyVO;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

@Component
public class NettyHttpStreamingInitializer extends ChannelInitializer<SocketChannel> {
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private final HTTPLiveStreamingHandler liveStreamingHandler;
	private final HTTPVodStreamingHandler vodStreamingHandler;
	private final NettyVO nettyVO;
	
	@Autowired
	public NettyHttpStreamingInitializer(HTTPLiveStreamingHandler liveStreamingHandler, HTTPVodStreamingHandler vodStreamingHandler, NettyVO nettyVO) {
		this.liveStreamingHandler = liveStreamingHandler;
		this.vodStreamingHandler = vodStreamingHandler;
		this.nettyVO = nettyVO;
	}
	
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		logger.debug("HTTP StreamingInitializer initChannel()_");
		CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin().build();
		
		ChannelPipeline p = ch.pipeline();
		
		SslContext sslCtx = null;
		if(nettyVO.getHttpSslUseYn().equalsIgnoreCase("Y")) {
			sslCtx = getCertificate();
			if(sslCtx != null) {
				p.addLast(sslCtx.newHandler(ch.alloc()));
			}
		}
		//p.addLast(new IdleStateHandler(0, 0, 10));
		p.addLast(new HttpServerCodec());
		p.addLast(new HttpObjectAggregator(64 * 1024));
		p.addLast(new CorsHandler(corsConfig));
		
		// 스트리밍 모드 ( VOD / Live )
		if(nettyVO.getHttpStreamingMode().equalsIgnoreCase("live")) {
			p.addLast(liveStreamingHandler);
		} else {
			p.addLast(vodStreamingHandler);
		}
	}
	
	private SslContext getCertificate() throws SSLException {
		
		if(nettyVO.getHttpSslCertPath() != null && nettyVO.getHttpSslKeyPath() != null) {
			File cert = new File(nettyVO.getHttpSslCertPath());
			File key = new File(nettyVO.getHttpSslKeyPath());
			return SslContextBuilder.forServer(cert, key)
									.build();
		} else {
			logger.error("HTTP SSL Certification Path Is Null");
			return null;
		}
	}

}
