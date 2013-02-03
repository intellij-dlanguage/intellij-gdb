package uk.co.cwspencer.ideagdb.gdbmi;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a message read from a GDB process via GDB/MI.
 */
public class GdbMiMessage
{
	/**
	 * Records in the message.
	 */
	public List<GdbMiRecord> records = new ArrayList<GdbMiRecord>();
}
