package uk.co.cwspencer.ideagdb.facet;

import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class GdbFacetEditorTab extends FacetEditorTab
{
	private static final Logger m_log =
		Logger.getInstance("#uk.co.cwspencer.ideagdb.facet.GdbFacetEditorTab");

	private TextFieldWithBrowseButton m_gdbPath;
	private JTextArea m_startupCommands;
	private JPanel m_contentPanel;

	private final GdbFacetConfiguration m_configuration;

	public GdbFacetEditorTab(FacetEditorContext context,
		GdbFacetConfiguration gdbFacetConfiguration)
	{
		m_configuration = gdbFacetConfiguration;
	}

	@Nls
	@Override
	public String getDisplayName()
	{
		m_log.warn("getDisplayName: stub");
		return null;
	}

	@Nullable
	@Override
	public JComponent createComponent()
	{
		return m_contentPanel;
	}

	@Override
	public boolean isModified()
	{
		if (!m_configuration.GDB_PATH.equals(m_gdbPath.getText()))
		{
			return true;
		}
		if (!m_configuration.STARTUP_COMMANDS.equals(m_startupCommands.getText()))
		{
			return true;
		}
		return false;
	}

	@Override
	public void apply() throws ConfigurationException
	{
		if (!isModified())
		{
			return;
		}

		m_configuration.GDB_PATH = m_gdbPath.getText();
		m_configuration.STARTUP_COMMANDS = m_startupCommands.getText();
	}

	@Override
	public void reset()
	{
		final String gdbPath = m_configuration.GDB_PATH;
		m_gdbPath.setText(gdbPath != null ? gdbPath : "");

		final String startupCommands = m_configuration.STARTUP_COMMANDS;
		m_startupCommands.setText(startupCommands != null ? startupCommands : "");
	}

	@Override
	public void disposeUIResources()
	{
	}
}
