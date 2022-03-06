package com.kdy.bean.util.io.amf3;

public class AMF3 {

	/**
	 * Minimum possible value for integer number encoding.
	 */
	public static final long MIN_INTEGER_VALUE = -268435456;

	/**
	 * Maximum possible value for integer number encoding.
	 */
	public static final long MAX_INTEGER_VALUE = 268435455;

	/**
	 * Max string length
	 */
	public static final int LONG_STRING_LENGTH = 65535;

	/**
	 * Undefined marker
	 */
	public static final byte TYPE_UNDEFINED = 0x00;

	/**
	 * Null marker
	 */
	public static final byte TYPE_NULL = 0x01;

	/**
	 * Boolean false marker
	 */
	public static final byte TYPE_BOOLEAN_FALSE = 0x02;

	/**
	 * Boolean true marker
	 */
	public static final byte TYPE_BOOLEAN_TRUE = 0x03;

	/**
	 * Integer marker
	 */
	public static final byte TYPE_INTEGER = 0x04;

	/**
	 * Number marker
	 */
	public static final byte TYPE_NUMBER = 0x05;

	/**
	 * String marker
	 */
	public static final byte TYPE_STRING = 0x06;

	/**
	 * XML document marker
	 * <br />
	 * This is for the legacy XMLDocument type is retained in the language
	 * as flash.xml.XMLDocument. Similar to AMF 0, the structure of an
	 * XMLDocument needs to be flattened into a string representation for
	 * serialization. As with other strings in AMF, the content is encoded in
	 * UTF-8.
	 * XMLDocuments can be sent as a reference to a previously occurring
	 * XMLDocument instance by using an index to the implicit object reference
	 * table.
	 */
	public static final byte TYPE_XML_DOCUMENT = 0x07;

	/**
	 * Date marker
	 */
	public static final byte TYPE_DATE = 0x08;

	/**
	 * Array start marker
	 */
	public static final byte TYPE_ARRAY = 0x09;

	/**
	 * Object start marker
	 */
	public static final byte TYPE_OBJECT = 0x0A;

	/**
	 * XML start marker
	 */
	public static final byte TYPE_XML = 0x0B;

	/**
	 * ByteArray marker
	 */
	public static final byte TYPE_BYTEARRAY = 0x0C;

	/**
	 * Vector<int> marker
	 */
	public static final byte TYPE_VECTOR_INT = 0x0D;

	/**
	 * Vector<uint> marker
	 */
	public static final byte TYPE_VECTOR_UINT = 0x0E;

	/**
	 * Vector<Number> marker
	 */
	public static final byte TYPE_VECTOR_NUMBER = 0x0F;

	/**
	 * Vector<Object> marker
	 */
	public static final byte TYPE_VECTOR_OBJECT = 0x10;

	/**
	 * Property list encoding.
	 * <p>
	 * The remaining integer-data represents the number of class members
	 * that exist. The property names are read as string-data. The values
	 * are then read as AMF3-data.
	 */
	public static final byte TYPE_OBJECT_PROPERTY = 0x00;

	/**
	 * Externalizable object.
	 * <p>
	 * What follows is the value of the "inner" object, including type code.
	 * This value appears for objects that implement IExternalizable, such
	 * as ArrayCollection and ObjectProxy.
	 */
	public static final byte TYPE_OBJECT_EXTERNALIZABLE = 0x01;

	/**
	 * Name-value encoding.
	 * <p>
	 * The property names and values are encoded as string-data followed by
	 * AMF3-data until there is an empty string property name. If there is
	 * a class-def reference there are no property names and the number of
	 * values is equal to the number of properties in the class-def.
	 */
	public static final byte TYPE_OBJECT_VALUE = 0x02;

	/**
	 * Proxy object.
	 */
	public static final byte TYPE_OBJECT_PROXY = 0x03;
}
