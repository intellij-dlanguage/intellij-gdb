package uk.co.cwspencer.ideagdb.debug;

import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XSuspendContext;
import org.jetbrains.annotations.Nullable;
import uk.co.cwspencer.ideagdb.debug.gdb.messages.GdbStopEvent;

public class GdbSuspendContext extends XSuspendContext
{
	// The stack trace
	private GdbExecutionStack m_stack;

	/**
	 * Constructor.
	 * @param stopEvent The stop event that caused the suspension.
	 */
	public GdbSuspendContext(GdbStopEvent stopEvent)
	{
		m_stack = new GdbExecutionStack(stopEvent);
	}

	@Nullable
	@Override
	public XExecutionStack getActiveExecutionStack()
	{
		return m_stack;
	}
}
