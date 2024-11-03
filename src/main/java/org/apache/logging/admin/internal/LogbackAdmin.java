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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.admin.LoggingAdmin;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;

class LogbackAdmin implements LoggingAdmin {

    private static final ReentrantLock lock = new ReentrantLock();
    private static final Map<LoggerContext, Object> tokensByLoggerContext = new WeakHashMap<>();

    private static final List<String> levels = Collections.unmodifiableList(
            Stream.of(Level.OFF, Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG, Level.TRACE)
                    .map(Level::toString)
                    .collect(Collectors.toList()));

    private final LoggerContext loggerContext;

    LogbackAdmin(final LoggerContext loggerContext) {
        this.loggerContext = loggerContext;
    }

    @Override
    public List<String> getSupportedLevels() {
        return levels;
    }

    @Override
    public Map<String, @Nullable String> getLevels() {
        Map<String, @Nullable String> loggerLevels = new HashMap<>();
        loggerContext
                .getLoggerList()
                .forEach(logger -> loggerLevels.put(rootToEmpty(logger.getName()), getLevel(logger)));
        return loggerLevels;
    }

    @Override
    public @Nullable String getLevel(String loggerName) {
        return getLevel(loggerContext.getLogger(emptyToRoot(loggerName)));
    }

    private static @Nullable String getLevel(Logger logger) {
        return Optional.ofNullable(logger.getLevel()).map(Level::toString).orElse(null);
    }

    @Override
    public void setLevel(String loggerName, @Nullable String level) {
        loggerContext.getLogger(emptyToRoot(loggerName)).setLevel(level != null ? Level.valueOf(level) : null);
    }

    private String rootToEmpty(String loggerName) {
        return Logger.ROOT_LOGGER_NAME.equals(loggerName) ? "" : loggerName;
    }

    private String emptyToRoot(String loggerName) {
        return loggerName.isEmpty() ? Logger.ROOT_LOGGER_NAME : loggerName;
    }

    static boolean isActive() {
        return LoggerFactory.getILoggerFactory() instanceof ch.qos.logback.classic.LoggerContext;
    }

    static LoggingAdmin newInstance(Object token) {
        lock.lock();
        try {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            if (tokensByLoggerContext.computeIfAbsent(loggerContext, k -> token) != token) {
                throw new SecurityException("The security token does not match: " + token);
            }
            return new LogbackAdmin(loggerContext);
        } finally {
            lock.unlock();
        }
    }
}
