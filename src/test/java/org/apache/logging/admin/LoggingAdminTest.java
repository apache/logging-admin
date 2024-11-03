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
package org.apache.logging.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LoggingAdminTest {

    private static final Map<String, List<String>> EXPECTED_LEVELS;

    static {
        final Map<String, List<String>> expectedLevels = new HashMap<>();
        expectedLevels.put(
                "jul", Arrays.asList("OFF", "SEVERE", "WARNING", "INFO", "CONFIG", "FINE", "FINER", "FINEST", "ALL"));
        expectedLevels.put(
                "log4j-core", Arrays.asList("OFF", "FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE", "ALL"));
        expectedLevels.put("logback", Arrays.asList("OFF", "ERROR", "WARN", "INFO", "DEBUG", "TRACE"));
        EXPECTED_LEVELS = Collections.unmodifiableMap(expectedLevels);
    }

    private static final Object TOKEN = new Object();
    private static LoggingAdmin admin;

    // Log4j Core is the default in the IDE
    private static final String type = System.getProperty("admin.implementation", "log4j-core");

    private static String debugLevel() {
        return "jul".equals(type) ? "FINE" : "DEBUG";
    }

    @BeforeAll
    static void setup() {
        admin = LoggingAdmin.getInstance(TOKEN);
        // JUL creates loggers lazily, so we force the creation of `foo.bar`
        Logger.getLogger("foo.bar");
    }

    @Test
    void should_return_correct_supported_levels() {
        assertThat(admin.getSupportedLevels()).containsExactlyElementsOf(EXPECTED_LEVELS.get(type));
    }

    static Stream<Arguments> should_return_correct_configured_levels() {
        return Stream.of(
                Arguments.of(LoggingAdmin.ROOT_LOGGER_NAME, "INFO"),
                Arguments.of("foo", null),
                Arguments.of("foo.bar", debugLevel()));
    }

    @ParameterizedTest
    @MethodSource
    void should_return_correct_configured_levels(String loggerName, @Nullable String expectedLevel) {
        assertThat(admin.getLevel(loggerName))
                .as("Level of logger `%s`.", loggerName)
                .isEqualTo(expectedLevel);
    }

    @Test
    void should_return_correct_configured_level_map() {
        assertThat(admin.getLevels())
                .contains(
                        entry(LoggingAdmin.ROOT_LOGGER_NAME, "INFO"),
                        entry("foo", null),
                        entry("foo.bar", debugLevel()));
    }

    @Test
    void should_return_admin_if_token_correct() {
        assertDoesNotThrow(() -> LoggingAdmin.getInstance(TOKEN));
    }

    @Test
    void should_throw_if_token_incorrect() {
        assertThrows(SecurityException.class, () -> LoggingAdmin.getInstance(new Object()));
    }

    private static Map.Entry<String, @Nullable String> entry(String loggerName, @Nullable String level) {
        return new AbstractMap.SimpleImmutableEntry<>(loggerName, level);
    }
}
