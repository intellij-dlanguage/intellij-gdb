package uk.co.cwspencer.ideagdb.debug.gdb;

import com.intellij.openapi.diagnostic.Logger;
import uk.co.cwspencer.ideagdb.debug.gdb.messages.GdbEvent;
import uk.co.cwspencer.ideagdb.debug.gdb.messages.GdbMiEventTypes;
import uk.co.cwspencer.ideagdb.debug.gdb.messages.annotations.GdbMiEvent;
import uk.co.cwspencer.ideagdb.debug.gdb.messages.annotations.GdbMiField;
import uk.co.cwspencer.ideagdb.debug.gdb.messages.annotations.GdbMiObject;
import uk.co.cwspencer.ideagdb.gdbmi.GdbMiList;
import uk.co.cwspencer.ideagdb.gdbmi.GdbMiMessage;
import uk.co.cwspencer.ideagdb.gdbmi.GdbMiParser;
import uk.co.cwspencer.ideagdb.gdbmi.GdbMiRecord;
import uk.co.cwspencer.ideagdb.gdbmi.GdbMiResult;
import uk.co.cwspencer.ideagdb.gdbmi.GdbMiResultRecord;
import uk.co.cwspencer.ideagdb.gdbmi.GdbMiStreamRecord;
import uk.co.cwspencer.ideagdb.gdbmi.GdbMiValue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for interacting with GDB.
 */
public class Gdb
{
	private static final Logger m_log =
		Logger.getInstance("#uk.co.cwspencer.ideagdb.debug.gdb.Gdb");

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

