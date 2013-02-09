package uk.co.cwspencer.ideagdb.debug.breakpoints;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import org.jetbrains.annotations.NotNull;
import uk.co.cwspencer.gdb.Gdb;
import uk.co.cwspencer.gdb.messages.GdbBreakpoint;
import uk.co.cwspencer.gdb.messages.GdbErrorEvent;
import uk.co.cwspencer.gdb.messages.GdbEvent;
import uk.co.cwspencer.ideagdb.debug.GdbDebugProcess;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GdbBreakpointHandler extends
	XBreakpointHandler<XLineBreakpoint<GdbBreakpointProperties>>
{
	private static final Logger m_log =
		Logger.getInstance("#uk.co.cwspencer.ideagdb.debug.breakpoints.GdbBreakpointHandler");

	private Gdb m_gdb;
	private GdbDebugProcess m_debugProcess;

	// The breakpoints that have been set and their GDB breakpoint numbers
	private Map<Integer, XLineBreakpoint<GdbBreakpointProperties>> m_breakpoints =
		new HashMap<Integer, XLineBreakpoint<GdbBreakpointProperties>>();

	public GdbBreakpointHandler(Gdb gdb, GdbDebugProcess debugProcess)
	{
		super(GdbBreakpointType.class);
		m_gdb = gdb;
		m_debugProcess = debugProcess;
	}

	/**
	 * Registers the given breakpoint with GDB.
	 * @param breakpoint The breakpoint.
	 */
	@Override
	public void registerBreakpoint(
		@NotNull final XLineBreakpoint<GdbBreakpointProperties> breakpoint)
	{
		try
		{
			// TODO: I think we can use tracepoints here if the suspend policy isn't to stop the
			// process
			XSourcePosition sourcePosition = breakpoint.getSourcePosition();
			String command = "-break-insert -f " + sourcePosition.getFile().getPath() + ":" +
				(sourcePosition.getLine() + 1);
			m_gdb.sendCommand(command, new Gdb.GdbEventCallback()
			{
				@Override
				public void onGdbCommandCompleted(GdbEvent event)
				{
					onGdbBreakpointReady(event, breakpoint);
				}
			});
		}
		catch (IOException ex)
		{
			m_debugProcess.getSession().updateBreakpointPresentation(breakpoint,
				AllIcons.Debugger.Db_invalid_breakpoint, "Failed to communicate with GDB");
			m_log.error("Failed to communicate with GDB", ex);
		}
	}

	/**
	 * Unregisters the given breakpoint with GDB.
	 * @param breakpoint The breakpoint.
	 * @param temporary Whether we are deleting the breakpoint or temporarily disabling it.
	 */
	@Override
	public void unregisterBreakpoint(@NotNull XLineBreakpoint<GdbBreakpointProperties> breakpoint,
		boolean temporary)
	{
		m_log.warn("unregisterBreakpoint: stub");
	}

	/**
	 * Finds a breakpoint by its GDB number.
	 * @param number The GDB breakpoint number.
	 * @return The breakpoint, or null if it could not be found.
	 */
	public XLineBreakpoint<GdbBreakpointProperties> findBreakpoint(int number)
	{
		return m_breakpoints.get(number);
	}

	/**
	 * Callback function for when GDB has responded to our breakpoint request.
	 * @param event The event.
	 * @param breakpoint The breakpoint we tried to set.
	 */
	private void onGdbBreakpointReady(GdbEvent event,
		XLineBreakpoint<GdbBreakpointProperties> breakpoint)
	{
		if (event instanceof GdbErrorEvent)
		{
			m_debugProcess.getSession().updateBreakpointPresentation(breakpoint,
				AllIcons.Debugger.Db_invalid_breakpoint, ((GdbErrorEvent) event).message);
			return;
		}
		if (!(event instanceof GdbBreakpoint))
		{
			m_debugProcess.getSession().updateBreakpointPresentation(breakpoint,
				AllIcons.Debugger.Db_invalid_breakpoint, "Unexpected data received from GDB");
			m_log.warn("Unexpected event " + event + " received from -break-insert request");
			return;
		}

		// Save the breakpoint
		GdbBreakpoint gdbBreakpoint = (GdbBreakpoint) event;
		if (gdbBreakpoint.number == null)
		{
			m_debugProcess.getSession().updateBreakpointPresentation(breakpoint,
				AllIcons.Debugger.Db_invalid_breakpoint, "No breakpoint number received from GDB");
			m_log.warn("No breakpoint number received from GDB after -break-insert request");
			return;
		}
		m_breakpoints.put(gdbBreakpoint.number, breakpoint);

		// Mark the breakpoint as set
		// TODO: Don't do this yet if the breakpoint is pending
		m_debugProcess.getSession().updateBreakpointPresentation(breakpoint,
			AllIcons.Debugger.Db_verified_breakpoint, null);
	}
}
