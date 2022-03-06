package com.kdy.bean.util.stream.IF;

import java.io.IOException;

import com.kdy.bean.util.io.Interface.ITagReader;
import com.kdy.bean.util.io.Interface.ITagWriter;

public interface IStreamableFile {

	/**
	 * Returns a reader to parse and read the tags inside the file.
	 *
	 * @return the reader                 Tag reader
	 *
	 * @throws java.io.IOException I/O exception
	 */
	public ITagReader getReader() throws IOException;

	/**
	 * Returns a writer that creates a new file or truncates existing contents.
	 *
	 * @return the writer                  Tag writer
	 *
	 * @throws java.io.IOException I/O exception
	 */
	public ITagWriter getWriter() throws IOException;

	/**
	 * Returns a Writer which is setup to append to the file.
	 *
	 * @return the writer                  Tag writer used for append mode
	 *
	 * @throws java.io.IOException I/O exception
	 */
	public ITagWriter getAppendWriter() throws IOException;
}
