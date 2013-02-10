package uk.co.cwspencer.ideagdb.debug;

import com.intellij.util.PlatformIcons;
import com.intellij.xdebugger.frame.XValue;
import com.intellij.xdebugger.frame.XValueModifier;
import com.intellij.xdebugger.frame.XValueNode;
import com.intellij.xdebugger.frame.XValuePlace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.cwspencer.gdb.Gdb;
import uk.co.cwspencer.gdb.messages.GdbVariableObject;

/**
 * Class for providing information about a value from GDB.
 */
public class GdbValue extends XValue
{
	// The GDB instance
	private Gdb m_gdb;

	// The variable object we are showing the value of
	private GdbVariableObject m_variableObject;

	/**
	 * Constructor.
	 * @param gdb Handle to the GDB instance.
	 * @param variableObject The variable object to show the value of.
	 */
	public GdbValue(Gdb gdb, GdbVariableObject variableObject)
	{
		m_gdb = gdb;
		m_variableObject = variableObject;
	}

	/**
	 * Computes the presentation for the variable.
	 * @param node The node to display the value in.
	 * @param place Where the node will be shown.
	 */
	@Override
	public void computePresentation(@NotNull XValueNode node, @NotNull XValuePlace place)
	{
		node.setPresentation(PlatformIcons.VARIABLE_ICON, m_variableObject.type,
			m_variableObject.value, false);
	}

	/**
	 * Returns a modifier which can be used to change the value.
	 * @return The modifier, or null if the value cannot be modified.
	 */
	@Nullable
	@Override
	public XValueModifier getModifier()
	{
		// TODO: Return null if we don't support editing
		return new GdbValueModifier(m_gdb, m_variableObject);
	}
}
