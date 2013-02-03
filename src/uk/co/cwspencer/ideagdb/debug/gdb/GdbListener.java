package uk.co.cwspencer.ideagdb.debug.gdb;

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
	 * Called when a stream record is received.
	 * @param record The record.
	 */
	void onStreamRecordReceived(GdbMiStreamRecord record);

	/**
	 * Called when a result record is received.
	 * @param record The record.
	 */
	void onResultRecordReceived(GdbMiResultRecord record);

	/**
	 * Called when GDB has started.
	 */
	void onGdbStarted();
}
