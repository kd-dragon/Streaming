package com.kdy.bean.util.io.amf3;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kdy.bean.util.common.hex.HexDump;
import com.kdy.bean.util.io.amf.AMF;
import com.kdy.bean.util.io.amf.Output;
import com.kdy.bean.util.io.object.Serializer;
import com.kdy.bean.util.io.object.IF.OutputIF;


public class OutputAmf3 extends Output implements OutputIF {

	protected static Logger log = LoggerFactory.getLogger(Output.class);

	/**
	 * Set to a value above <tt>0</tt> to disable writing of the AMF3 object tag.
	 */
	private int amf3_mode;

	/**
	 * List of strings already written.
	 */
	private Map<String, Integer> stringReferences;

	/**
	 * Constructor of AMF3 output.
	 *
	 * @param buf instance of IoBuffer
	 *
	 * @see IoBuffer
	 */
	public OutputAmf3(IoBuffer buf) {
		super(buf);
		amf3_mode = 0;
		stringReferences = new HashMap<String, Integer>();
	}

	protected static byte[] encodeString(String string) {
		Object element = getStringCache().get(string);
		byte[] encoded = (element == null ? null : (byte[]) element);
		if (encoded == null) {
			ByteBuffer buf = AMF.CHARSET.encode(string);
			encoded = new byte[buf.limit()];
			buf.get(encoded);
			getStringCache().put(string, encoded);
		}
		return encoded;
	}

	/**
	 * Force using AMF3 everywhere
	 */
	public void enforceAMF3() {
		amf3_mode++;
	}

