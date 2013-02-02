package uk.co.cwspencer.ideagdb.debug;

import com.intellij.execution.ExecutionResult;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import org.jetbrains.annotations.NotNull;

public class GdbDebugProcess extends XDebugProcess
{
	private static final Logger m_log =
		Logger.getInstance("#uk.co.cwspencer.ideagdb.debug.GdbDebugProcess");

	GdbDebuggerEditorsProvider m_editorsProvider = new GdbDebuggerEditorsProvider();
	ExecutionConsole m_console;

	public GdbDebugProcess(XDebugSession session, ExecutionResult executionResult)
	{
		super(session);
		m_console = executionResult.getExecutionConsole();
	}

	@NotNull
	@Override
	public XDebuggerEditorsProvider getEditorsProvider()
	{
		return m_editorsProvider;
	}

	@Override
	public void startStepOver()
	{
		m_log.warn("startStepOver: stub");
	}

	@Override
	public void startPausing()
	{
		m_log.warn("startPausing: stub");
	}

	@Override
	public void startStepInto()
	{
		m_log.warn("startStepInto: stub");
	}

	@Override
	public void startStepOut()
	{
		m_log.warn("startStepOut: stub");
	}

	@Override
	public void stop()
	{
		m_log.warn("stop: stub");
	}

	@Override
	public void resume()
	{
		m_log.warn("resume: stub");
	}

	@Override
	public void runToPosition(@NotNull XSourcePosition position)
	{
		m_log.warn("runToPosition: stub");
	}

	@NotNull
	@Override
	public ExecutionConsole createConsole()
	{
		return m_console;
	}
}
