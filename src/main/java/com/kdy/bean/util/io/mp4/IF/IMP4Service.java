package com.kdy.bean.util.io.mp4.IF;

import com.kdy.bean.util.io.object.Deserializer;
import com.kdy.bean.util.io.object.Serializer;

public interface IMP4Service {
	/**
	 * Sets the serializer
	 *
	 * @param serializer Serializer object
	 */
	public void setSerializer(Serializer serializer);

	/**
	 * Sets the deserializer
	 *
	 * @param deserializer Deserializer object
	 */
	public void setDeserializer(Deserializer deserializer);

}