	/**
	 * Provide access to raw data.
	 *
	 * @return IoBuffer
	 */
	protected IoBuffer getBuffer() {
		return buf;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean supportsDataType(byte type) {
		return true;
	}

	protected void writeAMF3() {
		if (amf3_mode == 0) {
			buf.put(AMF.TYPE_AMF3_OBJECT);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void writeBoolean(Boolean bol) {
		writeAMF3();
		buf.put(bol ? AMF3.TYPE_BOOLEAN_TRUE : AMF3.TYPE_BOOLEAN_FALSE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void writeNull() {
		writeAMF3();
		buf.put(AMF3.TYPE_NULL);
	}

	protected void putInteger(long value) {
		if ((value >= -268435456) && (value <= 268435455)) {
			value &= 536870911;
		}
		if (value < 128) {
			buf.put((byte) value);
		} else if (value < 16384) {
			buf.put((byte) (((value >> 7) & 0x7F) | 0x80));
			buf.put((byte) (value & 0x7F));
		} else if (value < 2097152) {
			buf.put((byte) (((value >> 14) & 0x7F) | 0x80));
			buf.put((byte) (((value >> 7) & 0x7F) | 0x80));
			buf.put((byte) (value & 0x7F));
		} else if (value < 1073741824) {
			buf.put((byte) (((value >> 22) & 0x7F) | 0x80));
			buf.put((byte) (((value >> 15) & 0x7F) | 0x80));
			buf.put((byte) (((value >> 8) & 0x7F) | 0x80));
			buf.put((byte) (value & 0xFF));
		} else {
			log.error("Integer out of range: {}", value);
		}
	}

	protected void putString(String str, byte[] encoded) {
		final int len = encoded.length;
		Integer   pos = stringReferences.get(str);
		if (pos != null) {
			// Reference to existing string
			putInteger(pos << 1);
			return;
		}
		putInteger(len << 1 | 1);
		buf.put(encoded);
		stringReferences.put(str, stringReferences.size());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void putString(String string) {
		if ("".equals(string)) {
			// Empty string;
			putInteger(1);
			return;
		}
		final byte[] encoded = encodeString(string);
		putString(string, encoded);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void writeNumber(Number num) {
		writeAMF3();
		if (num.longValue() < AMF3.MIN_INTEGER_VALUE || num.longValue() > AMF3.MAX_INTEGER_VALUE) {
			// Out of range for integer encoding
			buf.put(AMF3.TYPE_NUMBER);
			buf.putDouble(num.doubleValue());
		} else if (num instanceof Long || num instanceof Integer || num instanceof Short || num instanceof Byte) {
			buf.put(AMF3.TYPE_INTEGER);
			putInteger(num.longValue());
		} else {
			buf.put(AMF3.TYPE_NUMBER);
			buf.putDouble(num.doubleValue());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void writeString(String string) {
		writeAMF3();
		buf.put(AMF3.TYPE_STRING);
		if ("".equals(string)) {
			putInteger(1);
		} else {
			final byte[] encoded = encodeString(string);
			putString(string, encoded);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void writeDate(Date date) {
		writeAMF3();
		buf.put(AMF3.TYPE_DATE);
		if (hasReference(date)) {
			putInteger(getReferenceId(date) << 1);
			return;
		}
		storeReference(date);
		putInteger(1);
		buf.putDouble(date.getTime());
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeArray(Collection<?> array) {
		writeAMF3();
		buf.put(AMF3.TYPE_ARRAY);
		if (hasReference(array)) {
			putInteger(getReferenceId(array) << 1);
			return;
		}
		storeReference(array);
		amf3_mode += 1;
		int count = array.size();
		putInteger(count << 1 | 1);
		putString("");
		for (Object item : array) {
			Serializer.serialize(this, item);
		}
		amf3_mode -= 1;
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeArray(Object[] array) {
		writeAMF3();
		buf.put(AMF3.TYPE_ARRAY);
		if (hasReference(array)) {
			putInteger(getReferenceId(array) << 1);
			return;
		}
		storeReference(array);
		amf3_mode += 1;
		int count = array.length;
		putInteger(count << 1 | 1);
		putString("");
		for (Object item : array) {
			Serializer.serialize(this, item);
		}
		amf3_mode -= 1;
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeArray(Object array) {
		writeAMF3();
		buf.put(AMF3.TYPE_ARRAY);
		if (hasReference(array)) {
			putInteger(getReferenceId(array) << 1);
			return;
		}
		storeReference(array);
		amf3_mode += 1;
		int count = Array.getLength(array);
		putInteger(count << 1 | 1);
		putString("");
		for (int i = 0; i < count; i++) {
			Serializer.serialize(this, Array.get(array, i));
		}
		amf3_mode -= 1;
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeMap(Map<Object, Object> map) {
		writeAMF3();
		buf.put(AMF3.TYPE_ARRAY);
		if (hasReference(map)) {
			putInteger(getReferenceId(map) << 1);
			return;
		}
		storeReference(map);
		// Search number of starting integer keys
		int count = 0;
		for (int i = 0; i < map.size(); i++) {
			try {
				if (!map.containsKey(i)) {
					break;
				}
			} catch (ClassCastException err) {
				// Map has non-number keys.
				break;
			}
			count++;
		}
		amf3_mode += 1;
		if (count == map.size()) {
			// All integer keys starting from zero: serialize as regular array
			putInteger(count << 1 | 1);
			putString("");
			for (int i = 0; i < count; i++) {
				Serializer.serialize(this, map.get(i));
			}
			amf3_mode -= 1;
			return;
		}
		putInteger(count << 1 | 1);
		// Serialize key-value pairs first
		for (Map.Entry<Object, Object> entry : map.entrySet()) {
			Object key = entry.getKey();
			if ((key instanceof Number) && !(key instanceof Float) && !(key instanceof Double) && ((Number) key).longValue() >= 0 && ((Number) key).longValue() < count) {
				// Entry will be serialized later
				continue;
			}
			putString(key.toString());
			Serializer.serialize(this, entry.getValue());
		}
		putString("");
		// Now serialize integer keys starting from zero
		for (int i = 0; i < count; i++) {
			Serializer.serialize(this, map.get(i));
		}
		amf3_mode -= 1;
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeMap(Collection<?> array) {
		writeAMF3();
		buf.put(AMF3.TYPE_ARRAY);
		if (hasReference(array)) {
			putInteger(getReferenceId(array) << 1);
			return;
		}
		storeReference(array);
		// TODO: we could optimize this by storing the first integer
		//       keys after the key-value pairs
		amf3_mode += 1;
		putInteger(1);
		int idx = 0;
		for (Object item : array) {
			if (item != null) {
				putString(String.valueOf(idx));
				Serializer.serialize(this, item);
			}
			idx++;
		}
		amf3_mode -= 1;
		putString("");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void writeObject(Map<Object, Object> map) {
		writeAMF3();
		buf.put(AMF3.TYPE_OBJECT);
		if (hasReference(map)) {
			putInteger(getReferenceId(map) << 1);
			return;
		}
		storeReference(map);
		// We have an inline class that is not a reference.
		// We store the properties using key/value pairs
		int type = AMF3.TYPE_OBJECT_VALUE << 2 | 1 << 1 | 1;
		putInteger(type);
		// No classname
		putString("");
		// Store key/value pairs
		amf3_mode += 1;
		for (Map.Entry<Object, Object> entry : map.entrySet()) {
			putString(entry.getKey().toString());
			Serializer.serialize(this, entry.getValue());
		}
		amf3_mode -= 1;
		// End of object marker
		putString("");
	}
	
	/**
	 * Write a Vector<int>.
	 *
	 * @param vector
	 */
	@Override
	public void writeVectorInt(Vector<Integer> vector) {
		log.debug("writeVectorInt: {}", vector);
		writeAMF3();
		buf.put(AMF3.TYPE_VECTOR_INT);
		if (hasReference(vector)) {
			putInteger(getReferenceId(vector) << 1);
			return;
		}
		storeReference(vector);
		putInteger(vector.size() << 1 | 1);
		buf.put((byte) 0x00);
		for (Integer v : vector) {
			buf.putInt(v);
		}
		// debug
		if (log.isDebugEnabled()) {
			int pos = buf.position();
			buf.position(0);
			StringBuilder sb = new StringBuilder();
			HexDump.dumpHex(sb, buf.array());
			log.debug("\n{}", sb);
			buf.position(pos);
		}
	}

	/**
	 * Write a Vector<uint>.
	 *
	 * @param vector
	 */

	/**
	 * Write a Vector<Number>.
	 *
	 * @param vector
	 */
	@Override
	public void writeVectorNumber(Vector<Double> vector) {
		log.debug("writeVectorNumber: {}", vector);
		buf.put(AMF3.TYPE_VECTOR_NUMBER);
		if (hasReference(vector)) {
			putInteger(getReferenceId(vector) << 1);
			return;
		}
		storeReference(vector);
		putInteger(vector.size() << 1 | 1);
		putInteger(0);
		buf.put((byte) 0x00);
		for (Double v : vector) {
			buf.putDouble(v);
		}
	}

	/**
	 * Write a Vector<Object>.
	 *
	 * @param vector
	 */
	@Override
	public void writeVectorObject(Vector<Object> vector) {
		log.debug("writeVectorObject: {}", vector);
		buf.put(AMF3.TYPE_VECTOR_OBJECT);
		if (hasReference(vector)) {
			putInteger(getReferenceId(vector) << 1);
			return;
		}
		storeReference(vector);
		putInteger(vector.size() << 1 | 1);
		putInteger(0);
		buf.put((byte) 0x01);
		for (Object v : vector) {
			Serializer.serialize(this, v);
		}
	}

}
