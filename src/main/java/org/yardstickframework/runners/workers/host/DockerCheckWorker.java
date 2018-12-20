package org.yardstickframework.runners.workers.host;

import java.io.IOException;
import java.util.Set;
import org.yardstickframework.runners.workers.CheckWorkResult;
import org.yardstickframework.runners.CommandExecutionResult;
import org.yardstickframework.runners.context.RunContext;

import org.yardstickframework.runners.workers.WorkResult;

/**
 * Checks docker on remote hosts.
 */
public class DockerCheckWorker extends CheckWorker {
    /** {@inheritDoc} */
    public DockerCheckWorker(RunContext runCtx, Set<String> hostSet) {
        super(runCtx, hostSet);
    }

    /** {@inheritDoc} */
    @Override public WorkResult doWork(String host, int cnt) {
        CheckWorkResult workRes = new CheckWorkResult();

        try {
            CommandExecutionResult res = runCtx.handler().runDockerCmd(host, "images");

            if(res.exitCode() != 0 || !res.errorList().isEmpty()){
                for(String err : res.errorList())
                    log().error(err);

                workRes.exit(true);
            }
        }
        catch (IOException | InterruptedException e) {
            log().error(String.format("Failed to check docker on the host '%s'", host), e);

            workRes.exit(true);
        }

        return workRes;
    }
}