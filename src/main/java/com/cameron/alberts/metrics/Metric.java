package com.cameron.alberts.metrics;

import au.com.bytecode.opencsv.CSVWriter;
import lombok.Data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Data
public class Metric implements AutoCloseable {
    private static CSVWriter writer;

    static {
        try {
            writer = new CSVWriter(new FileWriter(new File("metrics.csv"), true));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String metricName;
    private double metricValue;

    public static Metric getMetric() {
        return new Metric();
    }

    public String[] toArray() {
        String[] array = new String[2];
        array[0] = metricName;
        array[1] = String.valueOf(metricValue);

        return array;
    }

    @Override
    public void close() {
        Metric.writeMetric(this);
    }

    private static void writeMetric(final Metric metric) {
        writer.writeNext(metric.toArray());
        try {
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
