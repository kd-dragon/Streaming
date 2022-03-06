package com.kdy.bean.util.io.amf;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.util.byteaccess.ByteArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.beans.BeanMap;
import org.w3c.dom.Document;

import com.kdy.bean.util.cache.CacheManager;
import com.kdy.bean.util.cache.ObjectCache;
import com.kdy.bean.util.io.object.BaseOutput;
import com.kdy.bean.util.io.object.Serializer;
import com.kdy.bean.util.io.object.IF.OutputIF;

public class Output extends BaseOutput implements OutputIF {

	protected static Logger log = LoggerFactory.getLogger(Output.class);

	/**
	 * Cache encoded strings... the TK way...
	 */
	private static ObjectCache stringCache;

	private static ObjectCache serializeCache;

	private static ObjectCache fieldCache;

	private static ObjectCache getterCache;

	/**
	 * Output buffer
	 */
	protected IoBuffer buf;

	/**
	 * Creates output with given byte buffer
	 *
	 * @param buf Bute buffer
	 */
	public Output(IoBuffer buf) {
		super();
		this.buf = buf;
	}

	/**
	 * Encode string.
	 *
	 * @param string
	 *
	 * @return encoded string
	 */
	protected static byte[] encodeString(String string) {
		Object element = getStringCache().get(string);
		byte[] encoded = (element == null ? null : (byte[]) element);
		if (encoded == null) {
			ByteBuffer buf = AMF.CHARSET.encode(string);
			encoded = new byte[buf.limit()];
			buf.get(encoded);
			getStringCache().put(string, (Object) encoded);
		}
		return encoded;
	}

	/**
	 * Write out string
	 *
	 * @param buf    Byte buffer to write to
	 * @param string String to write
	 */
	public static void putString(IoBuffer buf, String string) {
		final byte[] encoded = encodeString(string);
		buf.putShort((short) encoded.length);
		buf.put(encoded);
	}

	protected static ObjectCache getStringCache() {
		if (stringCache == null) {
			stringCache = CacheManager.getInstance().getCache("com.kdy.bean.util.data.Output.stringCache");
		}

		return stringCache;
	}

	protected static ObjectCache getSerializeCache() {
		if (serializeCache == null) {
			serializeCache = CacheManager.getInstance().getCache("com.kdy.bean.util.data.Output.serializeCache");
		}

		return serializeCache;
	}

	protected static ObjectCache getFieldCache() {
		if (fieldCache == null) {
			fieldCache = CacheManager.getInstance().getCache("com.kdy.bean.util.data.Output.fieldCache");
		}

		return fieldCache;
	}

