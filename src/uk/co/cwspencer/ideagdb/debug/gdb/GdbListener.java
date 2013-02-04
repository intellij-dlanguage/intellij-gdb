package uk.co.cwspencer.ideagdb.debug.gdb;

import uk.co.cwspencer.ideagdb.debug.gdb.messages.GdbEvent;
import uk.co.cwspencer.ideagdb.gdbmi.GdbMiResultRecord;
import uk.co.cwspencer.ideagdb.gdbmi.GdbMiStreamRecord;

/**
 * Interface that users of the Gdb class must implement to receive events.
 */
public interface GdbListener
{
	/**
	 * Called when a GDB error occurs.
	 * @param ex The exception
	 */
	void onGdbError(Throwable ex);

	/**
	 * Called when GDB has started.
	 */
	void onGdbStarted();

	/**
	 * Called when an event is received from GDB.
	 * @param event The event.
	 */
	void onGdbEventReceived(GdbEvent event);

	/**
	 * Called when a stream record is received.
	 * This should only be used for logging or advanced behaviour. Prefer to use
	 * onGdbEventReceived() instead.
	 * @param record The record.
	 */
	void onStreamRecordReceived(GdbMiStreamRecord record);

	/**
	 * Called when a result record is received.
	 * This should only be used for logging or advanced behaviour. Prefer to use
	 * onGdbEventReceived() instead.
	 * @param record The record.
	 */
	void onResultRecordReceived(GdbMiResultRecord record);
}
