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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.admin.LoggingAdmin;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.util.NameUtil;
import org.jspecify.annotations.Nullable;

class Log4jCoreAdmin implements LoggingAdmin {

    private static final ReentrantLock lock = new ReentrantLock();
    private static final Map<LoggerContext, Object> tokensByLoggerContext = new WeakHashMap<>();

    private final LoggerContext loggerContext;

    Log4jCoreAdmin(LoggerContext loggerContext) {
        this.loggerContext = loggerContext;
    }

    @Override
    public List<String> getSupportedLevels() {
        return Stream.of(Level.values()).sorted().map(Level::name).collect(Collectors.toList());
    }

    @Override
    public Map<String, @Nullable String> getLevels() {
        Map<String, @Nullable String> loggerLevels = new HashMap<>();
        // Insert the ancestors of all existing loggers
        loggerContext.getLoggers().forEach(logger -> fillLoggerLevels(logger.getName(), loggerLevels));
        // Insert the ancestors of all existing logger configurations
        loggerContext
                .getConfiguration()
                .getLoggers()
                .keySet()
                .forEach(loggerName -> fillLoggerLevels(loggerName, loggerLevels));
        return loggerLevels;
    }

    @Override
    public @Nullable String getLevel(String loggerName) {
        Configuration config = loggerContext.getConfiguration();
        return Optional.of(config.getLoggerConfig(loggerName))
                .filter(lc -> loggerName.equals(lc.getName()))
                .map(LoggerConfig::getLevel)
                .map(Level::name)
                .orElse(null);
    }

    @Override
    public void setLevel(String loggerName, @Nullable String level) {
        boolean changed;
        Configuration config = loggerContext.getConfiguration();
        Level levelObj = level != null ? Level.valueOf(level) : null;
        LoggerConfig loggerConfig = config.getLoggerConfig(loggerName);
        if (!loggerName.equals(loggerConfig.getName())) {
            loggerConfig = new LoggerConfig(loggerName, levelObj, true);
            config.addLogger(loggerName, loggerConfig);
            changed = true;
        } else {
            changed = Objects.equals(levelObj, loggerConfig.getLevel());
            loggerConfig.setLevel(levelObj);
        }
        if (changed) {
            loggerContext.updateLoggers();
        }
    }

    private void fillLoggerLevels(String loggerName, Map<String, @Nullable String> loggerLevels) {
        String currentName = loggerName;
        while (currentName != null && loggerLevels.putIfAbsent(currentName, getLevel(currentName)) == null) {
            currentName = NameUtil.getSubName(currentName);
        }
    }

    static boolean isActive() {
        org.apache.logging.log4j.spi.LoggerContext loggerContext = PrivateLogManager.getContext();
        return loggerContext instanceof org.apache.logging.log4j.core.LoggerContext;
    }

    static LoggingAdmin newInstance(Object token) {
        lock.lock();
        try {
            LoggerContext loggerContext = (LoggerContext) PrivateLogManager.getContext();
            if (tokensByLoggerContext.computeIfAbsent(loggerContext, k -> token) != token) {
                throw new SecurityException("The security token does not match: " + token);
            }
            return new Log4jCoreAdmin(loggerContext);
        } finally {
            lock.unlock();
        }
    }

    private static final class PrivateLogManager extends LogManager {
        private PrivateLogManager() {}

        public static org.apache.logging.log4j.spi.LoggerContext getContext() {
            return LogManager.getContext(LoggingAdmin.class.getName(), false);
        }
    }
}
