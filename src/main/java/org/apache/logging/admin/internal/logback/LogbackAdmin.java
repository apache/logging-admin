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
package org.apache.logging.admin.internal.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.admin.LoggingAdmin;
import org.jspecify.annotations.Nullable;

class LogbackAdmin implements LoggingAdmin {

    private static final Set<String> levels = Stream.of(
                    Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.OFF)
            .map(Level::toString)
            .collect(Collectors.toSet());

    private final LoggerContext loggerContext;

    @Override
    public Object getLoggerContext() {
        return loggerContext;
    }

    LogbackAdmin(final LoggerContext loggerContext) {
        this.loggerContext = loggerContext;
    }

    @Override
    public Set<String> getSupportedLevels() {
        return levels;
    }

    @Override
    public Map<String, Optional<String>> getLoggerLevels() {
        return loggerContext.getLoggerList().stream()
                .collect(Collectors.toMap(Logger::getName, LogbackAdmin::getLoggerLevel));
    }

    @Override
    public Optional<String> getLoggerLevel(String loggerName) {
        return getLoggerLevel(loggerContext.getLogger(loggerName));
    }

    private static Optional<String> getLoggerLevel(ch.qos.logback.classic.Logger logger) {
        return Optional.ofNullable(logger.getLevel()).map(Level::toString);
    }

    @Override
    public void setLoggerLevel(String loggerName, @Nullable String level) {
        loggerContext.getLogger(loggerName).setLevel(level != null ? Level.valueOf(level) : null);
    }
}
