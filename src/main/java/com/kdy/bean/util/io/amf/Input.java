package com.kdy.bean.util.io.amf;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.util.byteaccess.ByteArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.kdy.bean.util.common.ArrayUtils;
import com.kdy.bean.util.common.DataTypes;
import com.kdy.bean.util.io.object.BaseInput;
import com.kdy.bean.util.io.object.Deserializer;
import com.kdy.bean.util.io.object.IF.InputIF;

public class Input extends BaseInput implements InputIF {

	protected static Logger log = LoggerFactory.getLogger(Input.class);

	protected IoBuffer buf;

	protected byte currentDataType;

	/**
	 * Creates Input object from byte buffer
	 *
	 * @param buf Byte buffer
	 */
	public Input(IoBuffer buf) {
		super();
		this.buf = buf;
	}

	/**
	 * Returns a string based on the buffer
	 *
	 * @param buf Byte buffer with data
	 * @return String   Decoded string
	 */
	public static String getString(IoBuffer buf) {
		int len = buf.getShort() & 0xff; //buf.getUnsignedShort(); XXX is appears to be broken in mina at 2.0.4
		log.debug("Length: {}", len);
		int limit = buf.limit();
		log.debug("Limit: {}", limit);
		String string = bufferToString(buf.buf(), len);
		buf.limit(limit); // Reset the limit
		return string;
	}

	/**
	 * Returns a string based on the buffer
	 *
	 * @param buf Byte buffer with data
	 * @return String   Decoded string
	 */
	public static String getString(java.nio.ByteBuffer buf) {
		int len = buf.getShort() & 0xff;
		log.debug("Length: {}", len);
		int limit = buf.limit();
		log.debug("Limit: {}", limit);
		String string = bufferToString(buf, len);
		buf.limit(limit); // Reset the limit
		return string;
	}

	// Basic

	/**
	 * Converts the bytes into a string.
	 *
	 * @param strBuf
	 * @return contents of the ByteBuffer as a String
	 */
	private final static String bufferToString(final java.nio.ByteBuffer strBuf, int len) {
		//Trac #601 - part of the problem seems to be a null byte buffer
		String string = null;
		if (strBuf != null) {
			int pos = strBuf.position();
			log.debug("String buf - position: {} limit: {}", pos, (pos + len));
			strBuf.limit(pos + len);
			string = AMF.CHARSET.decode(strBuf).toString();
			log.debug("String: {}", string);
		} else {
			log.warn("ByteBuffer was null attempting to read String");
		}
		return string;
	}

	/**
	 * Reads the data type.
	 *
	 * @return byte         Data type
	 */
	public byte readDataType() {
		if (buf != null) {
			// XXX Paul: prevent an NPE here by returning the current data type
			// when there is a null buffer
			currentDataType = buf.get();
		} else {
			log.error("Why is buf null?");
		}
		return readDataType(currentDataType);
	}

	/**
	 * Reads the data type.
	 *
	 * @param dataType Data type as byte
	 * @return One of AMF class constants with type
	 * @see com.kdy.bean.util.io.amf.red5.io.amf.AMF
	 */
	protected byte readDataType(byte dataType) {
		byte coreType;

		switch (currentDataType) {

			case AMF.TYPE_NULL:
			case AMF.TYPE_UNDEFINED:
				coreType = DataTypes.CORE_NULL;
				break;

			case AMF.TYPE_NUMBER:
				coreType = DataTypes.CORE_NUMBER;
				break;

			case AMF.TYPE_BOOLEAN:
				coreType = DataTypes.CORE_BOOLEAN;
				break;

			case AMF.TYPE_STRING:
			case AMF.TYPE_LONG_STRING:
				coreType = DataTypes.CORE_STRING;
				break;

			case AMF.TYPE_CLASS_OBJECT:
			case AMF.TYPE_OBJECT:
				coreType = DataTypes.CORE_OBJECT;
				break;

			case AMF.TYPE_MIXED_ARRAY:
				coreType = DataTypes.CORE_MAP;
				break;

			case AMF.TYPE_ARRAY:
				coreType = DataTypes.CORE_ARRAY;
				break;

			case AMF.TYPE_DATE:
				coreType = DataTypes.CORE_DATE;
				break;

			case AMF.TYPE_XML:
				coreType = DataTypes.CORE_XML;
				break;

			case AMF.TYPE_REFERENCE:
				coreType = DataTypes.OPT_REFERENCE;
				break;

			case AMF.TYPE_UNSUPPORTED:
			case AMF.TYPE_MOVIECLIP:
			case AMF.TYPE_RECORDSET:
				// These types are not handled by core datatypes
				// So add the amf mask to them, this way the deserializer
				// will call back to readCustom, we can then handle or return null
				coreType = (byte) (currentDataType + DataTypes.CUSTOM_AMF_MASK);
				break;

			case AMF.TYPE_END_OF_OBJECT:
			default:
				// End of object, and anything else lets just skip
				coreType = DataTypes.CORE_SKIP;
				break;
		}

		return coreType;
	}

