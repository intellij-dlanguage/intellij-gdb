package uk.co.cwspencer.ideagdb.debug;

import com.intellij.util.PlatformIcons;
import com.intellij.xdebugger.frame.XValue;
import com.intellij.xdebugger.frame.XValueNode;
import com.intellij.xdebugger.frame.XValuePlace;
import org.jetbrains.annotations.NotNull;

/**
 * Class for providing information about a value from GDB.
 */
public class GdbValue extends XValue
{
	private String m_value;

	public GdbValue(String value)
	{
		m_value = value;
	}

	@Override
	public void computePresentation(@NotNull XValueNode node, @NotNull XValuePlace place)
	{
		node.setPresentation(PlatformIcons.VARIABLE_ICON, "SomeType", "'" + m_value + "'", false);
	}
}
