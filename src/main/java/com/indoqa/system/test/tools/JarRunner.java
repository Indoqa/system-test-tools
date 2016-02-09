/*
 * Licensed to the Indoqa Software Design und Beratung GmbH (Indoqa) under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Indoqa licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.indoqa.system.test.tools;

import static java.io.File.separator;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.OS;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang3.StringUtils;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

public class JarRunner extends ExternalResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(JarRunner.class);
    private static final long SECOND_TO_MILLIS = SECONDS.toMillis(1);
    private static final String PROCESS_KEY_PREFIX = "process-key";
    private static final String ENV_VAR_JAVA_HOME = "JAVA_HOME";

    private static final Path DEFAULT_WORKING_DIR = Paths.get(".");
    private static final PrintStream DEFAULT_OUT = System.out;
    private static final PrintStream DEFAULT_ERR = System.err;
    private static final int DEFAULT_HTTP_TIMEOUTS = 500;
    private static final long DEFAULT_MAX_WAIT_FOR_STARTUP = 10;
    private static final int DEFAULT_CHECK_INTERVALL = 500;
    private static final int DEFAULT_ALWAYS_WAIT = 0;
    protected static final int MIN_CHECK_INTERVALL = 5;

    private final Path javaRunnablePath;

    private URL checkAddress;
    private String processKey;
    private String javaHome;
    private Map<String, String> runnableSysProps = new ConcurrentHashMap<>();
    private List<String> arguments = new ArrayList<>();
    private PrintStream out = DEFAULT_OUT;
    private PrintStream err = DEFAULT_ERR;
    private Path workingDir = DEFAULT_WORKING_DIR;
    private long waitForStartupInSeconds = DEFAULT_MAX_WAIT_FOR_STARTUP;
    private int checkIntervall = DEFAULT_CHECK_INTERVALL;
    private int alwaysWait = DEFAULT_ALWAYS_WAIT;
    private JarRunnerAction preInitializationAction;

    protected JarRunner(Path javaRunnablePath) {
        checkOS();
        this.javaRunnablePath = javaRunnablePath;
    }

    private static void checkOS() {
        if (OS.isFamilyUnix() || OS.isFamilyWindows() || OS.isFamilyMac()) {
            return;
        }

        fail("The integration tests only run on Unix, MacOS or Windows based operating systems at present.");
    }

    private static String extractCommandFromJpsLine(String jpsLine) {
        List<String> parts = new ArrayList<>(Arrays.asList(jpsLine.split(" ")));
        if (parts.size() > 1) {
            parts.remove(0);
            return StringUtils.join(parts, " ");
        }
        return "unknown command";
    }

    private static String extractPid(String line) {
        return line.split(" ")[0];
    }

    private static void sleep(int sleep) {
        try {
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    protected void addArguments(String... arg) {
        this.arguments.addAll(Arrays.asList(arg));
    }

    protected void addSysProp(String name, String value) {
        this.runnableSysProps.put(name, value);
    }

    @Override
    protected void after() {
        this.cleanJavaProcesses();
    }

    protected Path getJavaRunnablePath() {
        return this.javaRunnablePath;
    }

    protected void run() {
        this.initializeJavaHome();
        this.initializeProcessKey();

        this.cleanJavaProcesses();
        this.preInitialization();

        this.startProcess();
        this.waitForAddress();
        this.alwaysWait();
    }

    protected void setAlwaysWait(int millis) {
        this.alwaysWait = millis;
    }

    protected void setCheckAddress(URL checkAddress) {
        this.checkAddress = checkAddress;
    }

    protected void setCheckIntervall(int checkIntervall) {
        this.checkIntervall = checkIntervall;
    }

    protected void setErr(PrintStream err) {
        this.err = err;
    }

    protected void setOut(PrintStream out) {
        this.out = out;
    }

    protected void setPreInitializationAction(JarRunnerAction preInitializationConsumer) {
        this.preInitializationAction = preInitializationConsumer;
    }

    protected void setWaitForStartupInSeconds(long waitForStartupInSeconds) {
        this.waitForStartupInSeconds = waitForStartupInSeconds;
    }

    protected void setWorkingDir(Path workingDir) {
        this.workingDir = workingDir;
    }

    private void alwaysWait() {
        if (this.alwaysWait > 0) {
            sleep(this.alwaysWait);
        }
    }

    private String buildStartCommand(String identifier) {
        StringBuilder commandBuilder = new StringBuilder().append(this.javaHome)
            .append(separator)
            .append("bin")
            .append(separator)
            .append("java");

        this.runnableSysProps.forEach((key, value) -> commandBuilder.append(" -D").append(key).append("=").append(value));

        return commandBuilder.append(" -D")
            .append(this.processKey)
            .append(" -jar ")
            .append(this.javaRunnablePath)
            .append(" ")
            .append(StringUtils.join(this.arguments, " "))
            .toString();
    }

    private void cleanJavaProcesses() {
        try {
            Map<String, String> pids = this.findJavaProcesses();
            LOGGER.info("Found {} Java process(es) with key '" + this.processKey + "' to be killed.", pids.size());

            for (String eachPid : pids.keySet()) {
                this.killProcess(eachPid, pids.get(eachPid));
            }
        } catch (InvalidExitValueException | IOException | InterruptedException | TimeoutException e) {
            LOGGER.error("Error while cleaning Java processes.", e);
            fail(e.getMessage());
        }
    }

    private Predicate<String> containsProcessKey() {
        return resultLine -> resultLine.contains(this.processKey);
    }

    private ProcessExecutor createKillCommand(String pid) {
        if (OS.isFamilyWindows()) {
            return new ProcessExecutor().command("taskkill", "/F", "/PID", pid);
        }
        return new ProcessExecutor().command("kill", pid);
    }

    private Map<String, String> findJavaProcesses() {
        String jpsCommand = new StringBuilder().append(this.javaHome)
            .append(separator)
            .append("bin")
            .append(separator)
            .append("jps")
            .toString();

        try {
            String jpsResult = new ProcessExecutor().command(jpsCommand, "-mlvV").readOutput(true).execute().outputUTF8();
            Stream<String> linesStream = Arrays.stream(jpsResult.split("\\r?\\n"));

            return linesStream.filter(this.containsProcessKey())
                .collect(toMap(JarRunner::extractPid, JarRunner::extractCommandFromJpsLine));
        } catch (InvalidExitValueException | IOException | InterruptedException | TimeoutException e) {
            fail("Error while cleaning Java processes: " + e.getMessage());
        }
        return emptyMap();
    }

    private void initializeJavaHome() {
        if (!StringUtils.isBlank(this.javaHome)) {
            return;
        }

        String envJavaHome = System.getenv(ENV_VAR_JAVA_HOME);

        if (StringUtils.isBlank(envJavaHome)) {
            fail("The environment variable " + ENV_VAR_JAVA_HOME + " is not set or blank.");
        }

        this.javaHome = envJavaHome;
    }

    private void initializeProcessKey() {
        StringBuilder processKeyBuilder = new StringBuilder();
        processKeyBuilder.append(PROCESS_KEY_PREFIX);
        processKeyBuilder.append("_");
        processKeyBuilder.append(sha1Hex(this.javaRunnablePath.toAbsolutePath().toString()));

        this.processKey = processKeyBuilder.toString();
    }

    private void killProcess(String pid, String command) throws IOException, InterruptedException, TimeoutException {
        LOGGER.info("Going to kill process with pid {} (Java command: {})", pid, command);
        ProcessResult killResult = this.createKillCommand(pid).environment(System.getenv()).execute();
        if (killResult.getExitValue() == 0) {
            LOGGER.info("Killed process with pid {}", pid);
            return;
        }

        fail("Could not 'force kill' the process with pid " + pid);
    }

    private void preInitialization() {
        if (this.preInitializationAction == null) {
            LOGGER.info("There is no pre-initialization action.");
            return;
        }

        LOGGER.info("Going to perform pre-initialization action.");
        this.preInitializationAction.perform();
        LOGGER.info("Pre-initialization action completed");
    }

    private void startProcess() {
        String command = this.buildStartCommand(this.processKey);
        CommandLine cmdLine = CommandLine.parse(command);
        LOGGER.info("Executing " + cmdLine);

        DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler(new PumpStreamHandler(this.out, this.err));
        executor.setWorkingDirectory(this.workingDir.toFile());

        try {
            DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
            executor.execute(cmdLine, System.getenv(), handler);

            if (handler.hasResult() && handler.getExitValue() != 0) {
                fail("Error while executing Java command '" + command + ". The command returned with exit value "
                        + handler.getExitValue() + ". Exception: " + handler.getException());
            }
        } catch (IOException e) {
            fail("Error while executing Java command: " + command + " (" + e.getMessage() + ")");
        }
    }

    private void waitForAddress() {
        if (this.checkAddress == null) {
            LOGGER.info("No check URL set.");
            return;
        }

        String urlString = null;
        try {
            urlString = this.checkAddress.toURI().toASCIIString();
            LOGGER.info("Waiting up to " + this.waitForStartupInSeconds + " seconds for '" + urlString + "' to respond.");

            long intervalls = this.waitForStartupInSeconds * SECOND_TO_MILLIS / this.checkIntervall;
            for (int i = 0; i < intervalls; i++) {
                HttpURLConnection connection = (HttpURLConnection) this.checkAddress.openConnection();
                connection.setConnectTimeout(DEFAULT_HTTP_TIMEOUTS);
                connection.setReadTimeout(DEFAULT_HTTP_TIMEOUTS);

                try {
                    int responseCode = connection.getResponseCode();
                    LOGGER.info("Accessing '" + urlString + "': attempt=" + i + ", responseCode=" + responseCode);
                    if (responseCode == HTTP_OK) {
                        break;
                    }
                } catch (SocketTimeoutException | ConnectException e) {
                    LOGGER.info("Accessing '" + urlString + "': attempt=" + i + ", exception=" + e.getMessage());
                }
                sleep(this.checkIntervall);
            }
        } catch (MalformedURLException | URISyntaxException e) {
            fail("Invalid address '" + urlString + "'. exception=" + e.getMessage());
        } catch (IOException e) {
            fail("Failed to wait for response from '" + urlString + "'. exception=" + e.getMessage());
        }
    }
}
