package uk.co.cwspencer.ideagdb.gdbmi;

/**
 * Class representing a single result from a GDB/MI result record.
 */
public class GdbMiResult
{
	/**
	 * Name of the variable.
	 */
	public String variable;

	/**
	 * Value of the variable.
	 */
	public GdbMiValue value = new GdbMiValue();

	/**
	 * Constructor.
	 * @param variable The name of the variable.
	 */
	public GdbMiResult(String variable)
	{
		this.variable = variable;
	}
}
