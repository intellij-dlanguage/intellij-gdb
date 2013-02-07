package uk.co.cwspencer.ideagdb.debug;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import org.jetbrains.annotations.Nullable;
import uk.co.cwspencer.gdb.Gdb;
import uk.co.cwspencer.gdb.messages.GdbErrorEvent;
import uk.co.cwspencer.gdb.messages.GdbEvent;
import uk.co.cwspencer.gdb.messages.GdbStackFrame;
import uk.co.cwspencer.gdb.messages.GdbStackTrace;
import uk.co.cwspencer.gdb.messages.GdbStoppedEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GdbExecutionStack extends XExecutionStack
{
	private static final Logger m_log =
		Logger.getInstance("#uk.co.cwspencer.ideagdb.debug.GdbExecutionStack");

	// Number of stack frames requested from GDB in one go
	private static final int StackFrameRequestCount = 100;

	// The GDB instance
	private Gdb m_gdb;

	// The top of the stack
	private GdbExecutionStackFrame m_topFrame;

	/**
	 * Constructor.
	 * @param gdb Handle to the GDB instance.
	 * @param stopEvent The stop event.
	 */
	public GdbExecutionStack(Gdb gdb, GdbStoppedEvent stopEvent)
	{
		super("Stack");
		m_gdb = gdb;

		// Get the top of the stack
		if (stopEvent.frame != null)
		{
			m_topFrame = new GdbExecutionStackFrame(stopEvent.frame);
		}
	}

	/**
	 * Returns the frame at the top of the stack.
	 * @return The stack fram.
	 */
	@Nullable
	@Override
	public XStackFrame getTopFrame()
	{
		return m_topFrame;
	}

	/**
	 * Gets the stack trace starting at the given index. This passes the request and returns
	 * immediately; the data is supplied to container asynchronously.
	 * @param firstFrameIndex The first frame to retrieve, where 0 is the top of the stack.
	 * @param container Container into which the stack frames are inserted.
	 */
	@Override
	public void computeStackFrames(int firstFrameIndex, final XStackFrameContainer container)
	{
		try
		{
			String command = "-stack-list-frames " + firstFrameIndex + " " +
				(firstFrameIndex + StackFrameRequestCount);
			m_gdb.sendCommand(command, new Gdb.GdbEventCallback()
				{
					@Override
					public void onGdbCommandCompleted(GdbEvent event)
					{
						onGdbStackTraceReady(event, container);
					}
				});
		}
		catch (IOException ex)
		{
			container.errorOccurred("Failed to communicate with GDB");
			m_log.error("Failed to communicate with GDB", ex);
		}
	}

	/**
	 * Callback function for when GDB has responded to our stack trace request.
	 * @param event The event.
	 * @param container The container passed to computeStackFrames().
	 */
	private void onGdbStackTraceReady(GdbEvent event, XStackFrameContainer container)
	{
		m_log.warn("GDB stack: " + event);
		if (event instanceof GdbErrorEvent)
		{
			container.errorOccurred(((GdbErrorEvent) event).message);
			return;
		}
		if (!(event instanceof GdbStackTrace))
		{
			container.errorOccurred("Unexpected data received from GDB");
			m_log.warn("Unexpected event " + event + " received from -stack-list-frames request");
			return;
		}

		// Inspect the stack trace
		GdbStackTrace stackTrace = (GdbStackTrace) event;
		if (stackTrace.stack == null || stackTrace.stack.isEmpty())
		{
			// No data
			container.addStackFrames(new ArrayList<XStackFrame>(0), true);
		}

		// Build a list of GdbExecutionStaceFrames
		List<GdbExecutionStackFrame> stack = new ArrayList<GdbExecutionStackFrame>();
		for (GdbStackFrame frame : stackTrace.stack)
		{
			stack.add(new GdbExecutionStackFrame(frame));
		}

		// Pass the data on
		boolean last = stack.size() < StackFrameRequestCount;
		container.addStackFrames(stack, last);
	}
}
