package com.kdy.bean.util.stream;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kdy.bean.util.stream.IF.IStreamableFile;
import com.kdy.bean.util.stream.IF.IStreamableFileService;

public abstract class BaseStreamableFileService implements IStreamableFileService {

	private static final Logger log = LoggerFactory.getLogger(BaseStreamableFileService.class);

	/**
	 * {@inheritDoc}
	 */
	public abstract String getPrefix();

	/**
	 * {@inheritDoc}
	 */
	public void setPrefix(String prefix) {
	}

	/**
	 * {@inheritDoc}
	 */
	public abstract String getExtension();

	/**
	 * {@inheritDoc}
	 */
	public void setExtension(String extension) {
	}

	/**
	 * {@inheritDoc}
	 */
	public String prepareFilename(String name) {
		String prefix = getPrefix() + ':';
		if (name.startsWith(prefix)) {
			name = name.substring(prefix.length());
			
			// if there is no extension on the file add the first one
			/*
			 * 파일이름이 xxx.flv같이 3글자인 경우 -1 == -1 인 경우가 되서 다음 if문 로직이 돌지 않아서 파일이름에 확장자가 붙지 않는 오류가 있다. if문 주석처리 해놓음.
			if (name.lastIndexOf('.') != name.length() - 4) {
				name = name + getExtension().split(",")[0];
			}
			*/
			name = name + getExtension().split(",")[0];
		}
		return name;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean canHandle(File file) {
		boolean valid = false;
		if (file.exists()) {
			String absPath = file.getAbsolutePath().toLowerCase();
			String fileExt = absPath.substring(absPath.lastIndexOf('.'));
			log.debug("canHandle - Path: {} Ext: {}", absPath, fileExt);
			String[] exts = getExtension().split(",");
			for (String ext : exts) {
				if (ext.equals(fileExt)) {
					valid = true;
					break;
				}
			}
		}
		return valid;
	}

	/**
	 * {@inheritDoc}
	 */
	public abstract IStreamableFile getStreamableFile(File file) throws IOException;

}
