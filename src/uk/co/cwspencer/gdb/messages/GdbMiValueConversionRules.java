package uk.co.cwspencer.gdb.messages;

import com.intellij.openapi.diagnostic.Logger;
import uk.co.cwspencer.gdb.gdbmi.GdbMiResult;
import uk.co.cwspencer.gdb.gdbmi.GdbMiValue;
import uk.co.cwspencer.gdb.messages.annotations.GdbMiEnum;
import uk.co.cwspencer.gdb.messages.annotations.GdbMiObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Class containing rules for converting GDB/MI results to Java objects.
 * The rules are evaluated in the order they are declared in this class. Execution stops once on of
 * the rules returns a non-null value.
 */
@SuppressWarnings("unused")
public class GdbMiValueConversionRules
{
	private static final Logger m_log =
		Logger.getInstance("#uk.co.cwspencer.gdb.messages.GdbMiValueConversionRules");

	/**
	 * Converts results where the target type has a GdbMiObject annotation.
	 */
	public static Object convertValueToTypeWithGdbMiObjectAnnotation(Field field,
		GdbMiValue.Type valueType, GdbMiResult result)
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
					return null;
				}
				break;

			case String:
				m_log.warn("Field " + field + " has a type with a GdbMiObject annotation, " +
					"but expects a string from GDB; it must be a list or tuple");
				return null;
			}

			// Process the field
			return GdbMiMessageConverter.processObject(field.getType(), results);
		}
		return null;
	}

	/**
	 * Converts string results to enums.
	 */
	public static Object convertStringToEnum(Field field, GdbMiValue.Type valueType,
		GdbMiResult result)
	{
		// Check if the field type is an enum
		if (field.getType().isEnum())
		{
			// Only strings can be converted
			if (valueType != GdbMiValue.Type.String)
			{
				m_log.warn("Field " + field + " has an enum type but expects a " + valueType +
					"; only strings can be converted to enums");
				return null;
			}

			// Check the enum has a GdbMiEnum annotation
			GdbMiEnum enumAnnotation = field.getType().getAnnotation(GdbMiEnum.class);
			if (enumAnnotation == null)
			{
				m_log.warn("Field " + field + " has enum type " + field.getType() + ", but " +
					"the enum does not have a GdbMiEnum annotation");
				return null;
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
				return value;
			}
			else
			{
				m_log.warn("Could not find an appropriate enum value for string '" +
					result.value.string + "' for field " + field);
				return null;
			}
		}
		return null;
	}

	/**
	 * Converts strings to simple types.
	 */
	public static Object convertStringToSimple(Field field, GdbMiValue.Type valueType,
		GdbMiResult result)
	{
		Class<?> type = field.getType();
		if (valueType == GdbMiValue.Type.String)
		{
			if (type.equals(String.class))
			{
				return result.value.string;
			}
			if (type.equals(Integer.class))
			{
				return Integer.parseInt(result.value.string);
			}
		}
		return null;
	}
}
