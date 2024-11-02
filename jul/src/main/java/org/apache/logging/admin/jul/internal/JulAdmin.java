/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
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
package org.apache.logging.admin.jul.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.admin.LoggingConfigurationAdmin;
import org.jspecify.annotations.Nullable;

class JulAdmin implements LoggingConfigurationAdmin {

    private static final Set<String> levels = Stream.of(
                    Level.ALL,
                    Level.FINEST,
                    Level.FINER,
                    Level.FINE,
                    Level.CONFIG,
                    Level.INFO,
                    Level.WARNING,
                    Level.SEVERE,
                    Level.OFF)
            .map(Level::toString)
            .collect(Collectors.toSet());

    private final LogManager logManager = LogManager.getLogManager();
    private final ClassLoader classLoader;

    JulAdmin(final ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public Object getLoggerContext() {
        // Implementations that support multiple logger contexts can be recognized by having multiple root loggers.
        ClassLoader oldClassLoader = updateThreadContextClassLoader(classLoader);
        try {
            return Logger.getLogger("");
        } finally {
            updateThreadContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public Set<String> getSupportedLevels() {
        return levels;
    }

    @Override
    public Map<String, Optional<String>> getLoggerLevels() {
        ClassLoader oldClassLoader = updateThreadContextClassLoader(classLoader);
        try {
            final Map<String, Optional<String>> loggerLevels = new HashMap<>();
            for (final String loggerName : Collections.list(logManager.getLoggerNames())) {
                loggerLevels.computeIfAbsent(loggerName, this::getLoggerLevel);
            }
            return loggerLevels;
        } finally {
            updateThreadContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public Optional<String> getLoggerLevel(String loggerName) {
        ClassLoader oldClassLoader = updateThreadContextClassLoader(classLoader);
        try {
            return Optional.ofNullable(logManager.getLogger(loggerName))
                    .map(Logger::getLevel)
                    .map(Level::getName);
        } finally {
            updateThreadContextClassLoader(oldClassLoader);
        }
    }

    private Optional<String> doGetLoggerLevel(String loggerName) {
        return Optional.ofNullable(logManager.getLogger(loggerName))
                .map(Logger::getLevel)
                .map(Level::getName);
    }

    @Override
    public void setLoggerLevel(String loggerName, @Nullable String level) {
        ClassLoader oldClassLoader = updateThreadContextClassLoader(classLoader);
        try {
            logManager.getLogger(loggerName).setLevel(level != null ? Level.parse(level) : null);
        } finally {
            updateThreadContextClassLoader(oldClassLoader);
        }
    }

    private static ClassLoader updateThreadContextClassLoader(final ClassLoader classLoader) {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        return old;
    }
}
