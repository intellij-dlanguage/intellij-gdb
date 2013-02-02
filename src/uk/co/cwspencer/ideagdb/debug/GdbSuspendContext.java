package uk.co.cwspencer.ideagdb.debug;

import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XSuspendContext;
import org.jetbrains.annotations.Nullable;

public class GdbSuspendContext extends XSuspendContext
{
	@Nullable
	@Override
	public XExecutionStack getActiveExecutionStack()
	{
		// Return a fake stack
		return new GdbExecutionStack("Frames");
	}
}
