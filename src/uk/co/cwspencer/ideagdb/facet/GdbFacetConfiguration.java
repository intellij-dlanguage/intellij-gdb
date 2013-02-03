package uk.co.cwspencer.ideagdb.facet;

import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;

public class GdbFacetConfiguration implements FacetConfiguration
{
	private static final Logger m_log =
		Logger.getInstance("#uk.co.cwspencer.ideagdb.facet.GdbFacetConfiguration");

	public String GDB_PATH = "gdb";
	public String APP_PATH = "";
	public String STARTUP_COMMANDS = "";

	@Override
	public FacetEditorTab[] createEditorTabs(FacetEditorContext editorContext, FacetValidatorsManager validatorsManager)
	{
		return new FacetEditorTab[] { new GdbFacetEditorTab(editorContext, this) };
	}

	@Override
	@Deprecated
	public void readExternal(Element element) throws InvalidDataException
	{
		XmlSerializer.deserializeInto(this, element);
	}

	@Override
	@Deprecated
	public void writeExternal(Element element) throws WriteExternalException
	{
		XmlSerializer.serializeInto(this, element, new SkipDefaultValuesSerializationFilters());
	}
}
