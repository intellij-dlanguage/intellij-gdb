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

	// Handle to the ASCII character set
	private static Charset m_ascii = Charset.forName("US-ASCII");

	// Handle to the GDB process
	private Process m_gdbProcess;

	// Token which the next GDB command will be sent with
	private long m_token = 1;

	// Flag indicating whether we have sent the user-specified startup commands to GDB yet
	private boolean m_sentStartupCommands = false;

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
			m_console.print("> " + m_facet.getConfiguration().GDB_PATH + " --interpreter=mi2\n",
				ConsoleViewContentType.USER_INPUT);
			final String[] commandLine = {
				m_facet.getConfiguration().GDB_PATH,
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
			m_log.error("GDB processing error", ex);
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
					StringBuilder sb = new StringBuilder();
					if (record.userToken != null)
					{
						sb.append("<");
						sb.append(record.userToken);
						sb.append(" ");
					}
					sb.append(streamRecord.message);
					m_console.print(sb.toString(), ConsoleViewContentType.SYSTEM_OUTPUT);
				}
				break;

			case Target:
				{
					GdbMiStreamRecord streamRecord = (GdbMiStreamRecord) record;
					m_console.print(streamRecord.message, ConsoleViewContentType.NORMAL_OUTPUT);
				}
				break;

			case Log:
				{
					GdbMiStreamRecord streamRecord = (GdbMiStreamRecord) record;
					m_log.info("GDB: " + streamRecord.message);
				}
				break;

			case Immediate:
				{
					GdbMiResultRecord resultRecord = (GdbMiResultRecord) record;
					StringBuilder sb = new StringBuilder();
					if (record.userToken != null)
					{
						sb.append("<");
						sb.append(record.userToken);
						sb.append(" ");
					}
					sb.append(resultRecord);
					sb.append("\n");
					m_console.print(sb.toString(), ConsoleViewContentType.SYSTEM_OUTPUT);
				}
				break;
			}
		}

		// Send startup commands if necessary
		if (!m_sentStartupCommands)
		{
			m_sentStartupCommands = true;
			try
			{
				// Send user defined startup commands
				String userStartupCommands = m_facet.getConfiguration().STARTUP_COMMANDS;
				if (!userStartupCommands.isEmpty())
				{
					sendCommands(userStartupCommands);
				}

				// Define the application executable
				String appPath = m_facet.getConfiguration().APP_PATH;
				if (!appPath.isEmpty())
				{
					sendCommands("-file-exec-and-symbols " + GdbMiUtil.formatGdbString(appPath));
				}
			}
			catch (IOException ex)
			{
				m_log.error("Failed to send startup commands to GDB", ex);
			}
		}
	}

	/**
	 * Sends the given commands to GDB.
	 * @param commands The commands to send.
	 */
	private void sendCommands(String commands) throws IOException
	{
		// Make sure the commands are formatted properly
		StringBuilder sb = new StringBuilder();
		String[] commandsArray = commands.split("\\r?\\n");
		for (String command : commandsArray)
		{
			long token = m_token++;
			sb.append(token);
			sb.append(command);
			sb.append("\r\n");
			m_console.print(token + "> " + command + "\n", ConsoleViewContentType.USER_INPUT);
		}

		byte[] message = sb.toString().getBytes(m_ascii);
		m_gdbProcess.getOutputStream().write(message);
		m_gdbProcess.getOutputStream().flush();
	}
}
