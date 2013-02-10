package uk.co.cwspencer.ideagdb.debug;

import com.intellij.util.PlatformIcons;
import com.intellij.xdebugger.frame.XValue;
import com.intellij.xdebugger.frame.XValueNode;
import com.intellij.xdebugger.frame.XValuePlace;
import org.jetbrains.annotations.NotNull;
import uk.co.cwspencer.gdb.messages.GdbVariableObject;

/**
 * Class for providing information about a value from GDB.
 */
public class GdbValue extends XValue
{
	private GdbVariableObject m_variableObject;

	public GdbValue(GdbVariableObject variableObject)
	{
		m_variableObject = variableObject;
	}

	@Override
	public void computePresentation(@NotNull XValueNode node, @NotNull XValuePlace place)
	{
		node.setPresentation(PlatformIcons.VARIABLE_ICON, m_variableObject.type,
			m_variableObject.value, false);
	}
}
