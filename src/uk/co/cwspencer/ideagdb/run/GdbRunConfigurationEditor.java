package uk.co.cwspencer.ideagdb.run;

import com.intellij.execution.ui.ConfigurationModuleSelector;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import uk.co.cwspencer.ideagdb.facet.GdbFacet;

import javax.swing.*;

public class GdbRunConfigurationEditor<T extends GdbRunConfiguration>
	extends SettingsEditor<T>
{
	private static final Logger m_log =
		Logger.getInstance("#uk.co.cwspencer.ideagdb.run.GdbRunConfigurationEditor");

	private JPanel m_panel;
	private JComboBox m_modulesComboBox;

	private final ConfigurationModuleSelector m_moduleSelector;

	public GdbRunConfigurationEditor(final Project project)
	{
		m_moduleSelector = new ConfigurationModuleSelector(project, m_modulesComboBox)
			{
				@Override
				public boolean isModuleAccepted(Module module)
				{
					if (module == null || !super.isModuleAccepted(module))
					{
						return false;
					}
					final GdbFacet facet = GdbFacet.getInstance(module);
					return facet != null;
				}
			};
	}

	@Override
	protected void resetEditorFrom(T configuration)
	{
		m_moduleSelector.reset(configuration);
	}

	@Override
	protected void applyEditorTo(T configuration) throws ConfigurationException
	{
		m_moduleSelector.applyTo(configuration);
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
	}
}
