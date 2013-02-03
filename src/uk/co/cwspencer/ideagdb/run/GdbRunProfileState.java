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
import uk.co.cwspencer.ideagdb.gdbmi.GdbMiMessage;
import uk.co.cwspencer.ideagdb.gdbmi.GdbMiParser;
import uk.co.cwspencer.ideagdb.gdbmi.GdbMiRecord;
import uk.co.cwspencer.ideagdb.gdbmi.GdbMiStreamRecord;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class GdbRunProfileState implements RunProfileState
{
	private static final Logger m_log =
		Logger.getInstance("#uk.co.cwspencer.ideagdb.run.GdbRunProfileState");

	private ExecutionEnvironment m_env;
	private Project m_project;
	private ConsoleView m_console;

	// Handle to the GDB process
	private Process m_gdbProcess;

	public GdbRunProfileState(@NotNull ExecutionEnvironment env, @NotNull Project project)
	{
		m_env = env;
		m_project = project;
	}

	@Nullable
	@Override
	public ExecutionResult execute(Executor executor, @NotNull ProgramRunner runner) throws ExecutionException
	{
		ProcessHandler processHandler = new DefaultDebugProcessHandler();

		// Create the console
		final TextConsoleBuilder builder =
			TextConsoleBuilderFactory.getInstance().createBuilder(m_project);
		m_console = builder.getConsole();

		// Launch GDB
		ApplicationManager.getApplication().executeOnPooledThread(new Runnable()
			{
				@Override
				public void run()
				{
					launchGdb();
				}
			});
		return new DefaultExecutionResult(m_console, processHandler);
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

	/**
	 * Launches the GDB process.
	 */
	private void launchGdb()
	{
		try
		{
			if (m_gdbProcess != null)
			{
				throw new IllegalStateException("GDB process has already been started");
			}

			// Launch the process
			// TODO: Don't hardcode the path
			final String[] commandLine = {
				"C:\\Android\\toolchain\\bin\\i686-linux-android-gdb.exe",
				"--interpreter=mi2" };
			m_gdbProcess = Runtime.getRuntime().exec(commandLine);

			// Start listening for data
			GdbMiParser parser = new GdbMiParser();

			// Read from the stream
			InputStream stream = m_gdbProcess.getInputStream();
			byte[] buffer = new byte[4096];
			int bytes;
			while ((bytes = stream.read(buffer)) != -1)
			{
				// Process the data
				parser.process(buffer, bytes);

				// Dispatch the messages
				List<GdbMiMessage> messages = parser.getMessages();
				for (GdbMiMessage message : messages)
				{
					handleMessage(message);
				}
				messages.clear();
			}
		}
		catch (IOException ex)
		{
			m_log.error(ex);
		}
	}

	/**
	 * Handles a message received from GDB.
	 * @param message The message.
	 */
	private void handleMessage(GdbMiMessage message)
	{
		for (GdbMiRecord record : message.records)
		{
			switch (record.type)
			{
			case Console:
				{
					GdbMiStreamRecord streamRecord = (GdbMiStreamRecord) record;
					m_console.print(streamRecord.message, ConsoleViewContentType.SYSTEM_OUTPUT);
				}
				break;

			case Target:
				{
					GdbMiStreamRecord streamRecord = (GdbMiStreamRecord) record;
					m_console.print(streamRecord.message, ConsoleViewContentType.NORMAL_OUTPUT);
				}
				break;
			}
		}
	}
}
