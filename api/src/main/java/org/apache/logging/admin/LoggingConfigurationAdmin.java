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

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.logging.admin.internal.FactoryHolder;
import org.jspecify.annotations.Nullable;

/**
 * The {@code LoggingConfigurationAdmin} interface provides a logging implementation independent support to:
 * <ul>
 *   <li>Query the logging implementation for the configured level of each logger.</li>
 *   <li>Change the configured level of a logger.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <p>
 *   This API should only be used by Java applications or libraries dedicated to the modification of logging configuration.
 *   For this purpose, the API features security tokens that prevent other libraries from accessing it.
 * </p>
 * <p>
 *   An example application of this API is to modify the level of the root logger, based on CLI parameters:
 * </p>
 * <pre>
 *   public final class Main {
 *     private static final Object TOKEN = new Object();
 *
 *     public static void main(String[] args) {
 *       int i = 0;
 *       while (i < args.length) {
 *         if ("--logLevel".equals(args[i]) && ++i < args.length) {
 *           LoggingConfigurationAdmin admin = LoggingConfigurationAdmin.getInstance(TOKEN);
 *           admin.setLoggerLevel("", args[i]);
 *         }
 *         i++;
 *       }
 *     }
 *   }
 * </pre>
 */
public interface LoggingConfigurationAdmin {

    /**
     * The logger context that this interface will be configuring.
     *
     * @return A logger context.
     */
    Object getLoggerContext();

    /**
     * The names of the available log levels.
     */
    Set<String> getSupportedLevels();

    /**
     * A map associating logger names with the configured log levels.
     * <p>
     *   Loggers that inherit their configuration from the parent logger will be associated with {@link Optional#empty()}.
     * </p>
     */
    Map<String, Optional<String>> getLoggerLevels();

    /**
     * The configured log level for the given logger.
     * <p>
     *   If a logger inherits its configuration from its parent, {@link Optional#empty()} will be returned.
     * </p>
     * @param loggerName The name of the logger.
     */
    Optional<String> getLoggerLevel(String loggerName);

    /**
     * Sets the level for a logger.
     *
     * @param loggerName The name of the logger.
     * @param level The level to use or {@code null} to inherit the level of the parent logger.
     */
    void setLoggerLevel(String loggerName, @Nullable String level);

    /**
     * Retrieves the logging configuration admin for the given classloader.
     * <p>
     *   If {@code token} is not null, it also sets a security token for the appropriate logger context.
     *   All future invocations of this method will need to use the same token.
     *   Tokens are compared using object equality.
     * </p>
     * @param token The security token.
     * @param classLoader A class loader.
     * @return A logging configuration admin.
     * @throws IllegalStateException If no implementation of the Apache Logging Admin API is present on the classpath.
     * @throws SecurityException If a security token is set for the associated logger context and the provided token
     * does not match.
     */
    static LoggingConfigurationAdmin getInstance(Object token, ClassLoader classLoader) {
        return FactoryHolder.getLoggingConfigurationAdmin(requireNonNull(token), requireNonNull(classLoader));
    }

    /**
     * Retrieves the logging configuration admin for the classloader that loaded this class.
     * <p>
     *   If {@code token} is not null, it also sets a security token for the appropriate logger context.
     *   All future invocations of this method will need to use the same token.
     *   Tokens are compared using object equality.
     * </p>
     * @return A logging configuration admin.
     * @throws IllegalStateException If no implementation of the Apache Logging Admin API is present on the classpath.
     * @throws SecurityException If a security token is set for the associated logger context and the provided token
     * does not match.
     */
    static LoggingConfigurationAdmin getInstance(Object token) {
        return FactoryHolder.getLoggingConfigurationAdmin(
                requireNonNull(token), LoggingConfigurationAdmin.class.getClassLoader());
    }
}
