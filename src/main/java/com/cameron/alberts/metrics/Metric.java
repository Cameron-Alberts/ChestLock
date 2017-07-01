package com.cameron.alberts.metrics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Metric implements AutoCloseable {
    private static final Gson GSON = new GsonBuilder().create();

    private static PrintWriter printWriter;

    static {
        try {
            printWriter = new PrintWriter(new FileWriter(new File("metrics.json"), true));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String metricName;
    private double metricValue;
    private String metricType;
    private String unit;
    private String version;

    Metric(final String version, final String metricType) {
        this.version = version;
        this.metricType = metricType;
    }

    void setMetricName(final String metricName) {
        this.metricName = metricName;
    }

    void setMetricValue(final double metricValue, final String unit) {
        this.metricValue = metricValue;
        this.unit = unit;
    }

    @Override
    public void close() {
        Metric.writeMetric(this);
    }

    private static void writeMetric(final Metric metric) {
        printWriter.println(GSON.toJson(metric));
        printWriter.flush();
    }
}
