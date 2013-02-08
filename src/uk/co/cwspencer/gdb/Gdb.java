package uk.co.cwspencer.gdb;

import com.intellij.openapi.diagnostic.Logger;
import uk.co.cwspencer.gdb.messages.GdbEvent;
import uk.co.cwspencer.gdb.messages.GdbMiMessageConverter;
import uk.co.cwspencer.gdb.gdbmi.GdbMiMessage;
import uk.co.cwspencer.gdb.gdbmi.GdbMiParser;
import uk.co.cwspencer.gdb.gdbmi.GdbMiRecord;
import uk.co.cwspencer.gdb.gdbmi.GdbMiResultRecord;
import uk.co.cwspencer.gdb.gdbmi.GdbMiStreamRecord;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for interacting with GDB.
 */
public class Gdb
{
	private static final Logger m_log = Logger.getInstance("#uk.co.cwspencer.gdb.Gdb");

	/**
	 * Interface for callbacks for results from completed GDB commands.
	 */
	public interface GdbEventCallback
	{
		/**
		 * Called when a response to the command is received.
		 * @param event The event.
		 */
		public void onGdbCommandCompleted(GdbEvent event);
	}

	// Information about a command that has been sent but GDB hasn't yet responded to
	private class PendingCommand
	{
		// The command that was sent, excluding any arguments
		String commandType;
		// The user provided callback; may be null
		GdbEventCallback callback;

		PendingCommand(String commandType, GdbEventCallback callback)
		{
			this.commandType = commandType;
			this.callback = callback;
		}
	}

	// Handle to the ASCII character set
	private static Charset m_ascii = Charset.forName("US-ASCII");

	// The listener
	private GdbListener m_listener;

	// Handle for the GDB process
	private Process m_process;

	// Thread which reads data from GDB
	private Thread m_readThread;

	// Flag indicating whether we have received the first message from GDB yet
	private boolean m_firstMessage = true;

	// Token which the next GDB command will be sent with
	private long m_token = 1;

	// Commands that have been sent to GDB and are awaiting a response
	private final Map<Long, PendingCommand> m_pendingCommands = new HashMap<Long, PendingCommand>();

	/**
	 * Constructor; launches GDB.
	 * @param gdbPath The path to the GDB executable.
	 * @param workingDirectory Working directory to launch the GDB process in. May be null.
	 * @param listener Listener that is to receive GDB events.
	 */
	public Gdb(final String gdbPath, final String workingDirectory, GdbListener listener)
	{
		m_listener = listener;
		m_readThread = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					runGdb(gdbPath, workingDirectory);
				}
			});
		m_readThread.start();
	}

	/**
	 * Sends an arbitrary command to GDB.
	 * @param command The command to send. This may be a normal CLI command or a GDB/MI command. It
	 * should not contain any line breaks.
	 * @return The token the command was sent with.
	 */
	public long sendCommand(String command) throws IOException
	{
		return sendCommand(command, null);
	}

	/**
	 * Sends an arbitrary command to GDB and requests a completion callback.
	 * @param command The command to send. This may be a normal CLI command or a GDB/MI command. It
	 * should not contain any line breaks.
	 * @param callback The callback function.
	 * @return The token the command was sent with.
	 */
	public long sendCommand(String command, GdbEventCallback callback) throws IOException
	{
		// Construct the message
		long token = m_token++;

		StringBuilder sb = new StringBuilder();
		sb.append(token);
		sb.append(command);
		sb.append("\r\n");

		// Get the command type
		int separatorIndex = command.indexOf(' ');
		String commandType = separatorIndex == -1 ? command : command.substring(0, separatorIndex);

		// Save the callback now since it is possible otherwise that GDB would respond before we
		// insert the item into the map
		PendingCommand pendingCommand = new PendingCommand(commandType, callback);
		synchronized (m_pendingCommands)
		{
			m_pendingCommands.put(token, pendingCommand);
		}

		// Send the message
		byte[] message = sb.toString().getBytes(m_ascii);
		m_process.getOutputStream().write(message);
		m_process.getOutputStream().flush();

		return token;
	}

	/**
	 * Launches the GDB process and starts listening for data.
	 * @param gdbPath Path to the GDB executable.
	 * @param workingDirectory Working directory to launch the GDB process in. May be null.
	 */
	private void runGdb(String gdbPath, String workingDirectory)
	{
		try
		{
			// Launch the process
			final String[] commandLine = {
				gdbPath,
				"--interpreter=mi2" };
			File workingDirectoryFile = null;
			if (workingDirectory != null)
			{
				workingDirectoryFile = new File(workingDirectory);
			}
			m_process = Runtime.getRuntime().exec(commandLine, null, workingDirectoryFile);

			// Start listening for data
			GdbMiParser parser = new GdbMiParser();
			InputStream stream = m_process.getInputStream();
			byte[] buffer = new byte[4096];
			int bytes;
			while ((bytes = stream.read(buffer)) != -1)
			{
				// Process the data
				parser.process(buffer, bytes);

				// Handle the messages
				List<GdbMiMessage> messages = parser.getMessages();
				for (GdbMiMessage message : messages)
				{
					handleMessage(message);
				}
				messages.clear();
			}
		}
		catch (Throwable ex)
		{
			m_listener.onGdbError(ex);
		}
	}

	/**
	 * Handles the given GDB/MI message.
	 * @param message The message.
	 */
	private void handleMessage(GdbMiMessage message)
	{
		for (GdbMiRecord record : message.records)
		{
			// Handle the record
			switch (record.type)
			{
			case Target:
			case Console:
			case Log:
				handleStreamRecord((GdbMiStreamRecord) record);
				break;

			case Immediate:
			case Exec:
			case Notify:
			case Status:
				handleResultRecord((GdbMiResultRecord) record);
				break;
			}
		}

		// If this is the first message we have received we know we are fully started, so notify the
		// listener
		if (m_firstMessage)
		{
			m_firstMessage = false;
			m_listener.onGdbStarted();
		}
	}

	/**
	 * Handles the given GDB/MI stream record.
	 * @param record The record.
	 */
	private void handleStreamRecord(GdbMiStreamRecord record)
	{
		// Notify the listener
		m_listener.onStreamRecordReceived(record);
	}

	/**
	 * Handles the given GDB/MI result record.
	 * @param record The record.
	 */
	private void handleResultRecord(GdbMiResultRecord record)
	{
		// Notify the listener
		m_listener.onResultRecordReceived(record);

		// Find the pending command data
		PendingCommand pendingCommand = null;
		String commandType = null;
		if (record.userToken != null)
		{
			synchronized (m_pendingCommands)
			{
				pendingCommand = m_pendingCommands.remove(record.userToken);
			}
			if (pendingCommand != null)
			{
				commandType = pendingCommand.commandType;
			}
		}

		// Process the event into something more useful
		GdbEvent event = GdbMiMessageConverter.processRecord(record, commandType);
		if (event != null)
		{
			// Notify the listener
			m_listener.onGdbEventReceived(event);
			if (pendingCommand != null && pendingCommand.callback != null)
			{
				pendingCommand.callback.onGdbCommandCompleted(event);
			}
		}
	}
}
