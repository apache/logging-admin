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
import java.util.Comparator;
import java.util.ServiceLoader;
import org.apache.logging.admin.LoggingAdmin;
import org.apache.logging.admin.spi.LoggingAdminFactory;

@ServiceConsumer(
        value = LoggingAdminFactory.class,
        cardinality = Cardinality.MULTIPLE,
        resolution = Resolution.MANDATORY)
public final class FactoryUtil {

    private static final Collection<LoggingAdminFactory> factories = new ArrayList<>();

    static {
        ServiceLoader.load(LoggingAdminFactory.class, FactoryUtil.class.getClassLoader())
                .forEach(factories::add);
    }

    public static LoggingAdmin getLoggingAdmin(Object token) {
        LoggingAdminFactory factory = factories.stream()
                .filter(LoggingAdminFactory::isActive)
                .sorted(Comparator.comparing(LoggingAdminFactory::getPriority))
                .findAny()
                .orElseThrow(() ->
                        new IllegalStateException("No active " + LoggingAdminFactory.class.getName() + " found."));
        return factory.getLoggingAdmin(token);
    }

    private FactoryUtil() {}
}
