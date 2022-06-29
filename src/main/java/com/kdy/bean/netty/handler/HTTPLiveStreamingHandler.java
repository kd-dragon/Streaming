package com.kdy.bean.netty.handler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.kdy.bean.handler.live.RedisSentinelFileHandler;
import com.kdy.bean.handler.m3u8.MasterM3u8Handler;
import com.kdy.dto.NoSignalVO;
import com.kdy.dto.URIDecodeVO;

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
import io.netty.util.CharsetUtil;
import lombok.RequiredArgsConstructor;

@Component
@Sharable
@RequiredArgsConstructor
public class HTTPLiveStreamingHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
	
	private final Logger log  = LoggerFactory.getLogger(HTTPLiveStreamingHandler.class);
	
	private final RedisSentinelFileHandler 	redisFileHandler;
	private final NoSignalVO 		noSignalVO;
	private final MasterM3u8Handler masterM3u8Handler;
	
	/**
	 * @author KDY
	 * HTTP SimpleChannelInboundHandler <FullHttpRequest>
	 * - channelRead0: Channel에서 데이터 읽을 때 호출
	 * - exceptionCaught: ChannelPipeline 오류 발생시 호출  
	 */
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
		
		FullHttpResponse res = null;
		String uri = req.uri();
		
		// URI handler (요청 URI 해석 및 substring)
		URIDecodeVO uriVo = uriDecodeHandler(uri);
		
		//해당되는 m3u8, ts 파일을 Redis에서 get 
		ByteBuf body = getContents(uriVo.getSeq(), uriVo.getName(), ctx);
		
		//받아온 데이터를 HTTP 응답 객체에 담기
		res = new DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.OK, body);
		
		// m3u8 혹은 ts 파일 가져오기
		if(uri.contains("/live") && uri.contains("index.m3u8")) {
			// Redis에 스트리밍 데이터가 없을 시 대체 기본 파일 처리
			if(body == null) {
				body = setNoSignalM3u8(ctx);
			}
			// HTTP Header 설정 후 writeAndFlush() 호출
			setHeaderM3u8(res);
		} else if(uri.contains("/live") && (uri.contains(".ts"))) {
			if(body == null) {
				body = setNoSignalTS(ctx);
			}
			setHeaderTS(res);
		} 
		
		setHttpUtilAndWrite(ctx, res, req);
	}
	
	protected void channelRead(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
		
		FullHttpResponse res = null;
		String uri = req.uri();
		
		// URI handler
		String name = uri.substring(uri.lastIndexOf("/") + 1);
		String tempUrl = uri.substring(0, uri.lastIndexOf("/"));
		String seq = tempUrl.substring(tempUrl.lastIndexOf("/") + 1);
		
		//Adaptive HLS 방식 사용시
		if(uri.contains("/live") && uri.contains("master.m3u8")) {
			
			//master.m3u8
			String m3u8 = uri.substring(uri.lastIndexOf("/") + 1);
			//live Sequence
			String liveFileSeq = uri.substring(uri.substring(0, uri.lastIndexOf("/")).lastIndexOf("/") + 1, uri.lastIndexOf(m3u8) - 1);
			//live file path
			String liveFilePath = uri.replace("/live", "").replace(liveFileSeq + "/" + m3u8, "");
			
			//log.info("master.m3u8\n[1] M3u8 Name : {} \n[2] liveFileSeq : {}\n[3]", m3u8, liveFileSeq, liveFilePath);
		
			byte[] liveBody = masterM3u8Handler.createLiveM3u8File(liveFileSeq, liveFilePath);
			ByteBuf rtnBuf = ctx.alloc().heapBuffer(liveBody.length);
			rtnBuf.writeBytes(liveBody);
			
			res = new DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.OK, rtnBuf);
			
			setHeaderM3u8(res);
		}
		
		// m3u8 혹은 ts 파일 가져오기
		if(uri.contains("/live") && uri.contains("index.m3u8")) {
			
			ByteBuf body = getContents(seq, name, ctx);
			// no signal 처리
			if(body == null) body = setNoSignalM3u8(ctx);
			res = new DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.OK, body);
			// M3U8 File Header Setting
			setHeaderM3u8(res);

		} else if(uri.contains("/live") && (uri.contains(".ts") || uri.contains(".TS"))) {
			
			ByteBuf body = getContents(seq, name, ctx);
			// no signal 처리
			if(body == null) body = setNoSignalTS(ctx);
			res = new DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.OK, body);
			// TS File Header Setting
			setHeaderTS(res);

			// 관리자 -> 스트리밍서버 관리 -> 스트리밍 상태 확인
		} else if(uri.contains("/monitor")){
			res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
	    	res.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
	    	//HttpUtil.setKeepAlive(res, false);
			ctx.writeAndFlush(res);
			return;
		}
		
		setHttpUtilAndWrite(ctx, res, req);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		
		log.error("error :\n{}", cause);
		ctx.close();
	}
	
	private void setHttpUtilAndWrite(ChannelHandlerContext ctx, FullHttpResponse res, FullHttpRequest req) throws Exception {
		HttpUtil.setContentLength(res, res.content().readableBytes());		
		
		if(HttpUtil.isKeepAlive(req) && res.status().code() == 200) {
			HttpUtil.setKeepAlive(res, true);
			ctx.writeAndFlush(res);
		} 
		
		ctx.writeAndFlush(sendError(ctx, HttpResponseStatus.BAD_REQUEST));
		
	}
    
    private FullHttpResponse sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
    	FullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Fail : " + status, CharsetUtil.UTF_8));
    	res.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
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
	
	//Get Video Byte 
	private ByteBuf getContents(String seq, String name, ChannelHandlerContext ctx) {
		String liveKey = seq + "_" + name;
		//log.info("getContents Key :: {} ", liveKey);
		return redisFileHandler.getByteBuf(liveKey, ctx.alloc());
	}
	
	private ByteBuf setNoSignalM3u8(ChannelHandlerContext ctx) {
    	return ctx.alloc().heapBuffer().writeBytes(noSignalVO.getNoSignalM3u8());
	}
	
	private ByteBuf setNoSignalTS(ChannelHandlerContext ctx) {
    	return ctx.alloc().heapBuffer().writeBytes(noSignalVO.getNoSignalTs());
	}
	
	private URIDecodeVO uriDecodeHandler(String uri) {
		
		URIDecodeVO uriDecodeVO = null;
		
		try {
			String tempUrl = uri.substring(0, uri.lastIndexOf("/"));
			String seq = tempUrl.substring(tempUrl.lastIndexOf("/") + 1);
			
			uriDecodeVO = new URIDecodeVO();
			uriDecodeVO.setName(uri.substring(uri.lastIndexOf("/") + 1));
			uriDecodeVO.setSeq(seq);
		} catch(IndexOutOfBoundsException ie) {
			log.error("uri decode error : {}", ie.getMessage());
		} catch(Exception e) {
			log.error("uri decode error : {}", e.getMessage());
		}
		
		return uriDecodeVO;
	}
}
