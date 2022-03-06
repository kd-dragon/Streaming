package com.kdy.bean.util.io;

import org.apache.mina.core.buffer.IoBuffer;

import com.kdy.bean.util.io.Interface.ITag;

public class Tag implements ITag {
	/**
	 * Tag type
	 */
	private byte type;

	/**
	 * Tag data type
	 */
	private byte dataType;

	/**
	 * Timestamp
	 */
	private int timestamp;

	/**
	 * Tag body size
	 */
	private int bodySize;

	/**
	 * Tag body as byte buffer
	 */
	private IoBuffer body;

	/**
	 * Previous tag size
	 */
	private int previousTagSize;

	/**
	 * Bit flags
	 */
	private byte bitflags;

	/**
	 * TagImpl Constructor
	 *
	 * @param dataType        Tag data type
	 * @param timestamp       Timestamp
	 * @param bodySize        Tag body size
	 * @param body            Tag body
	 * @param previousTagSize Previous tag size information
	 */
	public Tag(byte dataType, int timestamp, int bodySize, IoBuffer body, int previousTagSize) {
		this.dataType = dataType;
		this.timestamp = timestamp;
		this.bodySize = bodySize;
		this.body = body;
		this.previousTagSize = previousTagSize;
	}

	/**
	 * Constructs a new Tag.
	 */
	public Tag() {

	}

	/**
	 * Getter for bit flags
	 *
	 * @return Value for bit flags
	 */
	public byte getBitflags() {
		return bitflags;
	}

	/**
	 * Setter for bit flags
	 *
	 * @param bitflags Bit flags
	 */
	public void setBitflags(byte bitflags) {
		this.bitflags = bitflags;
	}

	/**
	 * Getter for previous tag size
	 *
	 * @return Value for previous tag size
	 */
	public int getpreviousTagSize() {
		return previousTagSize;
	}

	/**
	 * Setter for previous tag size
	 *
	 * @param previousTagSize Value to set for previous tag size
	 */
	public void setpreviousTagSize(int previousTagSize) {
		this.previousTagSize = previousTagSize;
	}

	/**
	 * {@inheritDoc}
	 */
	public IoBuffer getData() {
		return null;
	}

	/**
	 * Return the body IoBuffer
	 *
	 * @return Tag body
	 */
	public IoBuffer getBody() {
		return body;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setBody(IoBuffer body) {
		this.body = body;
	}

	/**
	 * Return the size of the body
	 *
	 * @return Tag body size
	 */
	public int getBodySize() {
		return bodySize;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setBodySize(int bodySize) {
		this.bodySize = bodySize;
	}

	/**
	 * Get the data type
	 *
	 * @return Tag data type
	 */
	public byte getDataType() {
		return dataType;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDataType(byte dataType) {
		this.dataType = dataType;
	}

	/**
	 * Return the timestamp
	 *
	 * @return Tag timestamp
	 */
	public int getTimestamp() {
		return timestamp;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Return previous tag size
	 *
	 * @return Previous tag size
	 */
	public int getPreviousTagSize() {
		return previousTagSize;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setPreviousTagSize(int size) {
		this.previousTagSize = size;
	}

	/**
	 * Prints out the contents of the tag
	 *
	 * @return Tag contents
	 */
	@Override
	public String toString() {
		String ret = "Data Type\t=" + dataType + "\n";
		ret += "Prev. Tag Size\t=" + previousTagSize + "\n";
		ret += "Body size\t=" + bodySize + "\n";
		ret += "timestamp\t=" + timestamp + "\n";
		ret += "Body Data\t=" + body + "\n";
		ret += "streamingType\t=" + streamingType + "\n";
		return ret;
	}

	/**
	 * Getter for tag type
	 *
	 * @return Tag type
	 */
	public byte getType() {
		return type;
	}

	/**
	 * Setter for tag type
	 *
	 * @param type Tag type
	 */
	public void setType(byte type) {
		this.type = type;
	}

	/**
	 * Setter for tag data. Empty method.
	 */
	public void setData() {
	}
	
	//CJG 통계 추가
	private String streamingType;
	
	public Tag(byte dataType, int timestamp, int bodySize, IoBuffer body, int previousTagSize, String streamingType) {
		this.dataType = dataType;
		this.timestamp = timestamp;
		this.bodySize = bodySize;
		this.body = body;
		this.previousTagSize = previousTagSize;
		this.streamingType = streamingType;
	}
	
	public String getStreamingType() {
		return streamingType;
	}

	public void setStreamingType(String streamingType) {
		this.streamingType = streamingType;
	}
	//CJG END

}
