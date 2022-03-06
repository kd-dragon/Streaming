package com.kdy.bean.convert;

/**
 * FLV TO MPEGTS TS Writer Interface
 * 
 *
 */
public interface IMP4TOMPEGTSWriter {

	public void nextBlock(long ts, byte[] block);
}
