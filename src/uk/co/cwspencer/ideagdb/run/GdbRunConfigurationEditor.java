package uk.co.cwspencer.ideagdb.run;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class GdbRunConfigurationEditor<T> extends SettingsEditor<T>
{
	private static final Logger m_log =
		Logger.getInstance("#uk.co.cwspencer.ideagdb.run.GdbRunConfigurationEditor");

	private JTextField m_gdbExecutable;
	private JPanel m_panel;

	@Override
	protected void resetEditorFrom(T s)
	{
		m_log.warn("resetEditorForm: stub");
	}

	@Override
	protected void applyEditorTo(T s) throws ConfigurationException
	{
		m_log.warn("applyEditorTo: stub");
	}

	@NotNull
	@Override
	protected JComponent createEditor()
	{
		return m_panel;
	}

	@Override
	protected void disposeEditor()
	{
		m_log.warn("disposeEditor: stub");
	}
}
