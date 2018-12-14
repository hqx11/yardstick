package org.yardstickframework.runners.starters;

import java.io.File;
import java.io.IOException;
import org.yardstickframework.runners.AbstractRunner;
import org.yardstickframework.runners.CommandHandler;
import org.yardstickframework.runners.context.NodeInfo;
import org.yardstickframework.runners.context.NodeType;
import org.yardstickframework.runners.context.DockerInfo;
import org.yardstickframework.runners.context.RunContext;

public class InDockerNodeStarter extends AbstractRunner implements NodeStarter {

    public InDockerNodeStarter(RunContext runCtx) {
        super(runCtx);
    }

    @Override public NodeInfo startNode(NodeInfo nodeInfo) throws InterruptedException {
        String contName = String.format("YARDSTICK_%s_%s", nodeInfo.nodeType(), nodeInfo.id());

        String nodeLogDir = new File(nodeInfo.logPath()).getParent();

        CommandHandler hndl = new CommandHandler(runCtx);

        String javaParams = nodeInfo.parameterString();

        NodeType type = nodeInfo.nodeType();

        String javaHome = type == NodeType.SERVER ?
            runCtx.dockerContext().getServerDockerJavaHome() :
            runCtx.dockerContext().getDriverDockerJavaHome() ;


        try {
            String mkdirCmd = String.format("exec %s mkdir -p %s", contName, nodeLogDir);

            String startNodeCmd = String.format("%s/bin/java %s", javaHome, javaParams);

            String cmd = String.format("exec %s nohup %s > %s 2>& 1 &", contName, startNodeCmd, nodeInfo.logPath());

            hndl.runDockerCmd(nodeInfo.host(), mkdirCmd);

            hndl.runDockerCmd(nodeInfo.host(), cmd);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        nodeInfo.dockerInfo(new DockerInfo(null, contName));

        return nodeInfo;
    }
}
