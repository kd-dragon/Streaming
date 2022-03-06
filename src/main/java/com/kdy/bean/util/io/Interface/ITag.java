package com.kdy.bean.util.io.Interface;

import org.apache.mina.core.buffer.IoBuffer;

public interface ITag extends IoConstants {

	/**
	 * Return the body ByteBuffer
	 *
	 * @return ByteBuffer        Body as byte buffer
	 */
	public IoBuffer getBody();

	/**
	 * Set the body ByteBuffer.
	 *
	 * @param body Body as ByteBuffer
	 */
	public void setBody(IoBuffer body);

	/**
	 * Return the size of the body
	 *
	 * @return int               Body size
	 */
	public int getBodySize();

	/**
	 * Set the size of the body.
	 *
	 * @param size Body size
	 */
	public void setBodySize(int size);

	/**
	 * Get the data type
	 *
	 * @return byte              Data type as byte
	 */
	public byte getDataType();

	/**
	 * Set the data type.
	 *
	 * @param datatype Data type
	 */
	public void setDataType(byte datatype);

	/**
	 * Return the timestamp
	 *
	 * @return int               Timestamp
	 */
	public int getTimestamp();

	/**
	 * Set the timestamp.
	 *
	 * @param timestamp Timestamp
	 */
	public void setTimestamp(int timestamp);

	/**
	 * Returns the data as a ByteBuffer
	 *
	 * @return ByteBuffer        Data as byte buffer
	 */
	public IoBuffer getData();

	/**
	 * Returns previous tag size
	 *
	 * @return int               Previous tag size
	 */
	public int getPreviousTagSize();

	/**
	 * Set the size of the previous tag.
	 *
	 * @param size Previous tag size
	 */
	public void setPreviousTagSize(int size);


}
