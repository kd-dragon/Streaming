package com.kdy.dto;

import java.io.Serializable;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ByteArraySerializer;

import lombok.Data;

@Data
public class StreamPushVO implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String key;
	private byte[] streamData;
	
	@JsonSerialize(using=ByteArraySerializer.class)
	public byte[] getStreamData() {
		return streamData;
	}
}
