package uk.co.cwspencer.ideagdb.debug;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import org.jetbrains.annotations.Nullable;
import uk.co.cwspencer.gdb.messages.GdbStoppedEvent;

public class GdbExecutionStack extends XExecutionStack
{
	private static final Logger m_log =
		Logger.getInstance("#uk.co.cwspencer.ideagdb.debug.GdbExecutionStack");

	// The top of the stack
	private GdbExecutionStackFrame m_topFrame;

	/**
	 * Constructor.
	 * @param stopEvent The stop event.
	 */
	public GdbExecutionStack(GdbStoppedEvent stopEvent)
	{
		super("Stack");

		// Get the top of the stack
		if (stopEvent.frame != null)
		{
			m_topFrame = new GdbExecutionStackFrame(stopEvent.frame);
		}
	}

	@Nullable
	@Override
	public XStackFrame getTopFrame()
	{
		return m_topFrame;
	}

	@Override
	public void computeStackFrames(int firstFrameIndex, XStackFrameContainer container)
	{
		m_log.warn("computeStackFrames: stub");
	}
}
