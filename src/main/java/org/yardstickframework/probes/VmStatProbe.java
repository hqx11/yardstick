/*
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package org.yardstickframework.probes;

import org.yardstickframework.*;
import org.yardstickframework.impl.util.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;

import static org.yardstickframework.BenchmarkUtils.*;

/**
 * Probe that gathers statistics generated by Linux 'vmstat' command.
 */
public class VmStatProbe implements BenchmarkProbe {
    /** */
    private static final String PATH = "BENCHMARK_PROBE_VMSTAT_PATH";

    /** */
    private static final String OPTS = "BENCHMARK_PROBE_VMSTAT_OPTS";

    /** */
    private static final int DEFAULT_INTERVAL_IN_SECS = 1;

    /** */
    private static final String DEFAULT_PATH = "vmstat";

    /** */
    private static final String DEFAULT_OPTS = "-n " + DEFAULT_INTERVAL_IN_SECS;

    /** */
    private static final String FIRST_LINE_RE = "^\\s*procs -*memory-* -*swap-* -*io-* -*system-* -*cpu-*\\s*$";

    /** */
    private static final Pattern FIRST_LINE = Pattern.compile(FIRST_LINE_RE);

    /** */
    private static final String HEADER_LINE_RE = "^\\s*r\\s+b\\s+swpd\\s+free\\s+buff\\s+cache\\s+si\\s+so\\s+bi" +
        "\\s+bo\\s+in\\s+cs\\s+us\\s+sy\\s+id\\s+wa\\s*(st\\s*)?$";

    /** */
    private static final Pattern HEADER_LINE = Pattern.compile(HEADER_LINE_RE);

    /** */
    private static final Pattern VALUES_PAT;

    /**
     *
     */
    static {
        int numFields = 16;

        StringBuilder sb = new StringBuilder("^\\s*");

        for (int i = 0; i < numFields; i++) {
            sb.append("(\\d+)");

            if (i < numFields - 1)
                sb.append("\\s+");
            else
                sb.append("\\s*");
        }

        sb.append("(\\d+)?\\s*$");

        VALUES_PAT = Pattern.compile(sb.toString());
    }

    /** */
    private BenchmarkConfiguration cfg;

    /** */
    private BenchmarkProcessLauncher proc;

    /** Collected points. */
    private Collection<BenchmarkProbePoint> collected = new ArrayList<>();

    /** {@inheritDoc} */
    @Override public void start(BenchmarkDriver drv, BenchmarkConfiguration cfg) throws Exception {
        this.cfg = cfg;

        BenchmarkClosure<String> c = new BenchmarkClosure<String>() {
            private final AtomicInteger lineNum = new AtomicInteger(0);

            @Override public void apply(String s) {
                parseLine(lineNum.getAndIncrement(), s);
            }
        };

        proc = new BenchmarkProcessLauncher();

        Collection<String> cmdParams = new ArrayList<>();

        cmdParams.add(path(cfg));
        cmdParams.addAll(opts(cfg));

        String execCmd = cmdParams.toString().replaceAll(",|\\[|\\]", "");

        try {
            proc.exec(cmdParams, Collections.<String, String>emptyMap(), c);

            println(cfg, this.getClass().getSimpleName() + " is started. Command: '" + execCmd + "'");
        }
        catch (Exception e) {
            errorHelp(cfg, "Can not start: '" + execCmd + "'", e);
        }
    }

    /** {@inheritDoc} */
    @Override public void stop() throws Exception {
        if (proc != null) {
            proc.shutdown(false);

            println(cfg, this.getClass().getSimpleName() + " is stopped.");
        }
    }

    /** {@inheritDoc} */
    @Override public Collection<String> metaInfo() {
        return Arrays.asList("Time, sec", "Processes Waiting For Run Time", "Processes In Uninterruptible Sleep",
            "Memory Used, KB", "Memory Free, KB", "Memory Buffered, KB", "Memory Cached, KB",
            "Memory Swapped In From Disk, per sec", "Memory Swapped To Disk, per sec",
            "IO Blocks Received, blocks/sec", "IO Blocks Sent, blocks/sec",
            "System Interrupts, per sec", "System Context Switches, per sec",
            "CPU User, %", "CPU System, %", "CPU Idle, %", "CPU Wait, %");
    }

    /** {@inheritDoc} */
    @Override public synchronized Collection<BenchmarkProbePoint> points() {
        Collection<BenchmarkProbePoint> ret = collected;

        collected = new ArrayList<>(ret.size() + 5);

        return ret;
    }

    /**
     * @param pnt Probe point.
     */
    private synchronized void collectPoint(BenchmarkProbePoint pnt) {
        collected.add(pnt);
    }

    /**
     * @param lineNum Line number.
     * @param line Line to parse.
     */
    private void parseLine(int lineNum, String line) {
        if (lineNum == 0) {
            Matcher m = FIRST_LINE.matcher(line);

            if (!m.matches())
                println(cfg, "WARNING: Unexpected first line: " + line);
        }
        else if (lineNum == 1) {
            Matcher m = HEADER_LINE.matcher(line);

            if (!m.matches())
                errorHelp(cfg, "Header line does not match expected header " +
                    "[exp=" + HEADER_LINE + ", act=" + line + "]");
        }
        else {
            Matcher m = VALUES_PAT.matcher(line);

            if (m.matches()) {
                try {
                    BenchmarkProbePoint pnt = new BenchmarkProbePoint(
                        TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()),
                        new double[] {
                            parseValue(m.group(1)), parseValue(m.group(2)),
                            parseValue(m.group(3)), parseValue(m.group(4)),
                            parseValue(m.group(5)), parseValue(m.group(6)),
                            parseValue(m.group(7)), parseValue(m.group(8)),
                            parseValue(m.group(9)), parseValue(m.group(10)),
                            parseValue(m.group(11)), parseValue(m.group(12)),
                            parseValue(m.group(13)), parseValue(m.group(14)),
                            parseValue(m.group(15)), parseValue(m.group(16)),
                        });

                    collectPoint(pnt);
                }
                catch (NumberFormatException e) {
                    errorHelp(cfg, "Can't parse line: " + line, e);
                }
            }
            else
                errorHelp(cfg, "Can't parse line: " + line);
        }
    }

    /**
     * @param val Value.
     * @return Parsed value.
     */
    private static long parseValue(String val) {
        return Long.parseLong(val);
    }

    /**
     * @param cfg Config.
     * @return Path to vmstat executable.
     */
    private static String path(BenchmarkConfiguration cfg) {
        String res = cfg.customProperties().get(PATH);

        return res == null || res.isEmpty() ? DEFAULT_PATH : res;
    }

    /**
     * @param cfg Config.
     * @return Options of vmstat.
     */
    private static Collection<String> opts(BenchmarkConfiguration cfg) {
        String res = cfg.customProperties().get(OPTS);

        res = res == null || res.isEmpty() ? DEFAULT_OPTS : res;

        return Arrays.asList(res.split("\\s+"));
    }
}