package com.kdy.bean.netty.handler;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.rtsp.RtspHeaderNames;
import io.netty.handler.codec.rtsp.RtspHeaderValues;
import io.netty.handler.codec.rtsp.RtspMethods;
import io.netty.handler.codec.rtsp.RtspResponseStatuses;
import io.netty.handler.codec.rtsp.RtspVersions;
import io.netty.util.CharsetUtil;

@Component
@Sharable
public class RTSPStreamingHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
	
	private final Logger log  = LoggerFactory.getLogger(RTSPStreamingHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
		
        FullHttpResponse rep = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0,  RtspResponseStatuses.NOT_FOUND);
        
        log.info("#### content \n" + req.content());
        log.info("#### method \n" + req.method());
        log.info("#### protocolVersion \n" + req.protocolVersion());
        log.info("#### headers \n" + req.headers());
        log.info("#### headers String \n" + req.headers().toString());
        
        if (req.method() == RtspMethods.OPTIONS){	
        	
            rep.setStatus(RtspResponseStatuses.OK);
            rep.headers().add(RtspHeaderValues.PUBLIC, "DESCRIBE, SETUP, PLAY, TEARDOWN");
            sendAnswer(ctx, req, rep);
            
        } else if (req.method() == RtspMethods.DESCRIBE){
        	
        	InetSocketAddress addr = (InetSocketAddress) ctx.channel().localAddress();
        	String sdp = String.format("c=IN IP4 %s \nm=video 0 RTP/AVP 96\na=rtpmap:96 H264/90000\n", addr.getHostString());
            //ByteBuf buf = Unpooled.copiedBuffer("c=IN IP4 10.5.110.117\r\nm=video 5004 RTP/AVP 96\r\na=rtpmap:96 H264/90000\r\n", CharsetUtil.UTF_8);
            ByteBuf buf = Unpooled.copiedBuffer(sdp, CharsetUtil.UTF_8);
            rep.setStatus(RtspResponseStatuses.OK);
            rep.headers().add(RtspHeaderNames.CONTENT_TYPE, "application/sdp");
            rep.headers().add(RtspHeaderNames.CONTENT_LENGTH, buf.writerIndex());
            rep.content().writeBytes(buf);
            sendAnswer(ctx, req, rep);
            
        } else if (req.method() == RtspMethods.SETUP){
        	
            rep.setStatus(RtspResponseStatuses.OK);
            String session = String.format("%08x",(int)(Math.random()*65536));
            rep.headers().add(RtspHeaderNames.SESSION, session);
            rep.headers().add(RtspHeaderNames.TRANSPORT,"RTP/AVP;unicast;client_port=5004-5005");
            sendAnswer(ctx, req, rep);
            
        } else if (req.method() == RtspMethods.PLAY){
        	
            rep.setStatus(RtspResponseStatuses.OK);
            sendAnswer(ctx, req, rep);
            
        } else {
            log.error("Not managed :" + req.method());					
            ctx.write(rep).addListener(ChannelFutureListener.CLOSE);
        }
	}
	
	private void sendAnswer(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse rep){
        String cseq = req.headers().get(RtspHeaderNames.CSEQ);
        if (cseq != null){
            rep.headers().add(RtspHeaderNames.CSEQ, cseq);
        }
        
        String session = req.headers().get(RtspHeaderNames.SESSION);
        if (session != null){
            rep.headers().add(RtspHeaderNames.SESSION, session);
        }
        
        if (!HttpUtil.isKeepAlive(req)) {
            ctx.write(rep).addListener(ChannelFutureListener.CLOSE);
        } else {
            rep.headers().set(RtspHeaderNames.CONNECTION, RtspHeaderValues.KEEP_ALIVE);
            ctx.write(rep);
        }					
    }
	
	
	
}
