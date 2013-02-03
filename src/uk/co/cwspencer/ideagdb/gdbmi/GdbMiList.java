package uk.co.cwspencer.ideagdb.gdbmi;

import java.util.List;

/**
 * Class representing a list read from a GDB/MI stream.
 */
public class GdbMiList
{
	/**
	 * Possible types of lists. GDB/MI lists may contain either results or values, but not both. If
	 * the list is empty there is no way to know which was intended, so it is classified as a
	 * separate type. If the list is empty, both results and values will be null.
	 */
	public enum Type
	{
		Empty,
		Results,
		Values
	}

	/**
	 * The type of list.
 	 */
	public Type type = Type.Empty;

	/**
	 * List of results. This will be null if type is not Results.
	 */
	public List<GdbMiResult> results;

	/**
	 * List of values. This will be null if type is not Values.
	 */
	public List<GdbMiValue> values;
}
