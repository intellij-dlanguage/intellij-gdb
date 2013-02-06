package uk.co.cwspencer.gdb.messages;

/**
 * Utility functions for use with the message converter.
 */
@SuppressWarnings("unused")
public class GdbMiMessageConverterUtils
{
	/**
	 * Converts a hexadecimal string to a long.
	 */
	public static Long hexStringToLong(String value)
	{
		Long longValue = null;
		if (value.substring(0, 2).equals("0x"))
		{
			longValue = Long.parseLong(value.substring(2), 16);
		}
		return longValue;
	}
}
