package uk.co.cwspencer.ideagdb.gdbmi;

import java.util.List;

/**
 * Class representing a variable value read from a GDB/MI stream.
 */
public class GdbMiValue
{
	/**
	 * Possible types the value can take.
	 */
	public enum Type
	{
		String,
		Tuple,
		List
	}

	/**
	 * Type of the value.
	 */
	public Type type;

	/**
	 * String. Will be null if type is not String.
	 */
	public String string;

	/**
	 * Tuple. Will be null if type is not Tuple.
	 */
	public List<GdbMiResult> tuple;

	/**
	 * List. Will be null if type is not List.
	 */
	public GdbMiList list;

	/**
	 * Default constructor.
	 */
	public GdbMiValue()
	{
	}

	/**
	 * Constructor; sets the type only.
	 */
	public GdbMiValue(Type type)
	{
		this.type = type;
	}
}