	/**
	 * Reads a null.
	 *
	 * @return Object
	 */
	public Object readNull(Type target) {
		return null;
	}

	/**
	 * Reads a boolean.
	 *
	 * @return boolean
	 */
	public Boolean readBoolean(Type target) {
		// TODO: check values
		return (buf.get() == AMF.VALUE_TRUE) ? Boolean.TRUE : Boolean.FALSE;
	}

	/**
	 * Reads a Number. In ActionScript 1 and 2 Number type represents all numbers,
	 * both floats and integers.
	 *
	 * @return Number
	 */
	public Number readNumber(Type target) {
		double num = buf.getDouble();
		if (num == Math.round(num)) {
			if (num < Integer.MAX_VALUE) {
				return (int) num;
			} else {
				return Math.round(num);
			}
		} else {
			return num;
		}
	}

	/**
	 * Reads string from buffer
	 *
	 * @return String
	 */
	public String getString() {
		return getString(buf);
	}

	/**
	 * Reads a string
	 *
	 * @return String
	 */
	public String readString(Type target) {
		int len = 0;
		switch (currentDataType) {
			case AMF.TYPE_LONG_STRING:
				len = buf.getInt();
				break;
			case AMF.TYPE_STRING:
				len = buf.getShort() & 0xff; //buf.getUnsignedShort();
				break;
			default:
				log.debug("Unknown AMF type: {}", currentDataType);
		}
		int limit = buf.limit();
		log.debug("Limit: {}", limit);
		String string = bufferToString(buf.buf(), len);
		buf.limit(limit); // Reset the limit
		return string;
	}

	/**
	 * Returns a date
	 *
	 * @return Date      Decoded string object
	 */
	public Date readDate(Type target) {
		/*
		 * Date: 0x0B T7 T6 .. T0 Z1 Z2 T7 to T0 form a 64 bit Big Endian number
		 * that specifies the number of nanoseconds that have passed since
		 * 1/1/1970 0:00 to the specified time. This format is UTC 1970. Z1 an
		 * Z0 for a 16 bit Big Endian number indicating the indicated time's
		 * timezone in minutes.
		 */
		long ms = (long) buf.getDouble();
		// The timezone can be ignored as the date always is encoded in UTC
		@SuppressWarnings("unused") short timeZoneMins = buf.getShort();
		Date date = new Date(ms);
		storeReference(date);
		return date;
	}

	// Array

	public Object readArray(Type target) {
		log.debug("readArray - target: {}", target);
		Object result = null;
		int count = buf.getInt();
		log.debug("Count: {}", count);
		List<Object> resultCollection = new ArrayList<Object>(count);
		storeReference(result);
		for (int i = 0; i < count; i++) {
			resultCollection.add(Deserializer.deserialize(this, Object.class));
		}
		// To maintain conformance to the Input API, we should convert the output
		// into an Array if the Type asks us to.
		Class<?> collection = Collection.class;
		if (target instanceof Class<?>) {
			collection = (Class<?>) target;
		}
		if (collection.isArray()) {
			result = ArrayUtils.toArray(collection.getComponentType(), resultCollection);
		} else {
			result = resultCollection;
		}
		return result;
	}

	// Map

	/**
	 * Read key - value pairs. This is required for the RecordSet
	 * deserializer.
	 */
	public Map<String, Object> readKeyValues() {
		Map<String, Object> result = new HashMap<String, Object>();
		readKeyValues(result);
		return result;
	}

	/**
	 * Read key - value pairs into Map object
	 *
	 * @param result       Map to put resulting pair to
	 * @param deserializer Deserializer used
	 */
	protected void readKeyValues(Map<String, Object> result) {
		while (hasMoreProperties()) {
			String name = readPropertyName();
			log.debug("property: {}", name);
			Object property = Deserializer.deserialize(this, Object.class);
			log.debug("val: {}", property);
			result.put(name, property);
			if (hasMoreProperties()) {
				skipPropertySeparator();
			}
		}
		skipEndObject();
	}

