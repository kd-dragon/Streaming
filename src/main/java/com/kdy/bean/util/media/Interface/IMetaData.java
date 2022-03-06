package com.kdy.bean.util.media.Interface;

public interface IMetaData<K, V> extends IMeta {

	/**
	 * Returns a boolean depending on whether the video can
	 * seek to end
	 *
	 * @return <code>true</code> if file is seekable to the end, <code>false</code> otherwise
	 */
	public boolean getCanSeekToEnd();

	/**
	 * Sets whether a video can seek to end
	 *
	 * @param b <code>true</code> if file is seekable to the end, <code>false</code> otherwise
	 */
	public void setCanSeekToEnd(boolean b);

	/**
	 * Returns the video codec id
	 *
	 * @return Video codec id
	 */
	public int getVideoCodecId();

	/**
	 * Sets the video codec id
	 *
	 * @param id Video codec id
	 */
	public void setVideoCodecId(int id);

	public int getAudioCodecId();

	public void setAudioCodecId(int id);

	/**
	 * Returns the framerate.
	 *
	 * @return FLV framerate in frames per second
	 */
	public double getFrameRate();

	/**
	 * Sets the framerate.
	 *
	 * @param rate FLV framerate in frames per second
	 */
	public void setFrameRate(double rate);

	/**
	 * Returns the videodatarate
	 *
	 * @return Video data rate
	 */
	public int getVideoDataRate();

	/**
	 * Sets the videodatarate
	 *
	 * @param rate Video data rate
	 */
	public void setVideoDataRate(int rate);

	/**
	 * Returns the height
	 *
	 * @return height       Video height
	 */
	public int getHeight();

	/**
	 * Sets the height
	 *
	 * @param h Video height
	 */
	public void setHeight(int h);

	/**
	 * Returns the width    Video width
	 *
	 * @return width
	 */
	public int getWidth();

	/**
	 * Sets the width
	 *
	 * @param w Video width
	 */
	public void setWidth(int w);

	/**
	 * Returns the duration.
	 *
	 * @return duration     Video duration in seconds
	 */
	public double getDuration();

	/**
	 * Sets the duration.
	 *
	 * @param d Video duration in seconds
	 */
	public void setDuration(double d);

	/**
	 * Gets the cue points
	 *
	 * @return Cue points
	 */
	public IMetaCue[] getMetaCue();

	/**
	 * Sets the cue points
	 *
	 * @param metaCue Cue points
	 */
	public void setMetaCue(IMetaCue[] metaCue);
}
