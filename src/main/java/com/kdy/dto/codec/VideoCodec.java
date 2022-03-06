package com.kdy.dto.codec;

public enum VideoCodec {
	
	JPEG((byte) 0x01), H263((byte) 0x02), SCREEN_VIDEO((byte) 0x03), VP6((byte) 0x04), 
	VP6a((byte) 0x05), SCREEN_VIDEO2((byte) 0x06), AVC((byte) 0x07), VP8((byte) 0x08), 
	VP9((byte) 0x09), AV1((byte) 0x0a), MPEG1((byte) 0x0b), HEVC((byte) 0x0c);

	private byte id;

	private VideoCodec(byte id) {
		this.id = id;
	}

	/**
	 * Returns back a numeric id for this codec,
	 * that happens to correspond to the numeric
	 * identifier that FLV will use for this codec.
	 *
	 * @return the codec id
	 */
	public byte getId() {
		return id;
	}

}
