package uk.co.cwspencer.ideagdb.debug.gdb;

import uk.co.cwspencer.ideagdb.gdbmi.GdbMiMessage;
import uk.co.cwspencer.ideagdb.gdbmi.GdbMiParser;
import uk.co.cwspencer.ideagdb.gdbmi.GdbMiRecord;
import uk.co.cwspencer.ideagdb.gdbmi.GdbMiResultRecord;
import uk.co.cwspencer.ideagdb.gdbmi.GdbMiStreamRecord;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Class for interacting with GDB.
 */
public class Gdb
{
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

	/**
	 * Constructor; launches GDB.
	 * @param gdbPath The path to the GDB executable.
	 * @param listener Listener that is to receive GDB events.
	 */
	public Gdb(final String gdbPath, GdbListener listener)
	{
		m_listener = listener;
		m_readThread = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					runGdb(gdbPath);
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
		long token = m_token++;

		StringBuilder sb = new StringBuilder();
		sb.append(token);
		sb.append(command);
		sb.append("\r\n");

		byte[] message = sb.toString().getBytes(m_ascii);
		m_process.getOutputStream().write(message);
		m_process.getOutputStream().flush();

		return token;
	}

	/**
	 * Launches the GDB process and starts listening for data.
	 * @param gdbPath Path to the GDB executable.
	 */
	private void runGdb(String gdbPath)
	{
		try
		{
			// Launch the process
			final String[] commandLine = {
				gdbPath,
				"--interpreter=mi2" };
			m_process = Runtime.getRuntime().exec(commandLine);

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
			// Notify the listener
			switch (record.type)
			{
			case Target:
			case Console:
			case Log:
				m_listener.onStreamRecordReceived((GdbMiStreamRecord) record);
				break;

			case Immediate:
			case Exec:
			case Notify:
			case Status:
				m_listener.onResultRecordReceived((GdbMiResultRecord) record);
				break;
			}
		}

		if (m_firstMessage)
		{
			m_firstMessage = false;
			m_listener.onGdbStarted();
		}
	}
}
