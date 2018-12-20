package org.yardstickframework.runners.workers.node;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.yardstickframework.runners.CommandExecutionResult;
import org.yardstickframework.runners.context.DockerInfo;
import org.yardstickframework.runners.DockerRunner;
import org.yardstickframework.runners.context.NodeInfo;
import org.yardstickframework.runners.context.NodeType;
import org.yardstickframework.runners.context.DockerContext;
import org.yardstickframework.runners.context.RunContext;
import org.yardstickframework.runners.context.RunMode;

/**
 * Starts docker containers.
 */
public class DockerStartContWorker extends NodeWorker {
    /** */
    private static final String DFLT_RUN_CMD =
        "run -d --network host --name CONT_NAME_PLACEHOLDER IMAGE_NAME_PLACEHOLDER sleep 1d";

    /** */
    private static final String CONT_NAME_PLACEHOLDER = "CONT_NAME_PLACEHOLDER";

    /** */
    private static final String IMAGE_NAME_PLACEHOLDER = "IMAGE_NAME_PLACEHOLDER";

    /** */
    private DockerContext dockerCtx;

    /** {@inheritDoc} */
    public DockerStartContWorker(RunContext runCtx, List<NodeInfo> nodeList) {
        super(runCtx, nodeList);

        dockerCtx = runCtx.dockerContext();
    }

    /** {@inheritDoc} */
    @Override public NodeInfo doWork(NodeInfo nodeInfo) throws InterruptedException {
        String host = nodeInfo.host();

        String id = nodeInfo.id();

        NodeType type = nodeInfo.nodeType();

        String imageName = dockerCtx.getImageName(type);

        String contName = String.format("YARDSTICK_%s_%s", type, id);

        DockerInfo docInfo = new DockerInfo(imageName, contName);

        nodeInfo.dockerInfo(docInfo);

        log().info(String.format("Starting the container '%s' on the host '%s'", contName, host));

        String startContCmd = getStartContCmd(imageName, contName);

        try {
            runCtx.handler().runDockerCmd(host, startContCmd);

            String mkdirCmd = String.format("exec %s mkdir -p %s", contName, runCtx.remoteWorkDirectory());

            runCtx.handler().runDockerCmd(host, mkdirCmd);

            String remPath = runCtx.remoteWorkDirectory();

            String parentPath = new File(remPath).getParentFile().getAbsolutePath();

            String cpCmd = String.format("cp %s %s:%s", remPath, contName, parentPath);

            runCtx.handler().runDockerCmd(host, cpCmd);
        }
        catch (IOException | InterruptedException e) {
            log().error(String.format("Failed to start container '%s' on the host '%s'",
                contName, host), e);
        }

        return nodeInfo;
    }

    /**
     * @param imageName Image name.
     * @param contName Container name.
     * @return Command to start docker container.
     */
    private String getStartContCmd(String imageName, String contName) {
        String withPlaceHolders = dockerCtx.getDockerRunCmd() != null ? dockerCtx.getDockerRunCmd() : DFLT_RUN_CMD;

        String withImageName = withPlaceHolders.replace(IMAGE_NAME_PLACEHOLDER, imageName);

        return withImageName.replace(CONT_NAME_PLACEHOLDER, contName);
    }

    /** {@inheritDoc} */
    @Override public void afterWork() {
        if (!resNodeList().isEmpty()) {
            try {
                setDockerJavaHome(resNodeList().get(0));
            }
            catch (IOException | InterruptedException e) {
                log().error(String.format("Failed to set Java home for the node type'%s'",
                    resNodeList().get(0).nodeType()), e);
            }
        }
    }

    /**
     * @param nodeInfo Node info.
     * @throws IOException if failed.
     * @throws InterruptedException if interrupted.
     */
    private void setDockerJavaHome(NodeInfo nodeInfo) throws IOException, InterruptedException {
        NodeType type = nodeInfo.nodeType();

        String contName = nodeInfo.dockerInfo().contName();

        String echoCmd = String.format("exec %s sh -c 'echo $JAVA_HOME'", contName);

        String host = nodeInfo.host();

        CommandExecutionResult res = runCtx.handler().runDockerCmd(host, echoCmd);

        if (!res.outputList().isEmpty()) {
            String javaHome = res.outputList().get(0);

            switch (type) {
                case SERVER:
                    if (dockerCtx.serverDockerJavaHome() == null) {
                        log().info(String.format("Using docker JAVA_HOME for server nodes: '%s'",
                            javaHome));

                        dockerCtx.serverDockerJavaHome(javaHome);
                    }
                    break;
                case DRIVER:
                    if (dockerCtx.driverDockerJavaHome() == null) {
                        log().info(String.format("Using docker JAVA_HOME for driver nodes: '%s'",
                            javaHome));

                        dockerCtx.driverDockerJavaHome(javaHome);
                    }
                    break;
                default:
                    log().info("Unknown node type " + type);
            }
        }
        else {
            log().info(String.format("Failed to get JAVA_HOME variable from docker container '%s'", contName));

            log().info("Will clean up docker and exit.");

            DockerRunner runner = new DockerRunner(runCtx);

            runner.cleanUp(runCtx.nodeTypes(RunMode.DOCKER), "after");

            System.exit(1);
        }

    }
}