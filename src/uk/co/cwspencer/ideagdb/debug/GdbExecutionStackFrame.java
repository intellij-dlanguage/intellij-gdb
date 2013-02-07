package uk.co.cwspencer.ideagdb.debug;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.xdebugger.XDebuggerBundle;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XStackFrame;
import org.jetbrains.annotations.Nullable;
import uk.co.cwspencer.gdb.messages.GdbStackFrame;

import java.io.File;

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

		return XDebuggerUtil.getInstance().createPosition(file, m_frame.line);
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
			component.append(m_frame.function + "():", SimpleTextAttributes.REGULAR_ATTRIBUTES);

			if (sourcePosition != null)
			{
				component.append(Integer.toString(sourcePosition.getLine() + 1),
					SimpleTextAttributes.REGULAR_ATTRIBUTES);

				component.append(" (" + sourcePosition.getFile().getName() + ")",
					SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
			}
			else
			{
				component.append("0x" + m_frame.address,
					SimpleTextAttributes.REGULAR_ATTRIBUTES);
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
			component.append("0x" + m_frame.address + "()",
				SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
		}
		component.setIcon(AllIcons.Debugger.StackFrame);
	}
}
