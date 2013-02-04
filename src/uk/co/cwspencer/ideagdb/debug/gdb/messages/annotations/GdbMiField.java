package uk.co.cwspencer.ideagdb.debug.gdb.messages.annotations;

import uk.co.cwspencer.ideagdb.gdbmi.GdbMiValue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation applied to classes which represent fields in GDB/MI messages.
 */
@Retention(value = RetentionPolicy.RUNTIME)
public @interface GdbMiField
{
	/**
	 * The name of the field.
	 */
	String name();

	/**
	 * The type values take.
	 */
	GdbMiValue.Type valueType();

	/**
	 * Name of the function in the class to use to convert the value from the GDB format to the
	 * variable format.
	 */
	String valueProcessor() default "";
}
