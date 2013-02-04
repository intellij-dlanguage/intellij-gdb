package uk.co.cwspencer.ideagdb.debug.gdb.messages;

import uk.co.cwspencer.ideagdb.debug.gdb.messages.annotations.GdbMiField;
import uk.co.cwspencer.ideagdb.debug.gdb.messages.annotations.GdbMiObject;
import uk.co.cwspencer.ideagdb.gdbmi.GdbMiValue;

/**
 * Class representing information about a stack frame from GDB.
 */
@GdbMiObject
public class GdbStackFrame
{
	/**
	 * The execution address.
	 */
	@SuppressWarnings("unused")
	@GdbMiField(name = "addr", valueType = GdbMiValue.Type.String,
		valueProcessor = "processAddress")
	public Long address;

	/**
	 * The name of the function.
	 */
	@SuppressWarnings("unused")
	@GdbMiField(name = "func", valueType = GdbMiValue.Type.String)
	public String function;

	// TODO
	//public ?? arguments;

	/**
	 * Value processor for address.
	 */
	@SuppressWarnings("unused")
	public Long processAddress(String value)
	{
		Long address = null;
		if (value.substring(0, 2).equals("0x"))
		{
			address = Long.parseLong(value.substring(2), 16);
		}
		return address;
	}
}
