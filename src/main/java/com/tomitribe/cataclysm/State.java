/*
 * Tomitribe Confidential
 *
 * Copyright Tomitribe Corporation. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.tomitribe.cataclysm;

import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;

import java.util.concurrent.atomic.AtomicInteger;

public class State {

    private final String name;
    private final AtomicInteger count = new AtomicInteger(0);
    private final SynchronizedDescriptiveStatistics statistics = new SynchronizedDescriptiveStatistics(2000);

    public State(String name) {
        this.name = name;
    }

    public State(String name, int count) {
        this.name = name;
        this.count.set(count);
    }

    public void count(final long time) {
        count.incrementAndGet();
        statistics.addValue(time);
    }

    public String getName() {
        return name;
    }

    public int getCount() {
        return count.get();
    }

    public int incrementAndGet() {
        return count.incrementAndGet();
    }

    @Override
    public String toString() {
        return String.format("%s (%s %sms)", name, count.get(), (long) statistics.getMean());
    }

}