	public Object readMap(Type target) {
		// The maximum number used in this mixed array.
		int maxNumber = buf.getInt();
		log.debug("Read start mixed array: {}", maxNumber);
		Object result;
		final Map<Object, Object> mixedResult = new LinkedHashMap<Object, Object>(maxNumber);
		// we must store the reference before we deserialize any items in it to ensure
		// that reference IDs are correct
		int reference = storeReference(mixedResult);
		Boolean normalArray = true;
		while (hasMoreProperties()) {
			String key = getString(buf);
			log.debug("key: {}", key);
			try {
				Integer.parseInt(key);
			} catch (NumberFormatException e) {
				log.debug("key {} is causing non normal array", key);
				normalArray = false;
			}
			Object item = Deserializer.deserialize(this, Object.class);
//			log.debug("item: {}", item);
			mixedResult.put(key, item);
		}

		if (mixedResult.size() <= maxNumber + 1 && normalArray) {
			// MixedArray actually is a regular array
			log.debug("mixed array is a regular array");
			final List<Object> listResult = new ArrayList<Object>(maxNumber);
			for (int i = 0; i < maxNumber; i++) {
				listResult.add(i, mixedResult.get(String.valueOf(i)));
			}
			result = listResult;
		} else {
			// Convert initial indexes
			mixedResult.remove("length");
			for (int i = 0; i < maxNumber; i++) {
				final Object value = mixedResult.remove(String.valueOf(i));
				mixedResult.put(i, value);
			}
			result = mixedResult;
		}
		// Replace the original reference with the final result
		storeReference(reference, result);
		skipEndObject();
		return result;
	}

	/**
	 * Returns a boolean stating whether there are more properties
	 *
	 * @return boolean       <code>true</code> if there are more properties to read, <code>false</code> otherwise
	 */
	public boolean hasMoreProperties() {
		byte pad = 0x00;
		byte pad0 = buf.get();
		byte pad1 = buf.get();
		byte type = buf.get();

		boolean isEndOfObject = (pad0 == pad && pad1 == pad && type == AMF.TYPE_END_OF_OBJECT);
		log.debug("End of object: ? {}", isEndOfObject);
		buf.position(buf.position() - 3);
		return !isEndOfObject;
	}

	/**
	 * Reads property name
	 *
	 * @return String        Object property name
	 */
	public String readPropertyName() {
		return getString(buf);
	}

	/**
	 * Skips property seperator
	 */
	public void skipPropertySeparator() {
		// SKIP
	}

	/**
	 * Skips end object
	 */
	public void skipEndObject() {
		// skip two marker bytes
		// then end of object byte
		buf.skip(3);
	}

	// Others



	/**
	 * Reads Custom
	 *
	 * @return Object       Custom type object
	 */
	public Object readCustom(Type target) {
		// Return null for now
		return null;
	}

	/**
	 * Read ByteArray object. This is not supported by the AMF0 deserializer.
	 *
	 * @return ByteArray object
	 */
	public ByteArray readByteArray(Type target) {
		throw new RuntimeException("ByteArray objects not supported with AMF0");
	}

	/**
	 * Read Vector<int> object. This is not supported by the AMF0 deserializer.
	 *
	 * @return Vector<Integer> object
	 */
	public Vector<Integer> readVectorInt() {
		throw new RuntimeException("Vector objects not supported with AMF0");
	}

	/**
	 * Read Vector<Long> object. This is not supported by the AMF0 deserializer.
	 *
	 * @return Vector<Long> object
	 */
	public Vector<Long> readVectorUInt() {
		throw new RuntimeException("Vector objects not supported with AMF0");
	}

	/**
	 * Read Vector<Number> object. This is not supported by the AMF0 deserializer.
	 *
	 * @return Vector<Double> object
	 */
	public Vector<Double> readVectorNumber() {
		throw new RuntimeException("Vector objects not supported with AMF0");
	}

	/**
	 * Read Vector<Object> object. This is not supported by the AMF0 deserializer.
	 *
	 * @return Vector<Object> object
	 */
	public Vector<Object> readVectorObject() {
		throw new RuntimeException("Vector objects not supported with AMF0");
	}

	/**
	 * Reads Reference
	 *
	 * @return Object       Read reference to object
	 */
	public Object readReference(Type target) {
		//return getReference(buf.getUnsignedShort());
		return getReference(buf.getShort() & 0xff);
	}

	/**
	 * Resets map
	 */
	public void reset() {
		this.clearReferences();
	}

	@Override
	public Object readObject(Type target) {
		
		return readSimpleObject();
	}
	
	protected Map<String, Object> readSimpleObject() {
		log.debug("read map");
		Map<String, Object> result = new ConcurrentHashMap<String, Object>();
		readKeyValues(result);
		storeReference(result);
		return result;
	}
	


	@Override
	public Document readXML(Type target) {
		// TODO Auto-generated method stub
		return null;
	}
}
