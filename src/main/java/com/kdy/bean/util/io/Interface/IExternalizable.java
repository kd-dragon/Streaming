package com.kdy.bean.util.io.Interface;

public interface IExternalizable {
	/**
	 * Load custom object from stream.
	 *
	 * @param input object to be used for data loading
	 */
	public void readExternal(IDataInput input);

	/**
	 * Store custom object to stream.
	 *
	 * @param output object to be used for data storing
	 */
	public void writeExternal(IDataOutput output);

}
