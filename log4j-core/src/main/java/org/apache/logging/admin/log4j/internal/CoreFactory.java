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
package org.apache.logging.admin.log4j.internal;

import aQute.bnd.annotation.spi.ServiceProvider;
import org.apache.logging.admin.LoggingConfigurationAdmin;
import org.apache.logging.admin.spi.LoggingConfigurationAdminFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.LoggerContext;

@ServiceProvider(value = LoggingConfigurationAdminFactory.class)
public class CoreFactory implements LoggingConfigurationAdminFactory {

    @Override
    public boolean isActive(ClassLoader classLoader) {
        LoggerContext loggerContext = LogManager.getContext(classLoader, false);
        return loggerContext instanceof org.apache.logging.log4j.core.LoggerContext;
    }

    @Override
    public LoggingConfigurationAdmin createLoggingConfigurationAdmin(ClassLoader classLoader) {
        LoggerContext loggerContext = LogManager.getContext(classLoader, false);
        return new CoreAdmin((org.apache.logging.log4j.core.LoggerContext) loggerContext);
    }
}
