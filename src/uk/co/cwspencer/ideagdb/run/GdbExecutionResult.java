package uk.co.cwspencer.ideagdb.run;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ExecutionConsole;
import org.jetbrains.annotations.NotNull;
import uk.co.cwspencer.ideagdb.facet.GdbFacet;

public class GdbExecutionResult extends DefaultExecutionResult
{
	public GdbFacet m_facet;

	public GdbExecutionResult(ExecutionConsole console, @NotNull ProcessHandler processHandler,
		GdbFacet facet)
	{
		super(console, processHandler);
		m_facet = facet;
	}

	public GdbFacet getFacet()
	{
		return m_facet;
	}
}
