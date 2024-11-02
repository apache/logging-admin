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
package org.apache.logging.admin.spi;

import org.apache.logging.admin.LoggingConfigurationAdmin;

/**
 * Factory class for {@link LoggingConfigurationAdmin} instances.
 * <p>
 *   Implementations of this class should be registered using {@link java.util.ServiceLoader}.
 * </p>
 */
public interface LoggingConfigurationAdminFactory {

    /**
     * Determines whether the logging system handled by this factory is used.
     *
     * @param classLoader The class loader associated with the logger context.
     */
    boolean isActive(ClassLoader classLoader);

    /**
     * Creates a new {@link LoggingConfigurationAdmin} instance associated with the given classloader.
     *
     * @param classLoader The class loader associated with the logger context.
     * @return A new instance of {@link LoggingConfigurationAdmin}.
     */
    LoggingConfigurationAdmin createLoggingConfigurationAdmin(ClassLoader classLoader);
}
