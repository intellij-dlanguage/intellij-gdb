package uk.co.cwspencer.ideagdb.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationPerRunnerSettings;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GdbRunProfileState implements RunProfileState
{
	private static final Logger m_log =
		Logger.getInstance("#uk.co.cwspencer.ideagdb.run.GdbRunProfileState");

	private ExecutionEnvironment m_env;

	public GdbRunProfileState(@NotNull ExecutionEnvironment env)
	{
		m_env = env;
	}

	@Nullable
	@Override
	public ExecutionResult execute(Executor executor, @NotNull ProgramRunner runner) throws ExecutionException
	{
		m_log.warn("execute: stub");
		return null;
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
