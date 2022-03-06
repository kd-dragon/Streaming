package com.kdy.bean.netty.initialize;

import java.io.File;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kdy.bean.netty.handler.RTSPStreamingHandler;
import com.kdy.dto.NettyVO;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.rtsp.RtspDecoder;
import io.netty.handler.codec.rtsp.RtspEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

@Component
public class NettyRtspStreamingInitializer extends ChannelInitializer<SocketChannel> {
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private final RTSPStreamingHandler rtspStreamingHandler;
	private final NettyVO nettyVO;
	
	@Autowired
	public NettyRtspStreamingInitializer(RTSPStreamingHandler rtspStreamingHandler, NettyVO nettyVO) {
		this.rtspStreamingHandler = rtspStreamingHandler;
		this.nettyVO = nettyVO;
	}
	
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		logger.debug("RTSP StreamingInitializer initChannel()_");
		CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin().build();
		
		ChannelPipeline p = ch.pipeline();
		
		SslContext sslCtx = null;
		if(nettyVO.getRtspSslUseYn().equalsIgnoreCase("Y")) {
			sslCtx = getCertificate();
			if(sslCtx != null) {
				p.addLast(sslCtx.newHandler(ch.alloc()));
			}
		}
		p.addLast(new RtspDecoder(), new RtspEncoder());
		p.addLast(new HttpObjectAggregator(64 * 1024));
		p.addLast(rtspStreamingHandler);
		//p.addLast(new CorsHandler(corsConfig));
		
	}
	
	private SslContext getCertificate() throws SSLException {
		
		if(nettyVO.getRtspSslCertPath() != null && nettyVO.getRtspSslKeyPath() != null) {
			File cert = new File(nettyVO.getRtspSslCertPath());
			File key = new File(nettyVO.getRtspSslKeyPath());
			return SslContextBuilder.forServer(cert, key)
									.build();
		} else {
			logger.error("RTSP SSL Certification Path Is Null");
			return null;
		}
	}

}
