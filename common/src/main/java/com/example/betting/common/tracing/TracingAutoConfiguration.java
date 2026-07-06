package com.example.betting.common.tracing;

import io.micrometer.tracing.otel.bridge.OtelBaggageManager;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelPropagator;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetrySdkAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 분산추적 익스포터 구성.
 * Boot 4.1 은 OTLP "추적" 익스포터를 자동 구성하지 않으므로(3.x 의 management.otlp.tracing 제거),
 * OTLP SpanExporter + SdkTracerProvider 를 명시적으로 제공한다. OpenTelemetrySdkAutoConfiguration 이
 * 이 SdkTracerProvider 를 받아 OpenTelemetrySdk 를 만들고, Micrometer 브리지가 그 위에서 스팬을 만든다.
 */
@AutoConfiguration(before = OpenTelemetrySdkAutoConfiguration.class)
public class TracingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    SpanExporter otlpSpanExporter(
            @Value("${management.otlp.tracing.endpoint:http://localhost:4318/v1/traces}") String endpoint) {
        return OtlpHttpSpanExporter.builder().setEndpoint(endpoint).build();
    }

    @Bean
    @ConditionalOnMissingBean
    SdkTracerProvider sdkTracerProvider(
            SpanExporter spanExporter,
            @Value("${spring.application.name:app}") String applicationName) {
        Resource resource = Resource.getDefault().toBuilder()
                .put(AttributeKey.stringKey("service.name"), applicationName)
                .build();
        return SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .setSampler(Sampler.parentBased(Sampler.alwaysOn()))
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    ContextPropagators contextPropagators() {
        return ContextPropagators.create(TextMapPropagator.composite(
                W3CTraceContextPropagator.getInstance(),
                W3CBaggagePropagator.getInstance()));
    }

    // --- OTel SDK ↔ Micrometer Tracing 브리지 ---
    // Boot 4.1 은 이 브리지 빈들을 자동 구성하지 않으므로 직접 만든다.
    // 이게 없으면 Micrometer Tracer 가 noop 으로 폴백되어 스팬이 생성되지 않는다.

    @Bean
    @ConditionalOnMissingBean
    OtelCurrentTraceContext otelCurrentTraceContext() {
        return new OtelCurrentTraceContext();
    }

    @Bean
    @ConditionalOnMissingBean(io.micrometer.tracing.Tracer.class)
    OtelTracer micrometerOtelTracer(OpenTelemetry openTelemetry, OtelCurrentTraceContext currentTraceContext) {
        io.opentelemetry.api.trace.Tracer otelTracer = openTelemetry.getTracer("com.example.betting");
        OtelTracer.EventPublisher publisher = event -> {
        };
        return new OtelTracer(otelTracer, currentTraceContext, publisher,
                new OtelBaggageManager(currentTraceContext, List.of(), List.of()));
    }

    @Bean
    @ConditionalOnMissingBean(io.micrometer.tracing.propagation.Propagator.class)
    OtelPropagator otelPropagator(ContextPropagators contextPropagators, OpenTelemetry openTelemetry) {
        return new OtelPropagator(contextPropagators, openTelemetry.getTracer("com.example.betting"));
    }
}
