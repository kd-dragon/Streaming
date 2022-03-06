package com.kdy.bean.util.media.Interface;

public interface IMetaCue extends IMeta, Comparable<Object> {

	/**
	 * Gets the name
	 *
	 * @return name         Cue point name
	 */
	public String getName();

	/**
	 * Sets the name
	 *
	 * @param name Cue point name
	 */
	public void setName(String name);

	/**
	 * Gets the type
	 *
	 * @return type         Cue point type
	 */
	public String getType();

	/**
	 * Sets the type type can be "event" or "navigation"
	 *
	 * @param type Cue point type
	 */
	public void setType(String type);

	/**
	 * Gets the time
	 *
	 * @return time          Timestamp
	 */
	public double getTime();

	/**
	 * Sets the time
	 *
	 * @param d Timestamp
	 */
	public void setTime(double d);

}
