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

import aQute.bnd.annotation.Cardinality;
import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceConsumer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.admin.LoggingConfigurationAdmin;
import org.apache.logging.admin.spi.LoggingConfigurationAdminFactory;

@ServiceConsumer(
        value = LoggingConfigurationAdminFactory.class,
        cardinality = Cardinality.MULTIPLE,
        resolution = Resolution.MANDATORY)
public final class FactoryHolder {

    private static final Collection<LoggingConfigurationAdminFactory> factories = new ArrayList<>();
    private static final ReentrantLock lock = new ReentrantLock();
    private static final Map<ClassLoader, LoggingConfigurationAdmin> instances = new WeakHashMap<>();
    private static final Map<Object, Object> tokensByLoggerContext = new WeakHashMap<>();

    static {
        ServiceLoader.load(LoggingConfigurationAdminFactory.class, FactoryHolder.class.getClassLoader())
                .forEach(factories::add);
    }

    private static void checkSecurityToken(Object token, Object loggerContext) {
        Object oldToken = tokensByLoggerContext.get(loggerContext);
        if (oldToken == null) {
            tokensByLoggerContext.put(loggerContext, token);
            return;
        }
        if (token != oldToken) {
            throw new SecurityException("Security token changed from " + oldToken + " to " + token);
        }
    }

    public static LoggingConfigurationAdmin getLoggingConfigurationAdmin(Object token, ClassLoader classLoader) {
        lock.lock();
        try {
            LoggingConfigurationAdmin instance = instances.get(classLoader);
            if (instance == null) {
                instance = factories.stream()
                        .filter(f -> f.isActive(classLoader))
                        .findAny()
                        .map(f -> f.createLoggingConfigurationAdmin(classLoader))
                        .orElseThrow(() -> new IllegalStateException(
                                "No " + LoggingConfigurationAdminFactory.class.getName() + " found."));
                instances.put(classLoader, instance);
            }
            checkSecurityToken(token, instance.getLoggerContext());
            return instance;
        } finally {
            lock.unlock();
        }
    }

    private FactoryHolder() {}
}
