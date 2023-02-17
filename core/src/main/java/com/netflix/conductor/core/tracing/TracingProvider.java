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
package com.netflix.conductor.core.tracing;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.core.config.ConductorProperties;
import com.netflix.conductor.core.config.TracingProperties;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

public class TracingProvider {
    private static final Logger log = LoggerFactory.getLogger(TracingProvider.class);

    private OpenTelemetrySdk openTelemtrySdk;
    private ConductorProperties properties;

    public TracingProvider(ConductorProperties properties, TracingProperties tracingProperties) {
        this.properties = properties;

        if (tracingProperties.getEnabled()) {
            log.info(
                    "Creating Otel instance with endpint, traces: {}, metrics: {}",
                    tracingProperties.getTracesEndpoint(),
                    tracingProperties.getMetricsEndpoint());

            try {
                Resource resource =
                        Resource.getDefault()
                                .merge(
                                        Resource.create(
                                                Attributes.of(
                                                        ResourceAttributes.SERVICE_NAME,
                                                        properties.getAppId())));

                SdkTracerProvider sdkTracerProvider =
                        SdkTracerProvider.builder()
                                .addSpanProcessor(
                                        BatchSpanProcessor.builder(
                                                        OtlpHttpSpanExporter.builder()
                                                                .setEndpoint(
                                                                        tracingProperties
                                                                                .getTracesEndpoint())
                                                                .build())
                                                .build())
                                .setResource(resource)
                                .build();

                // SdkMeterProvider sdkMeterProvider =
                //         SdkMeterProvider.builder()
                //                 .registerMetricReader(
                //                         PeriodicMetricReader.builder(
                //                                         OtlpHttpMetricExporter.builder()
                //                                                 .setEndpoint(
                //                                                         tracingProperties
                //
                // .getMetricsEndpoint())
                //                                                 .build())
                //                                 .build())
                //                 .setResource(resource)
                //                 .build();

                openTelemtrySdk =
                        OpenTelemetrySdk.builder()
                                .setTracerProvider(sdkTracerProvider)
                                // .setMeterProvider(sdkMeterProvider)
                                .setPropagators(
                                        ContextPropagators.create(
                                                W3CTraceContextPropagator.getInstance()))
                                .buildAndRegisterGlobal();
            } catch (Exception error) {
                log.error("Error setting up otel: {}", error.getMessage());
                throw error;
            }
        } else {
            openTelemtrySdk = null;
        }
    }

    public Tracing startTracing(String spanName, Optional<String> traceId) {
        if (this.openTelemtrySdk != null) {
            SpanBuilder builder =
                    this.openTelemtrySdk.getTracer("random scope name").spanBuilder(spanName);

            if (traceId != null && traceId.isPresent()) {
                log.info("Starting new span: {} with trace: {}", spanName, traceId);

                String[] traceComponents = traceId.get().split("-");
                SpanContext spanContext =
                        SpanContext.createFromRemoteParent(
                                traceComponents[0],
                                traceComponents[1],
                                TraceFlags.getSampled(),
                                null);
                Span span =
                        builder.setParent(Context.current().with(Span.wrap(spanContext)))
                                .startSpan();

                return new Tracing(Optional.of(span));
            } else {
                log.info("Starting new root span: {}", spanName);

                Span span = builder.setNoParent().startSpan();

                return new Tracing(Optional.of(span));
            }
        } else {
            return new Tracing(Optional.empty());
        }
    }
}
