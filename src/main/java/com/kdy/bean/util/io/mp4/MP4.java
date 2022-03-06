package com.kdy.bean.util.io.mp4;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kdy.bean.util.io.Interface.ITagReader;
import com.kdy.bean.util.io.Interface.ITagWriter;
import com.kdy.bean.util.io.mp4.IF.IMP4;
import com.kdy.bean.util.media.MetaService;
import com.kdy.bean.util.media.Interface.IMetaData;
import com.kdy.bean.util.media.Interface.IMetaService;

public class MP4 implements IMP4 {

    protected static Logger log = LoggerFactory.getLogger(MP4.class);

    private File file;

    private IMetaService metaService;

    private IMetaData<?, ?> metaData;

    /**
     * Default constructor, used by Spring so that parameters may be injected.
     */
    public MP4() {
    }

    /**
     * Create MP4 from given file source.
     * 
     * @param file
     *            File source
     */
    public MP4(File file) {
        this.file = file;
        /*
         * try { MP4Reader reader = new MP4Reader(this.file); ITag tag = reader.createFileMeta(); if (metaService == null) { metaService = new MetaService(this.file); } metaData =
         * metaService.readMetaData(tag.getBody()); reader.close(); } catch (Exception e) { log.error("An error occurred looking for metadata:", e); }
         */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasMetaData() {
        return metaData != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IMetaData<?, ?> getMetaData() throws FileNotFoundException {
        metaService.setFile(file);
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasKeyFrameData() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setKeyFrameData(Map<?, ?> keyframedata) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<?, ?> getKeyFrameData() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refreshHeaders() throws IOException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flushHeaders() throws IOException {
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unused")
    @Override
    public ITagReader getReader() throws IOException {
        
		/**
		 * 
		MP4Reader reader = null;
        IoBuffer fileData = null;
		String fileName = file.getName();
		
        if (file.exists()) {
            //log.debug("File name: {} size: {}", fileName, file.length());
            reader = new MP4Reader(file);
            // get a ref to the mapped byte buffer
            fileData = reader.getFileData();
            //log.trace("File data size: {}", fileData);
        } else {
            //log.info("Creating new file: {}", file);
            file.createNewFile();
        }
        */
        
        return new MP4Reader(file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ITagReader readerFromNearestKeyFrame(int seekPoint) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ITagWriter getWriter() throws IOException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ITagWriter writerFromNearestKeyFrame(int seekPoint) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void setMetaData(IMetaData<?, ?> meta) throws IOException {
        if (metaService == null) {
            metaService = new MetaService(file);
        }
        //if the file is not checked the write may produce an NPE
        if (metaService.getFile() == null) {
            metaService.setFile(file);
        }
        metaService.write(meta);
        metaData = meta;
    }

    /** {@inheritDoc} */
    @Override
    public void setMetaService(IMetaService service) {
        metaService = service;
    }

    @Override
    public ITagWriter getAppendWriter() throws IOException {
        return null;
    }

}