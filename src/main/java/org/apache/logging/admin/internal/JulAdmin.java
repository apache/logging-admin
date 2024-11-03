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
package org.apache.logging.admin.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.admin.LoggingAdmin;
import org.jspecify.annotations.Nullable;

class JulAdmin implements LoggingAdmin {

    private static final ReentrantLock lock = new ReentrantLock();
    private static final Map<Logger, Object> tokensByRootLogger = new WeakHashMap<>();

    private static final List<String> levels = Collections.unmodifiableList(Stream.of(
                    Level.OFF,
                    Level.SEVERE,
                    Level.WARNING,
                    Level.INFO,
                    Level.CONFIG,
                    Level.FINE,
                    Level.FINER,
                    Level.FINEST,
                    Level.ALL)
            .map(Level::toString)
            .collect(Collectors.toList()));

    private final LogManager logManager = LogManager.getLogManager();
    private final Logger rootLogger;

    JulAdmin(Logger rootLogger) {
        this.rootLogger = rootLogger;
    }

    @Override
    public List<String> getSupportedLevels() {
        return levels;
    }

    @Override
    public Map<String, @Nullable String> getLevels() {
        final Map<String, @Nullable String> loggerLevels = new HashMap<>();
        Collections.list(logManager.getLoggerNames()).forEach(loggerName -> fillLoggerLevels(loggerName, loggerLevels));
        return loggerLevels;
    }

    @Override
    public @Nullable String getLevel(String loggerName) {
        return Optional.ofNullable(logManager.getLogger(loggerName))
                .map(Logger::getLevel)
                .map(Level::getName)
                .orElse(null);
    }

    @Override
    public void setLevel(String loggerName, @Nullable String level) {
        Logger logger = logManager.getLogger(loggerName);
        Logger rootLogger = findRootLogger(logger);
        // Prevents setting the log level of a different "logger context"
        if (this.rootLogger.equals(rootLogger)) {
            logger.setLevel(level != null ? Level.parse(level) : null);
        }
    }

    private void fillLoggerLevels(String loggerName, Map<String, @Nullable String> loggerLevels) {
        String currentName = loggerName;
        while (loggerLevels.putIfAbsent(currentName, getLevel(currentName)) == null) {
            if (currentName.isEmpty()) {
                break;
            }
            int idx = currentName.lastIndexOf('.');
            currentName = idx == -1 ? "" : currentName.substring(0, idx);
        }
    }

    private static Logger findRootLogger(Logger logger) {
        Logger current = logger;
        while (current != null) {
            if ("".equals(current.getName()) || current.getParent() == null) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Unable to find root logger for " + logger);
    }

    /**
     * Checks if the `java.logging` module is present.
     * @throws LinkageError If `java.logging` is absent.
     */
    static boolean isActive() {
        return LogManager.getLogManager() != null;
    }

    static LoggingAdmin newInstance(Object token) {
        lock.lock();
        try {
            Logger rootLogger = Logger.getLogger("");
            if (tokensByRootLogger.computeIfAbsent(rootLogger, k -> token) != token) {
                throw new SecurityException("The security token does not match: " + token);
            }
            return new JulAdmin(rootLogger);
        } finally {
            lock.unlock();
        }
    }
}
