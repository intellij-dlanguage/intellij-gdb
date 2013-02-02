package uk.co.cwspencer.ideagdb.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationWithSuppressedDefaultDebugAction;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class GdbRunConfiguration extends ModuleBasedConfiguration<GdbRunConfigurationModule>
	implements RunConfigurationWithSuppressedDefaultRunAction,
	RunConfigurationWithSuppressedDefaultDebugAction
{
	private static final Logger m_log =
		Logger.getInstance("#uk.co.cwspencer.ideagdb.run.GdbRunConfiguration");

	public GdbRunConfiguration(String name, Project project, ConfigurationFactory factory)
	{
		super(name, new GdbRunConfigurationModule(project), factory);
	}

	@Override
	public Collection<Module> getValidModules()
	{
		m_log.warn("getValidModules: stub");
		return null;
	}

	@Override
	protected ModuleBasedConfiguration createInstance()
	{
		return new GdbRunConfiguration(getName(), getProject(),
			GdbRunConfigurationType.getInstance().getFactory());
	}

	@Override
	public SettingsEditor<? extends RunConfiguration> getConfigurationEditor()
	{
		GdbRunConfigurationEditor<GdbRunConfiguration> editor =
			new GdbRunConfigurationEditor<GdbRunConfiguration>();
		return editor;
	}

	@Nullable
	@Override
	public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env)
		throws ExecutionException
	{
		return new GdbRunProfileState(env);
	}
}