	protected static ObjectCache getGetterCache() {
		if (getterCache == null) {
			getterCache = CacheManager.getInstance().getCache("com.kdy.bean.util.data.Output.getterCache");
		}

		return getterCache;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isCustom(Object custom) {
		return false;
	}

	protected boolean checkWriteReference(Object obj) {
		if (hasReference(obj)) {
			writeReference(obj);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeArray(Collection<?> array) {
		if (checkWriteReference(array)) {
			return;
		}
		storeReference(array);
		buf.put(AMF.TYPE_ARRAY);
		buf.putInt(array.size());
		for (Object item : array) {
			Serializer.serialize(this, item);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeArray(Object[] array) {
		log.debug("writeArray - array: {}", array);
		if (array != null) {
			if (checkWriteReference(array)) {
				return;
			}
			storeReference(array);
			buf.put(AMF.TYPE_ARRAY);
			buf.putInt(array.length);
			for (Object item : array) {
				Serializer.serialize(this, item);
			}
		} else {
			writeNull();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeArray(Object array) {
		if (array != null) {
			if (checkWriteReference(array)) {
				return;
			}
			storeReference(array);
			buf.put(AMF.TYPE_ARRAY);
			buf.putInt(Array.getLength(array));
			for (int i = 0; i < Array.getLength(array); i++) {
				Serializer.serialize(this, Array.get(array, i));
			}
		} else {
			writeNull();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeMap(Map<Object, Object> map) {
		if (checkWriteReference(map)) {
			log.info("Output checkWriteReference(map) is False");
			return;
		}
		storeReference(map);
		buf.put(AMF.TYPE_MIXED_ARRAY);
		int maxInt = -1;
		for (int i = 0; i < map.size(); i++) {
			try {
				if (!map.containsKey(i)) {
					break;
				}
			} catch (ClassCastException err) {
				// Map has non-number keys.
				break;
			}

			maxInt = i;
		}
		buf.putInt(maxInt + 1);
		// TODO: Need to support an incoming key named length
		for (Map.Entry<Object, Object> entry : map.entrySet()) {
			final String key = entry.getKey().toString();
			if ("length".equals(key)) {
				continue;
			}
			putString(key);
			Serializer.serialize(this, entry.getValue());
		}
		if (maxInt >= 0) {
			putString("length");
			Serializer.serialize(this, maxInt + 1);
		}
		buf.put((byte) 0x00);
		buf.put((byte) 0x00);
		buf.put(AMF.TYPE_END_OF_OBJECT);
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeMap(Collection<?> array) {
		if (checkWriteReference(array)) {
			return;
		}
		storeReference(array);
		buf.put(AMF.TYPE_MIXED_ARRAY);
		buf.putInt(array.size() + 1);
		int idx = 0;
		for (Object item : array) {
			if (item != null) {
				putString(String.valueOf(idx++));
				Serializer.serialize(this, item);
			} else {
				idx++;
			}
		}
		putString("length");
		Serializer.serialize(this, array.size() + 1);

		buf.put((byte) 0x00);
		buf.put((byte) 0x00);
		buf.put(AMF.TYPE_END_OF_OBJECT);
	}


	/**
	 * {@inheritDoc}
	 */
	public boolean supportsDataType(byte type) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeBoolean(Boolean bol) {
		buf.put(AMF.TYPE_BOOLEAN);
		buf.put(bol ? AMF.VALUE_TRUE : AMF.VALUE_FALSE);
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeCustom(Object custom) {

	}

	/**
	 * {@inheritDoc}
	 */
	public void writeDate(Date date) {
		buf.put(AMF.TYPE_DATE);
		buf.putDouble(date.getTime());
		buf.putShort((short) (TimeZone.getDefault().getRawOffset() / 60 / 1000));
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeNull() {
		// System.err.println("Write null");
		buf.put(AMF.TYPE_NULL);
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeNumber(Number num) {
		buf.put(AMF.TYPE_NUMBER);
		buf.putDouble(num.doubleValue());
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeReference(Object obj) {
		log.debug("Write reference");
		buf.put(AMF.TYPE_REFERENCE);
		buf.putShort(getReferenceId(obj));
	}


	@SuppressWarnings("unchecked")
	protected Field getField(Class<?> objectClass, String keyName) {
		//again, to prevent null pointers, check if the element exists first.
		Object             element  = getFieldCache().get(objectClass.toString());
		Map<String, Field> fieldMap = (element == null ? null : (Map<String, Field>) element);
		if (fieldMap == null) {
			fieldMap = new HashMap<String, Field>();
			getFieldCache().put(objectClass.toString(), fieldMap);
		}

		Field field = null;

		if (fieldMap.containsKey(keyName)) {
			field = fieldMap.get(keyName);
		} else {
			for (Class<?> clazz = objectClass; !clazz.equals(Object.class); clazz = clazz.getSuperclass()) {
				Field[] fields = clazz.getDeclaredFields();
				if (fields.length > 0) {
					for (Field fld : fields) {
						if (fld.getName().equals(keyName)) {
							field = fld;
							break;
						}
					}
				}
			}

			fieldMap.put(keyName, field);
		}

		return field;
	}



	/**
	 * {@inheritDoc}
	 */
	public void writeObject(Map<Object, Object> map) {
		if (checkWriteReference(map)) {
			return;
		}
		storeReference(map);
		buf.put(AMF.TYPE_OBJECT);
		boolean isBeanMap = (map instanceof BeanMap);
		for (Map.Entry<Object, Object> entry : map.entrySet()) {
			if (isBeanMap && "class".equals(entry.getKey())) {
				continue;
			}
			putString(entry.getKey().toString());
			Serializer.serialize(this, entry.getValue());
		}
		buf.put((byte) 0x00);
		buf.put((byte) 0x00);
		buf.put(AMF.TYPE_END_OF_OBJECT);
	}

	
	/**
	 * {@inheritDoc}
	 */
	public void writeString(String string) {
		final byte[] encoded = encodeString(string);
		final int    len     = encoded.length;
		if (len < AMF.LONG_STRING_LENGTH) {
			buf.put(AMF.TYPE_STRING);
			buf.putShort((short) len);
		} else {
			buf.put(AMF.TYPE_LONG_STRING);
			buf.putInt(len);
		}
		buf.put(encoded);
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeByteArray(ByteArray array) {
		throw new RuntimeException("ByteArray objects not supported with AMF0");
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeVectorInt(Vector<Integer> vector) {
		throw new RuntimeException("Vector objects not supported with AMF0");
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeVectorUInt(Vector<Long> vector) {
		throw new RuntimeException("Vector objects not supported with AMF0");
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeVectorNumber(Vector<Double> vector) {
		throw new RuntimeException("Vector objects not supported with AMF0");
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeVectorObject(Vector<Object> vector) {
		throw new RuntimeException("Vector objects not supported with AMF0");
	}

	/**
	 * {@inheritDoc}
	 */
	public void putString(String string) {
		putString(buf, string);
	}


	/**
	 * Convenience method to allow XML text to be used, instead
	 * of requiring an XML Document.
	 *
	 * @param xml xml to write
	 */
	public void writeXML(String xml) {
		buf.put(AMF.TYPE_XML);
		putString(xml);
	}

	/**
	 * Return buffer of this Output object
	 *
	 * @return Byte buffer of this Output object
	 */
	public IoBuffer buf() {
		return this.buf;
	}

	public void reset() {
		clearReferences();
	}

	@Override
	public void writeObject(Object object) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeXML(Document xml) {
		// TODO Auto-generated method stub
		
	}

}
