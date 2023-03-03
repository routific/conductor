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
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import io.sentry.Instrumenter;
import io.sentry.Sentry;
import io.sentry.opentelemetry.OpenTelemetryLinkErrorEventProcessor;
import io.sentry.opentelemetry.SentryPropagator;
import io.sentry.opentelemetry.SentrySpanProcessor;

public class TracingProvider {
    private static final Logger log = LoggerFactory.getLogger(TracingProvider.class);

    private OpenTelemetrySdk openTelemtrySdk;
    private ConductorProperties properties;
    private TracingProperties tracingProperties;

    public TracingProvider(ConductorProperties properties, TracingProperties tracingProperties) {
        this.properties = properties;
        this.tracingProperties = tracingProperties;

        if (tracingProperties.getEnabled()) {
            try {
                if (tracingProperties.getSentryDsn() == null) {
                    throw new Exception("missing sentry dsn");
                }

                Sentry.init(
                        options -> {
                            options.setDsn(tracingProperties.getSentryDsn());
                            options.setEnableExternalConfiguration(true);
                            options.setTracesSampleRate(tracingProperties.getTracesSamplingRate());
                            options.setInstrumenter(Instrumenter.OTEL);
                            options.addEventProcessor(new OpenTelemetryLinkErrorEventProcessor());
                        });

                Resource resource =
                        Resource.getDefault()
                                .merge(
                                        Resource.create(
                                                Attributes.of(
                                                        ResourceAttributes.SERVICE_NAME,
                                                        properties.getAppId())));

                SdkTracerProvider sdkTracerProvider =
                        SdkTracerProvider.builder()
                                .addSpanProcessor(new SentrySpanProcessor())
                                .setResource(resource)
                                .build();

                openTelemtrySdk =
                        OpenTelemetrySdk.builder()
                                .setTracerProvider(sdkTracerProvider)
                                .setPropagators(ContextPropagators.create(new SentryPropagator()))
                                .buildAndRegisterGlobal();
            } catch (Exception error) {
                log.error("Error while setting up tracing: {}", error.getMessage());
            }
        } else {
            openTelemtrySdk = null;
        }
    }

    public String getTraceHeader() {
        return this.tracingProperties.getTraceHeader();
    }

    public Tracing startTracing(String spanName, Optional<String> header) {
        if (this.openTelemtrySdk != null) {
            SpanBuilder builder = this.openTelemtrySdk.getTracer("conductor").spanBuilder(spanName);

            if (header != null && header.isPresent()) {
                // sentrytrace = traceId-parentSpanId-sampled
                String[] traceComponents = header.get().split("-");
                if (traceComponents.length != 3) {
                    return new Tracing(Optional.empty());
                }

                log.info("Starting child span: {} from header: {}", spanName, header.get());

                SpanContext spanContext =
                        SpanContext.createFromRemoteParent(
                                traceComponents[0],
                                traceComponents[1],
                                (traceComponents[2].equals("1")) ? TraceFlags.getSampled() : TraceFlags.getDefault(),
                                TraceState.getDefault());
                Span span =
                        builder.setParent(Context.current().with(Span.wrap(
                                spanContext)))
                                .startSpan();

                log.info(
                        "Started child span: {}-{}",
                        span.getSpanContext().getTraceId(),
                        span.getSpanContext().getSpanId());

                return new Tracing(Optional.ofNullable(span));

            } else {
                log.info("Starting new root span: {}", spanName);

                Span span = builder.setNoParent().startSpan();

                return new Tracing(Optional.ofNullable(span));
            }
        }

        return new Tracing(Optional.empty());
    }
}
