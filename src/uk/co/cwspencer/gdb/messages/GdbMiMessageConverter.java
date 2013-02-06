package uk.co.cwspencer.gdb.messages;

import com.intellij.openapi.diagnostic.Logger;
import uk.co.cwspencer.gdb.gdbmi.GdbMiResult;
import uk.co.cwspencer.gdb.gdbmi.GdbMiResultRecord;
import uk.co.cwspencer.gdb.gdbmi.GdbMiValue;
import uk.co.cwspencer.gdb.messages.annotations.GdbMiConversionRule;
import uk.co.cwspencer.gdb.messages.annotations.GdbMiEvent;
import uk.co.cwspencer.gdb.messages.annotations.GdbMiField;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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

			if (eventAnnotation.recordType() == record.type)
			{
				boolean match = false;
				for (String className : eventAnnotation.className())
				{
					if (className.equals(record.className))
					{
						match = true;
						break;
					}
				}

				if (match)
				{
					// Found a matching event type wrapper
					event = processObject(clazz, record.results);
					break;
				}

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
		try
		{
			Object event = clazz.newInstance();

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
		catch (Throwable ex)
		{
			m_log.warn("Failed to convert GDB/MI message to a Java object", ex);
			return null;
		}
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
	private static void convertField(Object event, Class<?> clazz, Field field,
		GdbMiField fieldAnnotation, GdbMiResult result) throws InvocationTargetException,
		IllegalAccessException
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
			ParameterizedType genericType = null;
			{
				Type basicGenericType = field.getGenericType();
				if (basicGenericType instanceof ParameterizedType)
				{
					genericType = (ParameterizedType) basicGenericType;
				}
			}

			Object value = convertFieldManually(field.getType(), genericType, result);
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
			String valueProcessorName = fieldAnnotation.valueProcessor();
			int lastDotIndex = valueProcessorName.lastIndexOf('.');
			if (lastDotIndex == -1)
			{
				// Value processor is a function on the parent class
				valueProcessor = clazz.getMethod(valueProcessorName, String.class);
			}
			else
			{
				// Value processor is a fully-qualified name
				String className = valueProcessorName.substring(0, lastDotIndex);
				String methodName = valueProcessorName.substring(lastDotIndex + 1);

				Class<?> valueProcessorClass = Class.forName(className);
				valueProcessor = valueProcessorClass.getMethod(methodName, String.class);
			}
		}
		catch (NoSuchMethodException ex)
		{
			m_log.warn("Annotation on " + field.getName() + " has value processor " +
				fieldAnnotation.valueProcessor() + ", but no such function exists on the class " +
				"(or it does not take the right arguments)", ex);
			return;
		}
		catch (ClassNotFoundException ex)
		{
			m_log.warn("Annotation on " + field.getName() + " has value processor " +
				fieldAnnotation.valueProcessor() + ", but the referenced class does not exist", ex);
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
	 * @param targetType The type of object to be created.
	 * @param genericTargetType The generic type of the object to be created. May be null.
	 * @param result The result to get the data from.
	 * @return The new object, or null if it could not be created.
	 */
	static Object convertFieldManually(Class<?> targetType, ParameterizedType genericTargetType,
		GdbMiResult result) throws InvocationTargetException, IllegalAccessException
	{
		// Apply the conversion rules until we get a match
		Object value = null;
		Method[] methods = GdbMiValueConversionRules.class.getMethods();
		for (Method method : methods)
		{
			// Verify it is a conversion rule
			if (method.getAnnotation(GdbMiConversionRule.class) == null)
			{
				continue;
			}

			value = method.invoke(null, targetType, genericTargetType, result);
			if (value != null)
			{
				break;
			}
		}
		return value;
	}
}
