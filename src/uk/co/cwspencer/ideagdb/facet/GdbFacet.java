package uk.co.cwspencer.ideagdb.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetTypeId;
import com.intellij.facet.FacetTypeRegistry;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GdbFacet extends Facet<GdbFacetConfiguration>
{
	public static final FacetTypeId<GdbFacet> ID = new FacetTypeId<GdbFacet>("gdb");

	public GdbFacet(@NotNull Module module, @NotNull String name,
					@NotNull GdbFacetConfiguration configuration)
	{
		super(getFacetType(), module, name, configuration, null);
	}

	public static GdbFacetType getFacetType()
	{
		return (GdbFacetType) FacetTypeRegistry.getInstance().findFacetType(ID);
	}

	@Nullable
	public static GdbFacet getInstance(@NotNull Module module)
	{
		return FacetManager.getInstance(module).getFacetByType(ID);
	}
}
