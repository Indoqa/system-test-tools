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

public class JarRunnerBuilder {

    private JarRunner jarRunner;

    public JarRunnerBuilder(Path javaRunnablePath) {
        requireNonNull(javaRunnablePath, "The Jar runner needs a path to the Java runnable.");

        if (!Files.exists(javaRunnablePath)) {
            fail("The Java runnable " + javaRunnablePath.toAbsolutePath().toString() + " does not exist.");
        }

        this.jarRunner = new JarRunner(javaRunnablePath);
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

    public JarRunnerBuilder addArgument(String arg) {
        return this.addArguments(arg);
    }

    public JarRunnerBuilder addArguments(String... arg) {
        requireNonNull(arg, "The argument must not be null.");

        this.jarRunner.addArguments(arg);
        return this;
    }

    public JarRunnerBuilder addOptions(String... options) {
        requireNonNull(options, "The option must not be null.");

        this.jarRunner.addJavaOptions(options);
        return this;
    }

    public JarRunnerBuilder addSysProp(String name, int value) {
        return this.addSysProp(name, String.valueOf(value));
    }

    public JarRunnerBuilder addSysProp(String name, String value) {
        requireNonNull(name, "The name of the system property must not be null.");
        requireNonNull(value, "The value of the system property must not be null.");

        this.jarRunner.addSysProp(name, value);
        return this;
    }

    public JarRunner build() {
        this.jarRunner.run();
        return this.jarRunner;
    }

    public JarRunnerBuilder preInitialization(JarRunnerAction action) {
        this.jarRunner.setPreInitializationAction(action);
        return this;
    }

    public JarRunnerBuilder setAlwaysWait(int millis) {
        this.jarRunner.setAlwaysWait(millis);
        return this;
    }

    public JarRunnerBuilder setCheckAdress(String checkAddress) {
        this.jarRunner.setCheckAddress(createURL(checkAddress));
        return this;
    }

    public JarRunnerBuilder setCheckIntervall(int millis) {
        if (millis < MIN_CHECK_INTERVALL) {
            fail("A check intervall lower than " + MIN_CHECK_INTERVALL + " ms does not make sense.");
        }

        this.jarRunner.setCheckIntervall(millis);
        return this;
    }

    public JarRunnerBuilder setErrorStream(PrintStream err) {
        requireNonNull(err, "The error stream must not be null.");

        this.jarRunner.setErr(err);
        return this;
    }

    public JarRunnerBuilder setOutStream(PrintStream out) {
        requireNonNull(out, "The out stream must not be null.");

        this.jarRunner.setOut(out);
        return this;
    }

    public JarRunnerBuilder setWaitForStartupInSeconds(int seconds) {
        if (seconds <= 0) {
            fail("The 'waitForStartup' time must be a positive number.");
        }

        this.jarRunner.setWaitForStartupInSeconds(seconds);
        return this;
    }

    public JarRunnerBuilder setWorkingDir(Path workDir) {
        requireNonNull(workDir, "The workDir must not be null.");

        this.jarRunner.setWorkingDir(workDir);
        return this;
    }
}
