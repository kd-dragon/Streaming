package com.kdy.bean.util.io.mp4;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kdy.bean.util.io.mp4.IF.IMP4Service;
import com.kdy.bean.util.io.object.Deserializer;
import com.kdy.bean.util.io.object.Serializer;
import com.kdy.bean.util.stream.BaseStreamableFileService;
import com.kdy.bean.util.stream.IF.IStreamableFile;

public class MP4Service extends BaseStreamableFileService implements IMP4Service {
	
	protected static Logger log = LoggerFactory.getLogger(MP4Service.class);

	/**
	 * File extensions handled by this service. If there are more than one, they
	 * are comma separated. '.mp4' must be the first on the list because it is the
	 * default file extension for mp4 files.
	 *
	 * @see http://help.adobe.com/en_US/flashmediaserver/devguide/WS5b3ccc516d4fbf351e63e3d11a0773d117-7fc8.html
	 */
	private static String extension = ".mp4,.f4v,.mov,.3gp,.3g2,.m4v";
	private static String prefix = "mp4";
	/**
	 * Serializer
	 */
	private Serializer serializer;
	/**
	 * Deserializer
	 */
	private Deserializer deserializer;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getPrefix() {
		return prefix;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setPrefix(String prefix) {
		MP4Service.prefix = prefix;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getExtension() {
		return extension;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setExtension(String extension) {
		MP4Service.extension = extension;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IStreamableFile getStreamableFile(File file) throws IOException {
		return new MP4(file);
	}

	/**
	 * Getter for serializer
	 *
	 * @return Serializer
	 */
	public Serializer getSerializer() {
		return serializer;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSerializer(Serializer serializer) {
		this.serializer = serializer;
	}

	/**
	 * Getter for deserializer
	 *
	 * @return Deserializer
	 */
	public Deserializer getDeserializer() {
		return deserializer;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDeserializer(Deserializer deserializer) {
		this.deserializer = deserializer;

	}

}
