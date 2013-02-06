package uk.co.cwspencer.gdb.messages;

import uk.co.cwspencer.gdb.gdbmi.GdbMiRecord;
import uk.co.cwspencer.gdb.gdbmi.GdbMiValue;
import uk.co.cwspencer.gdb.messages.annotations.GdbMiEvent;
import uk.co.cwspencer.gdb.messages.annotations.GdbMiField;

import java.util.EnumSet;
import java.util.Map;

/**
 * Event fired when GDB connects to a remote target.
 */
@SuppressWarnings("unused")
@GdbMiEvent(recordType = GdbMiRecord.Type.Immediate, className = "connected")
public class GdbConnectedEvent
{
	/**
	 * The execution address.
	 */
	@SuppressWarnings("unused")
	@GdbMiField(name = "addr", valueType = GdbMiValue.Type.String,
		valueProcessor = "uk.co.cwspencer.gdb.messages.GdbMiMessageConverterUtils.hexStringToLong")
	public Long address;

	/**
	 * The name of the function.
	 */
	@SuppressWarnings("unused")
	@GdbMiField(name = "func", valueType = GdbMiValue.Type.String)
	public String function;

	/**
	 * The arguments to the function.
	 */
	@SuppressWarnings("unused")
	@GdbMiField(name = "args", valueType = GdbMiValue.Type.List)
	public Map<String, String> arguments;

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
