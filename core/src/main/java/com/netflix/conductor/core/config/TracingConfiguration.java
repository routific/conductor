/*
 * Copyright 2023 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.netflix.conductor.core.tracing.TracingProvider;

@Configuration()
@EnableConfigurationProperties(TracingProperties.class)
public class TracingConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TracingConfiguration.class);

    TracingConfiguration() {
        log.info("TraceConfiguration loaded.");
    }

    @Bean
    @Scope("singleton")
    public TracingProvider getTracing(
            ConductorProperties conductorProperties, TracingProperties tracingProperties) {
        if (tracingProperties.getEnabled()) {
            log.info("Tracing is ENABLED.");
        } else {
            log.info("Tracing is DISABLED.");
        }

        return new TracingProvider(conductorProperties, tracingProperties);
    }
}
