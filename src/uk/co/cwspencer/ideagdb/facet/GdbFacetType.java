package uk.co.cwspencer.ideagdb.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class GdbFacetType extends FacetType<GdbFacet, GdbFacetConfiguration>
{
	private static final Logger m_log =
		Logger.getInstance("#uk.co.cwspencer.ideagdb.facet.GdbFacetType");

	public GdbFacetType()
	{
		super(GdbFacet.ID, "gdb", "GDB");
	}

	@Override
	public GdbFacetConfiguration createDefaultConfiguration()
	{
		return new GdbFacetConfiguration();
	}

	@Override
	public GdbFacet createFacet(@NotNull Module module, String name,
		@NotNull GdbFacetConfiguration configuration, @Nullable Facet underlyingFacet)
	{
		return new GdbFacet(module, name, configuration);
	}

	@Override
	public boolean isSuitableModuleType(ModuleType moduleType)
	{
		return true;
	}

	@Nullable
	@Override
	public Icon getIcon()
	{
		return AllIcons.RunConfigurations.Application;
	}
}
