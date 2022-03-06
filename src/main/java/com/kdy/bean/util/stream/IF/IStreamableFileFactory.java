package com.kdy.bean.util.stream.IF;

import java.io.File;
import java.util.Set;

public interface IStreamableFileFactory {

	public static String BEAN_NAME = "streamableFileFactory";

	public abstract IStreamableFileService getService(File fp);

	/**
	 * Getter for services
	 *
	 * @return Set of streamable file services
	 */
	public abstract Set<IStreamableFileService> getServices();

}
