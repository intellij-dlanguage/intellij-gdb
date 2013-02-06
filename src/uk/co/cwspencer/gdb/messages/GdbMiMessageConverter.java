package uk.co.cwspencer.gdb.messages;

import com.intellij.openapi.diagnostic.Logger;
import uk.co.cwspencer.gdb.gdbmi.GdbMiResult;
import uk.co.cwspencer.gdb.gdbmi.GdbMiResultRecord;
import uk.co.cwspencer.gdb.gdbmi.GdbMiValue;
import uk.co.cwspencer.gdb.messages.annotations.GdbMiEvent;
import uk.co.cwspencer.gdb.messages.annotations.GdbMiField;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Class which handles the conversion of GDB/MI messages to Java objects.
 */
public class GdbMiMessageConverter
{
	private static final Logger m_log =
		Logger.getInstance("#uk.co.cwspencer.gdb.messages.GdbMiMessageConverter");

	/**
	 * Converts the given GDB/MI result record into a suitable Java object.
	 * @param record The GDB result record.
	 * @return The new object, or null if it could not be created.
	 */
	public static Object processRecord(GdbMiResultRecord record)
	{
		Object event = null;
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
				event = processObject(clazz, record.results);
				break;
			}
		}
		return event;
	}

	/**
	 * Converts the given list of GDB/MI results to an object of the given type.
	 * @param clazz The type of object to create.
	 * @param results The results from GDB.
	 * @return The new object, or null if it could not be created.
	 */
	public static Object processObject(Class<?> clazz, List<GdbMiResult> results)
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
	private static  void convertField(Object event, Class<?> clazz, Field field,
		GdbMiField fieldAnnotation, GdbMiResult result)
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
			convertFieldManually(event, clazz, field, fieldAnnotation.valueType(), result);
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
	private static  void convertFieldUsingValueProcessor(Object event, Class<?> clazz, Field field,
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
	 * @param valueType The expected GDB/MI value type for the field.
	 * @param result The result to get the data from.
	 */
	private static void convertFieldManually(Object event, Class<?> clazz, Field field,
		GdbMiValue.Type valueType, GdbMiResult result)
	{
		try
		{
			// Apply the conversion rules until we get a match
			Object value = null;
			Method[] methods = GdbMiValueConversionRules.class.getMethods();
			for (Method method : methods)
			{
				value = method.invoke(null, field, valueType, result);
				if (value != null)
				{
					break;
				}
			}

			// Save the value if we got one
			if (value != null)
			{
				field.set(event, value);
			}
			else
			{
				m_log.warn("No conversion rules were available to convert GDB/MI result '" +
					result + "' for field " + field);
			}
		}
		catch (Throwable ex)
		{
			m_log.warn("An error occurred whilst converting GDB/MI result '" + result + "' for " +
				"field " + field, ex);
		}
	}
}
