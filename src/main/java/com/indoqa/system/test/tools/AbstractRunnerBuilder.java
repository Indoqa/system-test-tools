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
import java.nio.file.Path;

public abstract class AbstractRunnerBuilder<R extends JarRunner> {

    protected R runner;

    protected static void requireNonNull(Object object, String message) {
        if (object == null) {
            fail(message);
        }
    }

    private static URL createURL(String checkAddress) {
        try {
            return new URL(checkAddress);
        } catch (MalformedURLException e) {
            fail("Cannot create URL from " + checkAddress);
        }
        return null;
    }

    public AbstractRunnerBuilder<R> addArgument(String arg) {
        return this.addArguments(arg);
    }

    public AbstractRunnerBuilder<R> addArguments(String... arg) {
        requireNonNull(arg, "The argument must not be null.");

        this.runner.addArguments(arg);
        return this;
    }

    public AbstractRunnerBuilder<R> addSysProp(String name, String value) {
        requireNonNull(name, "The name of the system property must not be null.");
        requireNonNull(value, "The value of the system property must not be null.");

        this.runner.addSysProp(name, value);
        return this;
    }

    public R build() {
        this.runner.run();
        return this.runner;
    }

    public AbstractRunnerBuilder<R> preInitialization(JarRunnerAction action) {
        this.runner.setPreInitializationAction(action);
        return this;
    }

    public AbstractRunnerBuilder<R> setAlwaysWait(int millis) {
        this.runner.setAlwaysWait(millis);
        return this;
    }

    public AbstractRunnerBuilder<R> setCheckAdress(String checkAddress) {
        this.runner.setCheckAddress(createURL(checkAddress));
        return this;
    }

    public AbstractRunnerBuilder<R> setCheckIntervall(int millis) {
        if (millis < MIN_CHECK_INTERVALL) {
            fail("A check intervall lower than " + MIN_CHECK_INTERVALL + " ms does not make sense.");
        }

        this.runner.setCheckIntervall(millis);
        return this;
    }

    public AbstractRunnerBuilder<R> setErrorStream(PrintStream err) {
        requireNonNull(err, "The error stream must not be null.");

        this.runner.setErr(err);
        return this;
    }

    public AbstractRunnerBuilder<R> setOutStream(PrintStream out) {
        requireNonNull(out, "The out stream must not be null.");

        this.runner.setOut(out);
        return this;
    }

    public AbstractRunnerBuilder<R> setWaitForStartupInSeconds(int seconds) {
        if (seconds <= 0) {
            fail("The 'waitForStartup' time must be a positive number.");
        }

        this.runner.setWaitForStartupInSeconds(seconds);
        return this;
    }

    public AbstractRunnerBuilder<R> setWorkingDir(Path workDir) {
        requireNonNull(workDir, "The workDir must not be null.");

        this.runner.setWorkingDir(workDir);
        return this;
    }
}
