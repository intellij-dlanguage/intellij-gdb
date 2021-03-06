package uk.co.cwspencer.ideagdb.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.DefaultProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.cwspencer.ideagdb.debug.GdbDebugProcess;

public class GdbRunner extends DefaultProgramRunner {

    @NotNull
    @Override
    public String getRunnerId() {
        return "GdbRunner";
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return DefaultDebugExecutor.EXECUTOR_ID.equals(executorId) &&
            profile instanceof GdbRunConfiguration;
    }

    @Override
    protected RunContentDescriptor doExecute(final RunProfileState state,
                                             final ExecutionEnvironment env)
        throws ExecutionException {
        FileDocumentManager.getInstance().saveAllDocuments();
        return createContentDescriptor(env.getProject(), env.getExecutor(), state, env);
    }

    @Nullable
    private RunContentDescriptor createContentDescriptor(final Project project,
                                                         final Executor executor,
                                                         final RunProfileState state,
                                                         final ExecutionEnvironment env) throws ExecutionException {
        final XDebugSession debugSession = XDebuggerManager
            .getInstance(project)
            .startSession(env, new XDebugProcessStarter() {
                @NotNull
                @Override
                public XDebugProcess start(@NotNull XDebugSession session) throws ExecutionException {
                    final ExecutionResult result = state.execute(executor, GdbRunner.this);
                    return new GdbDebugProcess(session, (GdbExecutionResult) result);
                }
            });
        return debugSession.getRunContentDescriptor();
    }
}
