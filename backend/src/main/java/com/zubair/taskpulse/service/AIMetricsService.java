package com.zubair.taskpulse.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AIMetricsService {

    private final MeterRegistry meterRegistry;

    public void recordRequest() {
        Counter.builder("taskpulse.ai.requests")
                .description("Total AI extraction requests")
                .register(meterRegistry)
                .increment();
    }

    public void recordSuccess() {
        Counter.builder("taskpulse.ai.success")
                .description("Successful AI extractions")
                .register(meterRegistry)
                .increment();
    }

    public void recordFailure() {
        Counter.builder("taskpulse.ai.failure")
                .description("Failed AI extractions")
                .register(meterRegistry)
                .increment();
    }

    public void recordCacheHit() {
        Counter.builder("taskpulse.ai.cache.hits")
                .description("AI extraction cache hits")
                .register(meterRegistry)
                .increment();
    }

    public void recordCacheMiss() {
        Counter.builder("taskpulse.ai.cache.misses")
                .description("AI extraction cache misses")
                .register(meterRegistry)
                .increment();
    }

    public void recordRateLimit() {
        Counter.builder("taskpulse.ai.rate_limited")
                .description("Rate limited AI requests")
                .register(meterRegistry)
                .increment();
    }

    public void recordLatency(long milliseconds) {
        Timer.builder("taskpulse.ai.latency")
                .description("AI extraction latency")
                .register(meterRegistry)
                .record(milliseconds, TimeUnit.MILLISECONDS);
    }
}