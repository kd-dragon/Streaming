package com.kdy.bean.netty.handler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kdy.bean.handler.m3u8.MasterM3u8Handler;
import com.kdy.bean.handler.m3u8.VodM3u8HandlerFactory;
import com.kdy.bean.handler.ts.VodMpegTsHandlerFactory;
import com.kdy.dto.StreamMemoryVO;
import com.kdy.dto.VodFileVO;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;

@Component
@Sharable
public class HTTPVodStreamingHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
	
	private final static Logger log  = LoggerFactory.getLogger(HTTPLiveStreamingHandler.class);
	
	private final Pattern pattern = Pattern.compile("(\\d+)-(\\d+)-(\\d+)-(\\d+)-(\\S+)\\.ts");
	
	private final VodM3u8HandlerFactory 	m3u8HandlerFactory;
	private final VodMpegTsHandlerFactory 	mpegTsHandlerFactory;
	private final StreamMemoryVO 			streamMemoryVO;
	private final MasterM3u8Handler 		masterM3u8Handler;
	
	final AttributeKey<String> key = AttributeKey.newInstance("key");
	
	@Autowired
	public HTTPVodStreamingHandler(	  VodM3u8HandlerFactory 	m3u8HandlerFactory
									, VodMpegTsHandlerFactory 	mpegTsHandlerFactory
									, StreamMemoryVO 			streamMemoryVO
									, MasterM3u8Handler 		masterM3u8Handler
	) {
		this.m3u8HandlerFactory		= m3u8HandlerFactory;
		this.mpegTsHandlerFactory 	= mpegTsHandlerFactory;
		this.streamMemoryVO 		= streamMemoryVO;
		this.masterM3u8Handler 		= masterM3u8Handler;
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
		
		FullHttpResponse res = null;
		String uri = req.uri();
		
		/** # Adaptive HLS Streaming
		 * 
		 *  <Master M3U8 파일 생성> 
		 *  URI Pattern: /vod/{path}/{vid}/master.m3u8
		 *  ex) /vod/upload/encoding/video/2021/07/20210719000000000548/master.m3u8
		 */
		if (uri.contains("/vod") && uri.contains("master.m3u8")) {
			
			//master.m3u8
			String m3u8 = uri.substring(uri.lastIndexOf("/") + 1); 
			//vod sequence
			String vodFileSeq = uri.substring(uri.substring(0, uri.lastIndexOf("/")).lastIndexOf("/") + 1, uri.lastIndexOf(m3u8) - 1);
			//vod file path
			String vodFilePath = uri.replace("/vod", "").replace(vodFileSeq+"/"+m3u8, "");
			
			//log.info("master.m3u8\n[1] M3u8 Name: {}\n[2] vodFileSeq: {}\n[3] vodFilePath: {}", m3u8, vodFileSeq, vodFilePath);
			
			byte[] body = masterM3u8Handler.createM3u8File(vodFileSeq, vodFilePath);
			ByteBuf rtnBuf = ctx.alloc().heapBuffer(body.length);
			rtnBuf.writeBytes(body);
			
			res = new DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.OK, rtnBuf);
			
			setHeaderM3u8(res);
			setHttpUtil(ctx, res, req);
			
		/**
		 *  <개별 M3U8 파일 생성> 
		 *  URI Pattern
		 *  /vod/{fullpath}/{vid}/{quality/index}.m3u8
		 *  ex) /vod/upload/encoding/video/2021/07/20210719000000000548_media_320x240.mp4/index.m3u8
		 */	
		} else if (uri.contains("/vod") && (uri.contains("low.m3u8") || uri.contains("mid.m3u8") || uri.contains("high.m3u8") || uri.contains("index.m3u8"))) {
			
			//index.m3u8
			String m3u8 = uri.substring(uri.lastIndexOf("/") + 1); 
			//vod file name
			String vodFileName = uri.substring(uri.substring(0, uri.lastIndexOf("/")).lastIndexOf("/") + 1, uri.lastIndexOf(m3u8) - 1);
			//vod sequence
			String vodFileSeq = vodFileName.substring(0, vodFileName.indexOf("_"));
			//vod file path
			String vodFilePath = uri.replace("/vod", "").replace("/"+m3u8, "");
			//vod quality
			String vodQuality = m3u8.substring(0, m3u8.indexOf("."));
			
			//log.info("slave.m3u8\n[1] M3u8 Name: {}\n[2] vodFileName: {}\n[3] vodFileSeq: {}\n[4] vodFilePath: {}\n[5] vodQuality: {}", m3u8, vodFileName, vodFileSeq, vodFilePath, vodQuality);
			
			VodFileVO vodFileVO = new VodFileVO();
			vodFileVO.setVodFullFilePath(streamMemoryVO.getHlsPathVod() + vodFilePath);
			vodFileVO.setVodFileSeq(vodFileSeq);
			vodFileVO.setVodQuality(vodQuality);
			
			//VOD 파일 분석 및 M3u8 생성
			byte[] body = m3u8HandlerFactory.getHandler().createM3u8File(vodFileVO.getVodFileSeq() + "_" + vodQuality, vodFileVO.getVodFullFilePath());
			
			if(body != null) { 
				
				ByteBuf rtnBuf = ctx.alloc().heapBuffer(body.length);
				rtnBuf.writeBytes(body);
				
				//put vod path 
				if(streamMemoryVO.getVodFileCache(vodFileVO.getVodFileSeq()+vodQuality, vodFileVO) == null) {
					streamMemoryVO.updateVodFileCache(vodFileVO.getVodFileSeq()+vodQuality, vodFileVO);
				};
				
				res = new DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.OK, rtnBuf);
				
				setHeaderM3u8(res);
				setHttpUtil(ctx, res, req);
				
			} else {
				log.warn("BODY IS NULL");
				ctx.writeAndFlush(sendError(ctx, HttpResponseStatus.NOT_FOUND)); 
			}
			
		/**
		 *  <TS 파일 생성> 
		 *  URI Pattern: /vod/{fullpath}/segment-{seq}-{startIndex}-{endIndex}-{vodSeq}-{quality}.ts
		 *  ex) /vod/upload/encoding/video/2021/07/20210719000000000548_media_320x240.mp4/segment-6-1910150-2299660-20210719000000000548-low.ts
		 */	
		} else if(uri.contains("/vod") && (uri.contains(".ts"))) {
			//log.info("TS URI==== " + uri);
			String name = uri.substring(uri.lastIndexOf("/") + 1);
			Matcher m = pattern.matcher(name);
			long start = 0;
			long end = 0;
			int seq = 0;
			
			if(m.find()) {
				
				seq = Integer.valueOf(m.group(1)); //media-sequence
				start = Long.valueOf(m.group(2)); //start-point
				end = Long.valueOf(m.group(3)); //end-point
				
				// Get Vod File VO 
				VodFileVO vodFileVO = streamMemoryVO.getVodFileCache(m.group(4)+m.group(5), null);
				
				// socket channel 속성에 (channel ID + vod Sequence) 추가 (channel 별 메모리 관리용) 
				ctx.channel().attr(key).set(ctx.channel().id().toString() + m.group(4));
				if(vodFileVO == null) {
					ctx.writeAndFlush(sendError(ctx, HttpResponseStatus.NOT_FOUND));
					log.error("VOD File is NULL");
				} else {
					
					byte[] body = mpegTsHandlerFactory.getHandler().encodeVodChunkFile(vodFileVO.getVodFileSeq() + "_" + seq, vodFileVO.getVodFileSeq()+m.group(5), vodFileVO.getVodFullFilePath(), ctx, start, end);
					
					if(body != null) { 
						ByteBuf rtnBuf = Unpooled.wrappedBuffer(body);
						res = new DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.OK, rtnBuf);
						
						// HTTP Response Header Mpeg-ts 설정
						setHeaderTS(res);
						setHttpUtil(ctx, res, req);
					} else {
						ctx.writeAndFlush(sendError(ctx, HttpResponseStatus.NOT_FOUND)); 
					}
				}
			} else {
				log.error("Invalid Uri Pattern :: " + uri.substring(uri.lastIndexOf("/") + 1));
				ctx.writeAndFlush(sendError(ctx, HttpResponseStatus.BAD_REQUEST));
			}
			
		} else {
			ctx.writeAndFlush(sendError(ctx, HttpResponseStatus.NOT_FOUND));
		}

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		for(StackTraceElement st : cause.getStackTrace()) { 
			if(st.toString().startsWith("com.kdy")) {
				log.error(st.toString()); 
			}
		}
		log.error(cause.getMessage());
		ctx.close();
	}
	
	
	private void setHttpUtil(ChannelHandlerContext ctx, FullHttpResponse res, FullHttpRequest req) throws Exception {
		HttpUtil.setContentLength(res, res.content().readableBytes());		
		
		if(HttpUtil.isKeepAlive(req) && res.status().code() == 200) {
			HttpUtil.setKeepAlive(res, true);
			res.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");
			ctx.writeAndFlush(res);
		} else {
			ctx.writeAndFlush(sendError(ctx, HttpResponseStatus.BAD_REQUEST));
		}
	}
	
	private FullHttpResponse sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
    	FullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Fail : " + status, CharsetUtil.UTF_8));
    	res.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
    	log.info("SEND ERROR :: " + status.toString() + " Channel ID : " + ctx.channel().id().toString());
    	return res;
    }
	
	private void setHeaderM3u8(FullHttpResponse res) {
		
		res.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range")
				.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, OPTIONS")
				.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
				.set(HttpHeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS, "Content-Length,Content-Range")
				.set(HttpHeaderNames.CACHE_CONTROL, "no-cache")
				.add(HttpHeaderNames.PRAGMA, "no-cache")
				.add(HttpHeaderNames.CONTENT_TYPE, "application/vnd.apple.mpegurl")
				.set(HttpHeaderNames.SERVER, "tgstream")
				.set(HttpHeaderNames.CONTENT_DISPOSITION, "inline;filename=f.m3u8")
				.set(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
		
	}
	
	private void setHeaderTS(FullHttpResponse res) {
		
		res.headers().add(HttpHeaderNames.ACCEPT_RANGES, "bytes")
				.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range")
				.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, OPTIONS")
				.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
				.set(HttpHeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS, "Content-Length,Content-Range")
				.set(HttpHeaderNames.CACHE_CONTROL, "no-cache")
				.add(HttpHeaderNames.PRAGMA, "no-cache")
				.add(HttpHeaderNames.CONTENT_TYPE, "video/MP2T")
				.set(HttpHeaderNames.CONTENT_DISPOSITION, "inline;filename=f.ts")
				.set(HttpHeaderNames.SERVER, "tgstream");
	}
}
