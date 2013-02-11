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
import uk.co.cwspencer.gdb.messages.GdbThread;

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

	// The thread
	private GdbThread m_thread;

	// The top of the stack
	private GdbExecutionStackFrame m_topFrame;

	/**
	 * Constructor.
	 * @param gdb Handle to the GDB instance.
	 * @param thread The thread.
	 */
	public GdbExecutionStack(Gdb gdb, GdbThread thread)
	{
		// The super() has to be the first call, hence the ugly..
		super("[" + thread.id + "]" +
			(thread.name != null ? " " + thread.name :
				(thread.targetId != null ? " " + thread.targetId : "")));

		m_gdb = gdb;
		m_thread = thread;

		// Get the top of the stack
		if (thread.frame != null)
		{
			m_topFrame = new GdbExecutionStackFrame(gdb, m_thread.id, thread.frame);
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

	/**
	 * Callback function for when GDB has responded to our stack trace request.
	 * @param event The event.
	 * @param container The container passed to computeStackFrames().
	 */
	private void onGdbStackTraceReady(GdbEvent event, XStackFrameContainer container)
	{
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
			stack.add(new GdbExecutionStackFrame(m_gdb, m_thread.id, frame));
		}

		// Pass the data on
		boolean last = stack.size() < StackFrameRequestCount;
		container.addStackFrames(stack, last);
	}
}
