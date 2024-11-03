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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.logging.admin.internal.FactoryUtil;
import org.jspecify.annotations.Nullable;

/**
 * The {@code LoggingAdmin} interface provides a logging implementation independent support to:
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
 *           LoggingAdmin admin = LoggingAdmin.getInstance(TOKEN);
 *           admin.setLevel("", args[i]);
 *         }
 *         i++;
 *       }
 *     }
 *   }
 * </pre>
 */
public interface LoggingAdmin {
    /**
     * Implementation independent name for the root logger.
     */
    String ROOT_LOGGER_NAME = "";

    /**
     * The names of the available log levels in decreasing severity order.
     */
    List<String> getSupportedLevels();

    /**
     * A map associating logger names with the configured log levels.
     * <p>
     *   Loggers that inherit their configuration from the parent logger will be associated with {@code null}.
     * </p>
     */
    Map<String, @Nullable String> getLevels();

    /**
     * The configured log level for the given logger.
     * <p>
     *   If a logger inherits its configuration from its parent, {@link Optional#empty()} will be returned.
     * </p>
     * @param loggerName The name of the logger.
     */
    @Nullable
    String getLevel(String loggerName);

    /**
     * Sets the level for a logger.
     *
     * @param loggerName The name of the logger.
     * @param level The level to use or {@code null} to inherit the level of the parent logger.
     */
    void setLevel(String loggerName, @Nullable String level);

    /**
     * Retrieves the logging configuration admin appropriate for the caller
     * <p>
     *   The {@code token} parameter sets a security token for the appropriate logger context.
     *   All future invocations of this method will need to use the same token.
     *   Tokens are compared using object equality.
     * </p>
     * @param token Any Java object.
     * @return A logging configuration admin.
     * @throws SecurityException If a security token is set for the associated logger context and the provided token
     * does not match.
     */
    static LoggingAdmin getInstance(Object token) {
        return FactoryUtil.getLoggingAdmin(Objects.requireNonNull(token));
    }
}
