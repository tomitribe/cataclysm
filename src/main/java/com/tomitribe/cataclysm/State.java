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

import io.airlift.stats.DecayCounter;
import io.airlift.stats.ExponentialDecay;
import io.airlift.stats.TimedStat;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class State {

    private final String name;
    private final AtomicInteger count = new AtomicInteger(0);
    private DecayCounter tps;
    private TimedStat response;

    public State(String name) {
        this.name = name;
        tps = new DecayCounter(ExponentialDecay.seconds(1));
        response = new TimedStat();
    }

    public State(String name, int count) {
        this.name = name;
        this.count.set(count);
    }

    public void count(final long time) {
        count.incrementAndGet();
        response.addValue(time, TimeUnit.MILLISECONDS);
        tps.add(1);
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
        return String.format("%s (%s %stps %sms)", name, count.get(), (long) tps.getRate(), (long) response.getMean());
    }

}
