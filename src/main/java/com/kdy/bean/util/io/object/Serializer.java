package com.kdy.bean.util.io.object;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.apache.mina.util.byteaccess.ByteArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.kdy.bean.util.io.Interface.IExternalizable;
import com.kdy.bean.util.io.amf.Output;
import com.kdy.bean.util.io.amf3.OutputAmf3;

public class Serializer {

	protected static Logger log = LoggerFactory.getLogger(Serializer.class);

	private Serializer() {
	}

	/**
	 * Serializes output to a core data type object
	 *
	 * @param out Output writer
	 * @param any Object to serialize
	 */
	public static void serialize(Output out, Object any) {
		Serializer.serialize(out, null, null, null, any);
	}

	/**
	 * Serializes output to a core data type object
	 *
	 * @param out    Output writer
	 * @param field  The field to serialize
	 * @param getter The getter method if not a field
	 * @param object Parent object
	 * @param value  Object to serialize
	 */
	@SuppressWarnings("unchecked")
	public static void serialize(Output out, Field field, Method getter, Object object, Object value) {
		log.trace("serialize");
		if (value instanceof IExternalizable) {
			// make sure all IExternalizable objects are serialized as objects
			out.writeObject(value);
		} else if (value instanceof ByteArray) {
			// write ByteArray objects directly
			out.writeByteArray((ByteArray) value);
		} else if (value instanceof Vector) {
			log.trace("Serialize Vector");
			// scan the vector to determine the generic type
			Vector<?> vector = (Vector<?>) value;
			int       ints   = 0;
			int       longs  = 0;
			int       dubs   = 0;
			int       nans   = 0;
			for (Object o : vector) {
				if (o instanceof Integer) {
					ints++;
				} else if (o instanceof Long) {
					longs++;
				} else if (o instanceof Number || o instanceof Double) {
					dubs++;
				} else {
					nans++;
				}
			}
			// look at the type counts
			if (nans > 0) {
				// if we have non-number types, use object
				((OutputAmf3) out).enforceAMF3();
				out.writeVectorObject((Vector<Object>) value);
			} else if (dubs == 0 && longs == 0) {
				// no doubles or longs
				out.writeVectorInt((Vector<Integer>) value);
			} else if (dubs == 0 && ints == 0) {
				// no doubles or ints
				out.writeVectorUInt((Vector<Long>) value);
			} else {
				// handle any other types of numbers
				((OutputAmf3) out).enforceAMF3();
				out.writeVectorNumber((Vector<Double>) value);
			}
		} else {
			if (writeBasic(out, value)) {
				log.trace("Wrote as basic");
			}
		}
	}

	/**
	 * Writes a primitive out as an object
	 *
	 * @param out   Output writer
	 * @param basic Primitive
	 *
	 * @return boolean true if object was successfully serialized, false
	 * otherwise
	 */
	@SuppressWarnings("rawtypes")
	protected static boolean writeBasic(Output out, Object basic) {
		if (basic == null) {
			out.writeNull();
		} else if (basic instanceof Boolean) {
			out.writeBoolean((Boolean) basic);
		} else if (basic instanceof Number) {
			out.writeNumber((Number) basic);
		} else if (basic instanceof String) {
			out.writeString((String) basic);
		} else if (basic instanceof Enum) {
			out.writeString(((Enum) basic).name());
		} else if (basic instanceof Date) {
			out.writeDate((Date) basic);
		} else {
			return false;
		}
		return true;
	}


	/**
	 * Writes Lists out as a data type
	 *
	 * @param out      Output write
	 * @param listType List type
	 *
	 * @return boolean true if object was successfully serialized, false
	 * otherwise
	 */
	protected static boolean writeListType(Output out, Object listType) {
		log.trace("writeListType");
		if (listType instanceof List<?>) {
			writeList(out, (List<?>) listType);
		} else {
			return false;
		}
		return true;
	}

	/**
	 * Writes a List out as an Object
	 *
	 * @param out  Output writer
	 * @param list List to write as Object
	 */
	protected static void writeList(Output out, List<?> list) {
		if (!list.isEmpty()) {
			int size = list.size();
			// if its a small list, write it as an array
			if (size < 100) {
				out.writeArray(list);
				return;
			}
			// else we should check for lots of null values,
			// if there are over 80% then its probably best to do it as a map
			int nullCount = 0;
			for (int i = 0; i < size; i++) {
				if (list.get(i) == null) {
					nullCount++;
				}
			}
			if (nullCount > (size * 0.8)) {
				out.writeMap(list);
			} else {
				out.writeArray(list);
			}
		} else {
			out.writeArray(new Object[]{});
		}
	}

	/**
	 * Writes array (or collection) out as output Arrays, Collections, etc
	 *
	 * @param out     Output object
	 * @param arrType Array or collection type
	 *
	 * @return <code>true</code> if the object has been written, otherwise
	 * <code>false</code>
	 */
	@SuppressWarnings("all")
	protected static boolean writeArrayType(Output out, Object arrType) {
		log.trace("writeArrayType");
		if (arrType instanceof Collection) {
			out.writeArray((Collection<Object>) arrType);
		} else if (arrType instanceof Iterator) {
			writeIterator(out, (Iterator<Object>) arrType);
		} else if (arrType.getClass().isArray() && arrType.getClass().getComponentType().isPrimitive()) {
			out.writeArray(arrType);
		} else if (arrType instanceof Object[]) {
			out.writeArray((Object[]) arrType);
		} else {
			return false;
		}
		return true;
	}

	/**
	 * Writes an iterator out to the output
	 *
	 * @param out Output writer
	 * @param it  Iterator to write
	 */
	protected static void writeIterator(Output out, Iterator<Object> it) {
		log.trace("writeIterator");
		// Create LinkedList of collection we iterate thru and write it out
		// later
		LinkedList<Object> list = new LinkedList<Object>();
		while (it.hasNext()) {
			list.addLast(it.next());
		}
		// Write out collection
		out.writeArray(list);
	}

	/**
	 * Writes an xml type out to the output
	 *
	 * @param out Output writer
	 * @param xml XML
	 *
	 * @return boolean <code>true</code> if object was successfully written,
	 * <code>false</code> otherwise
	 */
	protected static boolean writeXMLType(Output out, Object xml) {
		log.trace("writeXMLType");
		// If it's a Document write it as Document
		if (xml instanceof Document) {
			writeDocument(out, (Document) xml);
		} else {
			return false;
		}
		return true;
	}

	/**
	 * Writes a document to the output
	 *
	 * @param out Output writer
	 * @param doc Document to write
	 */
	protected static void writeDocument(Output out, Document doc) {
		out.writeXML(doc);
	}

	// Extension points

	/**
	 * Pre processes an object TODO // must be implemented
	 *
	 * @param any Object to preprocess
	 *
	 * @return Prerocessed object
	 */
	public static Object preProcessExtension(Object any) {
		// Does nothing right now but will later
		return any;
	}

	/**
	 * Writes a custom data to the output
	 *
	 * @param out Output writer
	 * @param obj Custom data
	 *
	 * @return <code>true</code> if the object has been written, otherwise
	 * <code>false</code>
	 */
	protected static boolean writeCustomType(Output out, Object obj) {
		if (out.isCustom(obj)) {
			// Write custom data
			out.writeCustom(obj);
			return true;
		} else {
			return false;
		}
	}

}
