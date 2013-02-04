package uk.co.cwspencer.ideagdb.debug;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GdbExecutionStack extends XExecutionStack
{
	private static final Logger m_log =
		Logger.getInstance("#uk.co.cwspencer.ideagdb.debug.GdbExecutionStack");

	List<GdbExecutionStackFrame> m_stack;

	public GdbExecutionStack(String name)
	{
		super(name);

		// Generate a dummy stack
		m_stack = new ArrayList<GdbExecutionStackFrame>();
		m_stack.add(new GdbExecutionStackFrame());
		m_stack.add(new GdbExecutionStackFrame());
	}

	@Nullable
	@Override
	public XStackFrame getTopFrame()
	{
		return m_stack.get(0);
	}

	@Override
	public void computeStackFrames(int firstFrameIndex, XStackFrameContainer container)
	{
		m_log.warn("computeStackFrames: parameter ignored [firstFrameIndex=" + firstFrameIndex +
			"]");
		container.addStackFrames(m_stack, true);
	}
}
