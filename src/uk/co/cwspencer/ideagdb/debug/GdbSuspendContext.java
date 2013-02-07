package uk.co.cwspencer.ideagdb.debug;

import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XSuspendContext;
import org.jetbrains.annotations.Nullable;
import uk.co.cwspencer.gdb.Gdb;
import uk.co.cwspencer.gdb.messages.GdbStoppedEvent;

public class GdbSuspendContext extends XSuspendContext
{
	// The stack trace
	private GdbExecutionStack m_stack;

	/**
	 * Constructor.
	 * @param gdb Handle to the GDB instance.
	 * @param stopEvent The stop event that caused the suspension.
	 */
	public GdbSuspendContext(Gdb gdb, GdbStoppedEvent stopEvent)
	{
		m_stack = new GdbExecutionStack(gdb, stopEvent);
	}

	@Nullable
	@Override
	public XExecutionStack getActiveExecutionStack()
	{
		return m_stack;
	}
}
