package com.kdy.bean.util.io.Interface;

import java.nio.ByteOrder;

public interface IDataInput {

	/**
	 * Return the byteorder used when loading values.
	 *
	 * @return the byteorder
	 */
	public ByteOrder getEndian();

	/**
	 * Set the byteorder to use when loading values.
	 *
	 * @param endian the byteorder to use
	 */
	public void setEndian(ByteOrder endian);

	/**
	 * Read boolean value.
	 *
	 * @return the value
	 */
	public boolean readBoolean();

	/**
	 * Read signed single byte value.
	 *
	 * @return the value
	 */
	public byte readByte();

	/**
	 * Read list of bytes.
	 *
	 * @param bytes destination for read bytes
	 */
	public void readBytes(byte[] bytes);

	/**
	 * Read list of bytes to given offset.
	 *
	 * @param bytes  destination for read bytes
	 * @param offset offset in destination to write to
	 */
	public void readBytes(byte[] bytes, int offset);

	/**
	 * Read given number of bytes to given offset.
	 *
	 * @param bytes  destination for read bytes
	 * @param offset offset in destination to write to
	 * @param length number of bytes to read
	 */
	public void readBytes(byte[] bytes, int offset, int length);

	/**
	 * Read double-precision floating point value.
	 *
	 * @return the value
	 */
	public double readDouble();

	/**
	 * Read single-precision floating point value.
	 *
	 * @return the value
	 */
	public float readFloat();

	/**
	 * Read signed integer value.
	 *
	 * @return the value
	 */
	public int readInt();

	/**
	 * Read multibyte string.
	 *
	 * @param length  length of string to read
	 * @param charSet character set of string to read
	 *
	 * @return the string
	 */
	public String readMultiByte(int length, String charSet);

	/**
	 * Read arbitrary object.
	 *
	 * @return the object
	 */
	public Object readObject();

	/**
	 * Read signed short value.
	 *
	 * @return the value
	 */
	public short readShort();

	/**
	 * Read unsigned single byte value.
	 *
	 * @return the value
	 */
	public int readUnsignedByte();

	/**
	 * Read unsigned integer value.
	 *
	 * @return the value
	 */
	public long readUnsignedInt();

	/**
	 * Read unsigned short value.
	 *
	 * @return the value
	 */
	public int readUnsignedShort();

	/**
	 * Read UTF-8 encoded string.
	 *
	 * @return the string
	 */
	public String readUTF();

	/**
	 * Read UTF-8 encoded string with given length.
	 *
	 * @param length the length of the string
	 *
	 * @return the string
	 */
	public String readUTFBytes(int length);
}
