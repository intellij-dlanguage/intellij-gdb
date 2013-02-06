package uk.co.cwspencer.gdb.messages;

import com.intellij.openapi.diagnostic.Logger;
import uk.co.cwspencer.gdb.gdbmi.GdbMiList;
import uk.co.cwspencer.gdb.gdbmi.GdbMiResult;
import uk.co.cwspencer.gdb.gdbmi.GdbMiValue;
import uk.co.cwspencer.gdb.messages.annotations.GdbMiConversionRule;
import uk.co.cwspencer.gdb.messages.annotations.GdbMiEnum;
import uk.co.cwspencer.gdb.messages.annotations.GdbMiObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class containing rules for converting GDB/MI results to Java objects.
 */
@SuppressWarnings("unused")
public class GdbMiValueConversionRules
{
	private static final Logger m_log =
		Logger.getInstance("#uk.co.cwspencer.gdb.messages.GdbMiValueConversionRules");

	/**
	 * Converts results where the target type has a GdbMiObject annotation.
	 */
	@GdbMiConversionRule
	public static Object convertValueToTypeWithGdbMiObjectAnnotation(Class<?> type,
		ParameterizedType genericType, GdbMiResult result)
	{
		// If the field type class has a GdbMiObject annotation then recursively process it
		GdbMiObject objectAnnotation = type.getAnnotation(GdbMiObject.class);
		if (objectAnnotation != null)
		{
			// Get the list of results
			List<GdbMiResult> results = null;
			switch (result.value.type)
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
					m_log.warn(type + " has a GdbMiObject annotation and expects a list, but GDB " +
						"returned a list of values rather than a list of results, so it cannot " +
						"be processed");
					return null;
				}
				break;

			case String:
				m_log.warn(type + " has a GdbMiObject annotation, but a string is expected from " +
					"GDB; it must be a list or tuple");
				return null;
			}

			// Process the field
			return GdbMiMessageConverter.processObject(type, results);
		}
		return null;
	}

	/**
	 * Converts string results to enums.
	 */
	@GdbMiConversionRule
	public static Object convertStringToEnum(Class<?> type, ParameterizedType genericType,
		GdbMiResult result)
	{
		// Check if the field type is an enum
		if (type.isEnum())
		{
			// Only strings can be converted
			if (result.value.type != GdbMiValue.Type.String)
			{
				m_log.warn(type + " is an enum, but expects a " + result.value.type + "; only " +
					"strings can be converted to enums");
				return null;
			}

			// Check the enum has a GdbMiEnum annotation
			GdbMiEnum enumAnnotation = type.getAnnotation(GdbMiEnum.class);
			if (enumAnnotation == null)
			{
				m_log.warn(type + " is an enum, but the does not have a GdbMiEnum annotation");
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
			Enum[] enumConstants = (Enum[]) type.getEnumConstants();
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
					result.value.string + "' for type " + type);
				return null;
			}
		}
		return null;
	}

	/**
	 * Converts strings to simple types.
	 */
	@GdbMiConversionRule
	public static Object convertStringToSimple(Class<?> type, ParameterizedType genericType,
		GdbMiResult result)
	{
		if (result.value.type == GdbMiValue.Type.String)
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

	/**
	 * Converts list of tuples to a map. The tuples must all have two elements each. The variable
	 * names of the tuples are discarded.
	 */
	@GdbMiConversionRule
	public static Object convertListOfTuplesToMap(Class<?> type, ParameterizedType genericType,
		GdbMiResult result) throws InvocationTargetException,
		IllegalAccessException
	{
		if (result.value.type != GdbMiValue.Type.List || genericType == null ||
			!type.equals(Map.class))
		{
			return null;
		}

		if (result.value.list.type == GdbMiList.Type.Results)
		{
			// Only value lists can be converted
			return null;
		}

		Map map = new LinkedHashMap();
		if (result.value.list.type == GdbMiList.Type.Empty)
		{
			return map;
		}

		// Get the key and value types from the map
		Type[] mapTypes = genericType.getActualTypeArguments();
		assert mapTypes.length == 2;
		if (!(mapTypes[0] instanceof Class) || !(mapTypes[1] instanceof Class))
		{
			return null;
		}

		Class<?> mapKeyType = (Class<?>) mapTypes[0];
		Class<?> mapValueType = (Class<?>) mapTypes[1];

		// Process each value in the list
		for (GdbMiValue value : result.value.list.values)
		{
			// Verify the value is a tuple and contains two elements
			if (value.type != GdbMiValue.Type.Tuple || value.tuple.size() != 2)
			{
				return null;
			}

			Object key = GdbMiMessageConverter.convertFieldManually(mapKeyType, null,
				value.tuple.get(0));
			if (key == null)
			{
				return null;
			}

			Object newValue = GdbMiMessageConverter.convertFieldManually(mapValueType, null,
				value.tuple.get(1));
			if (newValue == null)
			{
				return null;
			}

			map.put(key, newValue);
		}
		return map;
	}
}
