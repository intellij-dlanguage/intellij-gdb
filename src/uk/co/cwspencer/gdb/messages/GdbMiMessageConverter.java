package uk.co.cwspencer.gdb.messages;

import com.intellij.openapi.diagnostic.Logger;
import uk.co.cwspencer.gdb.gdbmi.GdbMiResult;
import uk.co.cwspencer.gdb.gdbmi.GdbMiResultRecord;
import uk.co.cwspencer.gdb.gdbmi.GdbMiValue;
import uk.co.cwspencer.gdb.messages.annotations.GdbMiEnum;
import uk.co.cwspencer.gdb.messages.annotations.GdbMiEvent;
import uk.co.cwspencer.gdb.messages.annotations.GdbMiField;
import uk.co.cwspencer.gdb.messages.annotations.GdbMiObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
	private static Object processObject(Class<?> clazz, List<GdbMiResult> results)
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
			// If the field type class has a GdbMiObject annotation then recursively process it
			GdbMiObject objectAnnotation = field.getType().getAnnotation(GdbMiObject.class);
			if (objectAnnotation != null)
			{
				// Get the list of results
				List<GdbMiResult> results = null;
				switch (valueType)
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
						m_log.warn("Field " + field + " has a type with a GdbMiObject annotation " +
							"and expects a list, but GDB returned a list of values rather than a " +
							"list of results, so it cannot be processed");
						return;
					}
					break;

				case String:
					m_log.warn("Field " + field + " has a type with a GdbMiObject annotation, " +
						"but expects a string from GDB; it must be a list or tuple");
					return;
				}

				// Process the field
				field.set(event, processObject(field.getType(), results));
				return;
			}

			// Check if the field type is an enum
			if (field.getType().isEnum())
			{
				// Only strings can be converted
				if (valueType != GdbMiValue.Type.String)
				{
					m_log.warn("Field " + field + " has an enum type but expects a " + valueType +
						"; only strings can be converted to enums");
					return;
				}

				// Check the enum has a GdbMiEnum annotation
				GdbMiEnum enumAnnotation = field.getType().getAnnotation(GdbMiEnum.class);
				if (enumAnnotation == null)
				{
					m_log.warn("Field " + field + " has enum type " + field.getType() + ", but " +
						"the enum does not have a GdbMiEnum annotation");
					return;
				}

				// Convert the GDB/MI string into an enum value name. Hyphens are stripped and the
				// first letter of each word is capitalised
				boolean capitalise = true;
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i != result.value.string.length(); ++i)
				{
					char ch = result.value.string.charAt(i);
					if (ch == '-')
					{
						capitalise = true;
						continue;
					}
					if (capitalise)
					{
						capitalise = false;
						ch = Character.toUpperCase(ch);
					}
					sb.append(ch);
				}
				String name = sb.toString();

				// Search the enum
				Enum value = null;
				Enum[] enumConstants = (Enum[]) field.getType().getEnumConstants();
				for (Enum enumValue : enumConstants)
				{
					if (enumValue.name().equals(name))
					{
						// Found it
						value = enumValue;
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
					m_log.warn("Could not find an appropriate enum value for string '" +
						result.value.string + "' for field " + field);
				}
				return;
			}

			// Determine how to convert the value based on the value type and the field type
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
