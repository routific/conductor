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

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("conductor.tracing")
public class TracingProperties {

    private Boolean enabled = false;

    private String tracesEndpoint = null;

    private String metricsEndpoint = null;

    private String sentryDsn = null;

    private Double tracesSamplingRate = 1.0;

    public Boolean getEnabled() {
        return this.enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Double getTracesSampleRate() {
        return this.tracesSamplingRate;
    }

    public void setTracesSamplingRate(Double tracesSamplingRate) {
        this.tracesSamplingRate = tracesSamplingRate;
    }

    public String getSentryDsn() {
        return this.sentryDsn;
    }

    public void setSentryDsn(String sentryDsn) {
        this.sentryDsn = sentryDsn;
    }

    public String getTracesEndpoint() {
        return this.tracesEndpoint;
    }

    public void setTracesEndpoint(String tracesEndpoint) {
        this.tracesEndpoint = tracesEndpoint;
    }

    public String getMetricsEndpoint() {
        return this.metricsEndpoint;
    }

    public void setMetricsEndpoint(String metricsEndpoint) {
        this.metricsEndpoint = metricsEndpoint;
    }
}
