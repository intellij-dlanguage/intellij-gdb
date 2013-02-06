package uk.co.cwspencer.gdb.messages;

import uk.co.cwspencer.gdb.gdbmi.GdbMiValue;
import uk.co.cwspencer.gdb.messages.annotations.GdbMiDoneEvent;
import uk.co.cwspencer.gdb.messages.annotations.GdbMiField;

import java.util.List;

/**
 * A stack trace. This is returned from a -stack-list-frames request.
 */
@SuppressWarnings("unused")
@GdbMiDoneEvent(command = "-stack-list-frames")
public class GdbStackTrace extends GdbDoneEvent
{
	/**
	 * The stack frames.
	 */
	@SuppressWarnings("unused")
	@GdbMiField(name = "stack", valueType = GdbMiValue.Type.List)
	public List<GdbStackFrame> stack;
}
