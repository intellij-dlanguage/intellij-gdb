package uk.co.cwspencer.ideagdb.debug;

import com.intellij.icons.AllIcons;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.xdebugger.XDebuggerBundle;
import com.intellij.xdebugger.frame.XStackFrame;
import uk.co.cwspencer.gdb.messages.GdbStackFrame;

/**
 * Class for providing information about a stack frame.
 */
public class GdbExecutionStackFrame extends XStackFrame
{
	// The GDB stack frame
	private GdbStackFrame m_frame;

	/**
	 * Constructor.
	 * @param frame The GDB stack frame to wrap.
	 */
	public GdbExecutionStackFrame(GdbStackFrame frame)
	{
		m_frame = frame;
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
		StringBuilder sb = new StringBuilder();
		sb.append("0x");
		sb.append(String.format("%x", m_frame.address));

		if (m_frame.function != null)
		{
			sb.append(" in ");
			sb.append(m_frame.function);
			sb.append(" ()");
		}

		component.append(sb.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
		component.setIcon(AllIcons.Debugger.StackFrame);
	}
}
