package com.kdy.bean.util.io.event.IF;

public interface IEventListener {

	/**
	 * Notify of event.
	 *
	 * @param event the event object
	 */
	public void notifyEvent(IEvent event);
}