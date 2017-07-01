package com.cameron.alberts.metrics;

import com.google.common.base.Stopwatch;

import java.util.concurrent.TimeUnit;

public class TimerMetric extends Metric {
    private static final String VERSION = "1.0";
    private static final String METRIC_TYPE = "TimerMetric";

    private transient Stopwatch stopwatch; // Marked transient to avoid Gson serialization

    private TimerMetric() {
        super(VERSION, METRIC_TYPE);
        stopwatch = Stopwatch.createStarted();
    }

    public static TimerMetric create(final String metricName) {
        TimerMetric metric = new TimerMetric();
        metric.setMetricName(metricName);

        return metric;
    }

    @Override
    public void close() {
        setMetricValue(stopwatch.elapsed(TimeUnit.MICROSECONDS), TimeUnit.MICROSECONDS.name());
        super.close();
    }
}
