package uk.co.cwspencer.ideagdb.debug;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import org.jetbrains.annotations.NotNull;

public class GdbDebuggerEditorsProvider extends XDebuggerEditorsProvider
{
	private static final Logger m_log =
		Logger.getInstance("#uk.co.cwspencer.ideagdb.debug.GdbDebuggerEditorsProvider");

	@NotNull
	@Override
	public FileType getFileType()
	{
		m_log.warn("getFileType: stub");
		return null;
	}
}
