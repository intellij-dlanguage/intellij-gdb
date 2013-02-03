package uk.co.cwspencer.ideagdb.run;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationPerRunnerSettings;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.DefaultDebugProcessHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.cwspencer.ideagdb.facet.GdbFacet;
import uk.co.cwspencer.ideagdb.gdbmi.GdbMiMessage;
import uk.co.cwspencer.ideagdb.gdbmi.GdbMiParser;
import uk.co.cwspencer.ideagdb.gdbmi.GdbMiRecord;
import uk.co.cwspencer.ideagdb.gdbmi.GdbMiResultRecord;
import uk.co.cwspencer.ideagdb.gdbmi.GdbMiStreamRecord;
import uk.co.cwspencer.ideagdb.gdbmi.GdbMiUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

public class GdbRunProfileState implements RunProfileState
{
	private static final Logger m_log =
		Logger.getInstance("#uk.co.cwspencer.ideagdb.run.GdbRunProfileState");

	private ExecutionEnvironment m_env;
	private GdbFacet m_facet;
	private ConsoleView m_console;

	public GdbRunProfileState(@NotNull ExecutionEnvironment env, @NotNull GdbFacet facet)
	{
		m_env = env;
		m_facet = facet;
	}

	@Nullable
	@Override
	public ExecutionResult execute(Executor executor, @NotNull ProgramRunner runner) throws ExecutionException
	{
		ProcessHandler processHandler = new DefaultDebugProcessHandler();

		// Create the console
		Project project = m_facet.getModule().getProject();
		final TextConsoleBuilder builder =
			TextConsoleBuilderFactory.getInstance().createBuilder(project);
		m_console = builder.getConsole();

		return new GdbExecutionResult(m_console, processHandler, m_facet);
	}

	@Override
	public RunnerSettings getRunnerSettings()
	{
		m_log.warn("getRunnerSettings: stub");
		return null;
	}

	@Override
	public ConfigurationPerRunnerSettings getConfigurationSettings()
	{
		return m_env.getConfigurationSettings();
	}
}
