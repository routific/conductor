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

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;

public class Tracing {
    private Optional<Span> span = Optional.empty();

    public Tracing(Optional<Span> span) {
        this.span = span;
    }

    public Optional<String> getTraceId() {
        if (this.span.isPresent()) {
            SpanContext spanContext = this.span.get().getSpanContext();
            return Optional.of(spanContext.getTraceId() + "-" + spanContext.getSpanId() + "-" + (spanContext.getTraceFlags().isSampled() ? "1" : "0"));
        }

        return Optional.empty();
    }

    public void finish() {
        if (this.span.isPresent()) {
            Span span = this.span.get();

            span.setStatus(StatusCode.OK);
            span.end();
        }
    }
}