		// Process the event into something more useful
		for (Class<?> clazz : GdbMiEventTypes.classes)
		{
			GdbMiEvent eventAnnotation = clazz.getAnnotation(GdbMiEvent.class);
			if (eventAnnotation == null)
			{
				m_log.warn("Class " + clazz.getName() + " is in GdbMiEventTypes list but does " +
					"not have GdbMiEvent annotation");
				continue;
			}

			if (eventAnnotation.recordType() == record.type &&
				eventAnnotation.className().equals(record.className))
			{
				// Found a matching event type wrapper
				Object event = processObject(clazz, record.results);

				// Notify the listener
				m_listener.onGdbEventReceived((GdbEvent) event);
			}
		}
	}

	private Object processObject(Class<?> clazz, List<GdbMiResult> results)
	{
		Object event;
		try
		{
			event = clazz.newInstance();
		}
		catch (InstantiationException ex)
		{
			m_log.warn("Failed to instantiate event class " + clazz.getName(), ex);
			return null;
		}
		catch (IllegalAccessException ex)
		{
			m_log.warn("Failed to instantiate event class " + clazz.getName(), ex);
			return null;
		}

		// Populate the fields with data from the result
		Field[] fields = clazz.getFields();
		for (Field field : fields)
		{
			GdbMiField fieldAnnotation = field.getAnnotation(GdbMiField.class);
			if (fieldAnnotation == null)
			{
				continue;
			}

			// Find a result with the requested variable name
			for (GdbMiResult result : results)
			{
				if (!fieldAnnotation.name().equals(result.variable))
				{
					continue;
				}

				// Found a matching field; convert the value
				convertField(event, clazz, field, fieldAnnotation, result);
				break;
			}
		}

		return event;
	}

	/**
	 * Converts a GdbMiResult into a suitable Java type and puts it in the given field on the given
	 * object.
	 * @param event The object to put the value into.
	 * @param clazz The class of the object.
	 * @param field The field on the object to put the value into.
	 * @param fieldAnnotation The GdbMiField annotation on the field.
	 * @param result The result to get the data from.
	 */
	private void convertField(Object event, Class<?> clazz, Field field, GdbMiField fieldAnnotation,
		GdbMiResult result)
	{

		if (fieldAnnotation.valueType() != result.value.type)
		{
			m_log.warn("Annotation on " + field.getName() + " requires GDB/MI type " +
				fieldAnnotation.valueType() + "; got " + result.value.type);
			return;
		}

		if (!fieldAnnotation.valueProcessor().isEmpty())
		{
			// Field has a custom value processor
			convertFieldUsingValueProcessor(event, clazz, field, fieldAnnotation, result);
		}
		else
		{
			// Field does not have a custom value processor; convert it manually
			convertFieldManually(event, clazz, field, fieldAnnotation, result);
		}
	}

	/**
	 * Converts a GdbMiResult into a suitable Java type and puts it in the given field on the given
	 * object using a custom value processor defined by the field.
	 * @param event The object to put the value into.
	 * @param clazz The class of the object.
	 * @param field The field on the object to put the value into.
	 * @param fieldAnnotation The GdbMiField annotation on the field.
	 * @param result The result to get the data from.
	 */
	private void convertFieldUsingValueProcessor(Object event, Class<?> clazz, Field field,
		GdbMiField fieldAnnotation, GdbMiResult result)
	{
		// Get the value processor function
		Method valueProcessor;
		try
		{
			// TODO: Correct parameter type
			valueProcessor = clazz.getMethod(
				fieldAnnotation.valueProcessor(), String.class);
		}
		catch (NoSuchMethodException ex)
		{
			m_log.warn("Annotation on " + field.getName() + " has value processor " +
				fieldAnnotation.valueProcessor() + ", but no such function exists on the class " +
				"(or it does not take the right arguments)", ex);
			return;
		}

		// Invoke the method
		Object resultValue = null;
		Object value = null;
		try
		{
			switch (result.value.type)
			{
			case String:
				resultValue = result.value.string;
				value = valueProcessor.invoke(event, result.value.string);
				break;

			case Tuple:
				resultValue = result.value.tuple;
				value = valueProcessor.invoke(event, result.value.tuple);
				break;

			case List:
				resultValue = result.value.list;
				value = valueProcessor.invoke(event, result.value.list);
				break;
			}
		}
		catch (Throwable ex)
		{
			m_log.warn("Field to invoke value processor for field " + field.getName() + " with " +
				"value " + resultValue, ex);
			return;
		}

		// We don't need to do anything if the value processor returned null
		if (value == null)
		{
			return;
		}

		// Check the returned value is of the correct type
		if (!field.getType().isAssignableFrom(value.getClass()))
		{
			m_log.warn("Field " + field.getName() + " is of type " + field.getType() + ", but " +
				"the value processor returned " + value + " [type=" + value.getClass() + "]");
			return;
		}

		// Set the value on the field
		try
		{
			field.set(event, value);
		}
		catch (IllegalAccessException ex)
		{
			m_log.warn("Failed to set value on field " + field, ex);
			return;
		}
	}

	/**
	 * Converts a GdbMiResult into a suitable Java type and puts it in the given field on the given
	 * object using built-in value processors.
	 * @param event The object to put the value into.
	 * @param clazz The class of the object.
	 * @param field The field on the object to put the value into.
	 * @param fieldAnnotation The GdbMiField annotation on the field.
	 * @param result The result to get the data from.
	 */
	private void convertFieldManually(Object event, Class<?> clazz, Field field,
		GdbMiField fieldAnnotation, GdbMiResult result)
	{
		// If the field type class has a GdbMiObject annotation then recursively process it
		GdbMiObject objectAnnotation = field.getType().getAnnotation(GdbMiObject.class);
		if (objectAnnotation != null)
		{
			// Get the list of results
			List<GdbMiResult> results = null;
			switch (fieldAnnotation.valueType())
			{
			case Tuple:
				results = result.value.tuple;
				break;

			case List:
				switch (result.value.list.type)
				{
				case Results:
					results = result.value.list.results;
					break;

				case Empty:
					results = new ArrayList<GdbMiResult>(0);
					break;

				case Values:
					m_log.warn("Field " + field + " has a type with a GdbMiObject annotation and " +
						"expects a list, but GDB returned a list of values rather than a list of " +
						"results, so it cannot be processed");
					return;
				}
				break;

			case String:
				m_log.warn("Field " + field + " has a type with a GdbMiObject annotation, but " +
					"expects a string from GDB; it must be a list or tuple");
				return;
			}

			// Process the field
			try
			{
				field.set(event, processObject(field.getType(), results));
			}
			catch (IllegalAccessException ex)
			{
				m_log.warn("Failed to set value on field " + field, ex);
			}
			return;
		}

		// Determine how to convert the value based on the value type and the field type
		try
		{
			GdbMiValue.Type valueType = fieldAnnotation.valueType();
			Class<?> fieldType = field.getType();

			Object value = null;
			switch (valueType)
			{
			case String:
				if (fieldType.equals(String.class))
				{
					value = result.value.string;
				}
				else if (fieldType.equals(Integer.class))
				{
					value = Integer.parseInt(result.value.string);
				}
				break;
			}

			// Save the value if we got one
			if (value != null)
			{
				field.set(event, value);
			}
			else
			{
				m_log.warn("No built-in method available to convert value for field " + field +
					" from " + valueType + " to " + fieldType.getName() + " and the field type " +
					"does not have a GdbMiObject annotation");
			}
		}
		catch (IllegalAccessException ex)
		{
			m_log.warn("Failed to set value on field " + field, ex);
		}
		catch (NumberFormatException ex)
		{
			m_log.warn("Failed to convert String '" + result.value.string + "' to " +
				"Integer for field " + field, ex);
		}
	}
}
