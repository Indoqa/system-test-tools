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

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

public final class JarRunnerUtils {

    private JarRunnerUtils() {
        // hide utility class constructor
    }

    public static Predicate<Path> pathEndsWith(String suffix) {
        return file -> StringUtils.endsWith(file.getFileName().toString(), suffix);
    }

    public static Predicate<Path> endsWithRunnableJar() {
        return file -> StringUtils.endsWith(file.getFileName().toString(), "-runnable.jar");
    }

    public static Path searchJavaRunnable(Path baseDir, Predicate<Path> predicate) {
        try {
            List<Path> runnableJars = Files.list(baseDir).filter(file -> predicate.test(file.getFileName())).collect(toList());

            if (runnableJars.size() == 0) {
                fail("Cannot find the requested file in '" + baseDir.toAbsolutePath().toString() + "'.");
            }

            else if (runnableJars.size() > 1) {
                fail("There is more than file matching in '" + baseDir.toAbsolutePath().toString() + "'.");
            }

            else {
                return runnableJars.get(0);
            }
        } catch (IOException e) {
            fail("Error while searching the Java runnable in '" + baseDir.toAbsolutePath() + "': " + e.getMessage());
        }
        return null;
    }
}
