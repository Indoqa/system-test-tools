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

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WarRunner extends JarRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(WarRunner.class);

    private String webArchive;

    protected WarRunner(Path warArchivePath) {
        super(getJettyRunnerPath());

        if (!Files.exists(warArchivePath)) {
            fail("The War archive " + warArchivePath.toAbsolutePath().toString() + " does not exist.");
        }

        this.webArchive = warArchivePath.toAbsolutePath().toString();
    }

    private static void copyJarToTempFile(URL jettyJar, File file) throws IOException, FileNotFoundException {
        InputStream inputStream = jettyJar.openStream();
        IOUtils.copy(inputStream, new FileOutputStream(file));
        IOUtils.closeQuietly(inputStream);
    }

    private static Path getJettyRunnerPath() {
        try {
            Path jettyPath = Files.createTempFile("jetty-runner", ".jar");
            copyJarToTempFile(WarRunner.class.getClassLoader().getResource("jetty-runner.jar"), jettyPath.toFile());
            return jettyPath;
        } catch (Exception e) {
            throw new IllegalStateException("Error loading jetty-runner.jar.", e);
        }
    }

    @Override
    protected void after() {
        super.after();

        try {
            Files.deleteIfExists(this.getJettyRunnerJarPath());
        } catch (Exception e) {
            LOGGER.error("Could not remove temporary jetty path.", e);
        }
    }

    protected Path getJettyRunnerJarPath() {
        return this.getJavaRunnablePath();
    }

    @Override
    protected void run() {
        this.addArguments(this.webArchive);
        super.run();
    }

    protected void setHttpPort(int httpPort) {
        this.addArguments("--port " + httpPort);
    }
}
