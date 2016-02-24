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

import static com.indoqa.system.test.tools.JarRunner.MIN_CHECK_INTERVALL;
import static org.junit.Assert.fail;

import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class WarRunnerBuilder {

    private WarRunner warRunner;

    public WarRunnerBuilder(Path warArchivePath) {
        requireNonNull(warArchivePath, "The War runner needs a path to the WAR archive.");

        if (!Files.exists(warArchivePath)) {
            fail("The War archive " + warArchivePath.toAbsolutePath().toString() + " does not exist.");
        }

        this.warRunner = new WarRunner(warArchivePath);
    }

    private static URL createURL(String checkAddress) {
        try {
            return new URL(checkAddress);
        } catch (MalformedURLException e) {
            fail("Cannot create URL from " + checkAddress);
        }
        return null;
    }

    private static void requireNonNull(Object object, String message) {
        if (object == null) {
            fail(message);
        }
    }

    public WarRunnerBuilder addArgument(String arg) {
        return this.addArguments(arg);
    }

    public WarRunnerBuilder addArguments(String... arg) {
        requireNonNull(arg, "The argument must not be null.");

        this.warRunner.addArguments(arg);
        return this;
    }

    public WarRunnerBuilder addSysProp(String name, String value) {
        requireNonNull(name, "The name of the system property must not be null.");
        requireNonNull(value, "The value of the system property must not be null.");

        this.warRunner.addSysProp(name, value);
        return this;
    }

    public WarRunner build() {
        this.warRunner.run();
        return this.warRunner;
    }

    public WarRunnerBuilder preInitialization(JarRunnerAction action) {
        this.warRunner.setPreInitializationAction(action);
        return this;
    }

    public WarRunnerBuilder setAlwaysWait(int millis) {
        this.warRunner.setAlwaysWait(millis);
        return this;
    }

    public WarRunnerBuilder setCheckAdress(String checkAddress) {
        this.warRunner.setCheckAddress(createURL(checkAddress));
        return this;
    }

    public WarRunnerBuilder setCheckIntervall(int millis) {
        if (millis < MIN_CHECK_INTERVALL) {
            fail("A check intervall lower than " + MIN_CHECK_INTERVALL + " ms does not make sense.");
        }

        this.warRunner.setCheckIntervall(millis);
        return this;
    }

    public WarRunnerBuilder setErrorStream(PrintStream err) {
        requireNonNull(err, "The error stream must not be null.");

        this.warRunner.setErr(err);
        return this;
    }

    public WarRunnerBuilder setHttpPort(int httpPort) {
        this.warRunner.setHttpPort(httpPort);
        return this;
    }

    public WarRunnerBuilder setOutStream(PrintStream out) {
        requireNonNull(out, "The out stream must not be null.");

        this.warRunner.setOut(out);
        return this;
    }

    public WarRunnerBuilder setWaitForStartupInSeconds(int seconds) {
        if (seconds <= 0) {
            fail("The 'waitForStartup' time must be a positive number.");
        }

        this.warRunner.setWaitForStartupInSeconds(seconds);
        return this;
    }

    public WarRunnerBuilder setWorkingDir(Path workDir) {
        requireNonNull(workDir, "The workDir must not be null.");

        this.warRunner.setWorkingDir(workDir);
        return this;
    }
}
