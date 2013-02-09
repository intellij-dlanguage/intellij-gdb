package uk.co.cwspencer.ideagdb.debug;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.xdebugger.XDebuggerBundle;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XValueChildrenList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.cwspencer.gdb.Gdb;
import uk.co.cwspencer.gdb.messages.GdbErrorEvent;
import uk.co.cwspencer.gdb.messages.GdbEvent;
import uk.co.cwspencer.gdb.messages.GdbStackFrame;
import uk.co.cwspencer.gdb.messages.GdbVariables;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class for providing information about a stack frame.
 */
public class GdbExecutionStackFrame extends XStackFrame
{
	private static final Logger m_log =
		Logger.getInstance("#uk.co.cwspencer.ideagdb.debug.GdbExecutionStackFrame");

	// The GDB instance
	private Gdb m_gdb;

	// The GDB stack frame
	private GdbStackFrame m_frame;

	/**
	 * Constructor.
	 * @param gdb Handle to the GDB instance.
	 * @param frame The GDB stack frame to wrap.
	 */
	public GdbExecutionStackFrame(Gdb gdb, GdbStackFrame frame)
	{
		m_gdb = gdb;
		m_frame = frame;
	}

	/**
	 * Gets the source position of the stack frame, if available.
	 * @return The source position, or null if it is not available.
	 */
	@Nullable
	@Override
	public XSourcePosition getSourcePosition()
	{
		if (m_frame.fileAbsolute == null || m_frame.line == null)
		{
			return null;
		}

		String path = m_frame.fileAbsolute.replace(File.separatorChar, '/');
		VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
		if (file == null)
		{
			return null;
		}

		return XDebuggerUtil.getInstance().createPosition(file, m_frame.line - 1);
	}

	/**
	 * Controls the presentation of the frame in the stack trace.
	 * @param component The stack frame visual component.
	 */
	@Override
	public void customizePresentation(SimpleColoredComponent component)
	{
		if (m_frame.address == null)
		{
			component.append(XDebuggerBundle.message("invalid.frame"),
				SimpleTextAttributes.ERROR_ATTRIBUTES);
			return;
		}

		// Format the frame information
		XSourcePosition sourcePosition = getSourcePosition();
		if (m_frame.function != null)
		{
			component.append(m_frame.function + "()", SimpleTextAttributes.REGULAR_ATTRIBUTES);

			if (sourcePosition != null)
			{
				component.append(":" + (sourcePosition.getLine() + 1),
					SimpleTextAttributes.REGULAR_ATTRIBUTES);

				component.append(" (" + sourcePosition.getFile().getName() + ")",
					SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
			}
			else
			{
				component.append(" (0x" + Long.toHexString(m_frame.address) + ")",
					SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
			}
		}
		else if (sourcePosition != null)
		{
			component.append(
				sourcePosition.getFile().getName() + ":" + (sourcePosition.getLine() + 1),
				SimpleTextAttributes.REGULAR_ATTRIBUTES);
		}
		else
		{
			String addressStr = "0x" + Long.toHexString(m_frame.address);
			component.append(addressStr, SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
			component.appendFixedTextFragmentWidth(addressStr.length());
		}
		component.setIcon(AllIcons.Debugger.StackFrame);
	}

	/**
	 * Gets the variables available on this frame. This passes the request and returns immediately;
	 * the data is supplied to node asynchronously.
	 * @param node The node into which the variables are inserted.
	 */
	@Override
	public void computeChildren(@NotNull final XCompositeNode node)
	{
		try
		{
			String command = "-stack-list-variables --all-values";
			m_gdb.sendCommand(command, new Gdb.GdbEventCallback()
				{
					@Override
					public void onGdbCommandCompleted(GdbEvent event)
					{
						onGdbVariablesReady(event, node);
					}
				});
		}
		catch (IOException ex)
		{
			node.setErrorMessage("Failed to communicate with GDB");
			m_log.error("Failed to communicate with GDB", ex);
		}
	}

	/**
	 * Callback function for when GDB has responded to our stack variables request.
	 * @param event The event.
	 * @param node The node passed to computeChildren().
	 */
	private void onGdbVariablesReady(GdbEvent event, XCompositeNode node)
	{
		if (event instanceof GdbErrorEvent)
		{
			node.setErrorMessage(((GdbErrorEvent) event).message);
			return;
		}
		if (!(event instanceof GdbVariables))
		{
			node.setErrorMessage("Unexpected data received from GDB");
			m_log.warn("Unexpected event " + event + " received from -stack-list-variables " +
				"request");
			return;
		}

		// Inspect the data
		GdbVariables variables = (GdbVariables) event;
		if (variables.variables == null || variables.variables.isEmpty())
		{
			// No data
			node.addChildren(XValueChildrenList.EMPTY, true);
		}

		// Build a XValueChildrenList
		XValueChildrenList children = new XValueChildrenList(variables.variables.size());
		for (Map.Entry<String, String> variable : variables.variables.entrySet())
		{
			children.add(variable.getKey(), new GdbValue(variable.getValue()));
		}

		// Pass the data on
		node.addChildren(children, true);
	}
}
