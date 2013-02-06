package uk.co.cwspencer.gdb.messages;

import uk.co.cwspencer.gdb.messages.annotations.GdbMiField;
import uk.co.cwspencer.gdb.messages.annotations.GdbMiObject;
import uk.co.cwspencer.gdb.gdbmi.GdbMiValue;

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
	 * The name of the file being executed.
	 */
	@SuppressWarnings("unused")
	@GdbMiField(name = "file", valueType = GdbMiValue.Type.String)
	public String file;

	/**
	 * The full path to the file being executed.
	 */
	@SuppressWarnings("unused")
	@GdbMiField(name = "fullname", valueType = GdbMiValue.Type.String)
	public String filePath;

	/**
	 * The line number being executed.
	 */
	@SuppressWarnings("unused")
	@GdbMiField(name = "line", valueType = GdbMiValue.Type.String)
	public Integer line;

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
