package uk.co.cwspencer.ideagdb.debug.gdb.messages;

import uk.co.cwspencer.ideagdb.debug.gdb.messages.annotations.GdbMiEvent;
import uk.co.cwspencer.ideagdb.debug.gdb.messages.annotations.GdbMiField;
import uk.co.cwspencer.ideagdb.gdbmi.GdbMiRecord;
import uk.co.cwspencer.ideagdb.gdbmi.GdbMiValue;

/**
 * Event fired when the target application stops.
 */
@GdbMiEvent(recordType = GdbMiRecord.Type.Exec, className = "stopped")
public class GdbStopEvent extends GdbEvent
{
	/**
	 * The current point of execution.
	 */
	@SuppressWarnings("unused")
	@GdbMiField(name = "frame", valueType = GdbMiValue.Type.Tuple)
	public GdbStackFrame frame;

	/**
	 * The thread of execution.
	 */
	@SuppressWarnings("unused")
	@GdbMiField(name = "thread-id", valueType = GdbMiValue.Type.String)
	public Integer threadId;

	/**
	 * Flag indicating whether all threads were stopped. If false, stoppedThreads contains a list of
	 * the threads that were stopped.
	 */
	@SuppressWarnings("unused")
	@GdbMiField(name = "stopped-threads", valueType = GdbMiValue.Type.String,
		valueProcessor = "processAllStopped")
	public Boolean allStopped;

	// TODO
	//public ?? stoppedThreads;

	/**
	 * Value processor for allStopped.
	 */
	@SuppressWarnings("unused")
	public Boolean processAllStopped(String value)
	{
		return value.equals("all");
	}
}
