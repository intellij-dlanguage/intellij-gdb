package uk.co.cwspencer.ideagdb.gdbmi;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a result record from a GDB/MI stream.
 */
public class GdbMiResultRecord extends GdbMiRecord
{
	/**
	 * The result/async class.
	 */
	public String className;

	/**
	 * The results.
	 */
	public List<GdbMiResult> results = new ArrayList<GdbMiResult>();

	/**
	 * Constructor.
	 * @param type The record type.
	 * @param userToken The user token. May be null.
	 */
	public GdbMiResultRecord(Type type, Long userToken)
	{
		this.type = type;
		this.userToken = userToken;
	}
}
