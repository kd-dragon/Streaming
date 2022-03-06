package com.kdy.bean.util.stream;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kdy.bean.util.io.mp4.MP4Service;
import com.kdy.bean.util.stream.IF.IStreamableFileFactory;
import com.kdy.bean.util.stream.IF.IStreamableFileService;

public class StreamableFileFactory implements IStreamableFileFactory {
	// Initialize Logging
	public static Logger logger = LoggerFactory.getLogger(StreamableFileFactory.class);

	private Set<IStreamableFileService> services = new HashSet<IStreamableFileService>();

	public StreamableFileFactory() {
		MP4Service mp4FileService = new MP4Service();
		services.add(mp4FileService);
	}

	/**
	 * {@inheritDoc}
	 */
	public IStreamableFileService getService(File fp) {
		//logger.info("Get service for file: " + fp.getName());
		// Return first service that can handle the passed file
		for (IStreamableFileService service : this.services) {
			if (service.canHandle(fp)) {
				//logger.debug("Found service");
				return service;
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Set<IStreamableFileService> getServices() {
		//logger.debug("StreamableFileFactory get services");
		return services;
	}

	/**
	 * Setter for services
	 *
	 * @param services Set of streamable file services
	 */
	public void setServices(Set<IStreamableFileService> services) {
		//logger.debug("StreamableFileFactory set services");
		this.services = services;
	}
}
