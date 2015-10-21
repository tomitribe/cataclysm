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

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.tomitribe.crest.api.Command;
import org.tomitribe.crest.api.Default;
import org.tomitribe.crest.api.Option;
import org.tomitribe.util.Files;
import org.tomitribe.util.Hex;
import org.tomitribe.util.IO;
import org.tomitribe.util.Longs;
import org.tomitribe.util.PrintString;
import org.tomitribe.util.hash.XxHash64;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) throws Exception {
        new Main().run(10, new File("/tmp/issues"), Scenario.RefreshGrant);
    }

    private final Map<String, State> counts = new ConcurrentHashMap<>();
    private File logs;

    @Command
    public void run(@Option("threads") @Default("10") final int threads,
                    @Option("logs") @Default("/tmp/issues") final File logs,
                    @Option("logs") @Default("ResourceOwnerPasswordGrant") final Scenario scenario) throws Exception {

        this.logs = logs;
        Files.mkdir(this.logs);

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::printStatus, 2, 2, TimeUnit.SECONDS);

        final Callable<Response> test = scenario.call();

        for (int i = 0; i < threads; i++) {
            final Caller caller = new Caller(() -> {
                exec(test);
            });

            final Thread thread = new Thread(caller);
            thread.start();
        }
    }

    private void printStatus() {
        System.out.print("\r" + States.printStates(counts));
    }

    private void exec(final Callable<Response> callable) {
        final long start = System.nanoTime();
        try {

            final Response response = callable.call();
            final int statusCode = response.getHttpResponse().getStatusLine().getStatusCode();
            final String name = statusCode + "";

            if (ok(statusCode)) {

                count(name, time(start));

            } else {

                final String format = format(response);

                count(log(name, format), time(start));

            }

        } catch (Throwable t) {
            final String name = t.getClass().getSimpleName();
            count(log(name, format(t)), time(start));
        }
    }

    private String format(Response r) {
        final PrintString out = new PrintString();

        // HTTP/1.1 403 Forbidden
        out.println(r.getHttpResponse().getStatusLine());

        // headers (minus date so it hashes with stable result)
        Stream.of(r.getHttpResponse().getAllHeaders())
                .filter(header -> !"Date".equalsIgnoreCase(header.getName()))
                .map(Header::toString)
                .sorted()
                .forEach(out::println);

        // Content
        out.println();
        out.println(r.getContent());

        return out.toString();
    }

    private static long time(long start) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }

    private String count(String name, long time) {
        counts.computeIfAbsent(name, aLong -> new State(name)).count(time);
        return name;
    }

    private String log(String name, String content) {
        final String hex = hash(content);
        final String id = hex + " " + name;
        final File file = new File(logs, id);

        if (file.exists()) {
            file.setLastModified(System.currentTimeMillis());
        } else {
            store(content, file);
        }

        return id;
    }

    private void store(String content, File file) {
        try {
            IO.copy(IO.read(content), file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String format(HttpResponse response, String content) {
        final PrintString out = new PrintString();

        // HTTP/1.1 403 Forbidden
        out.println(response.getStatusLine());

        // headers (minus date so it hashes with stable result)
        Stream.of(response.getAllHeaders())
                .filter(header -> !"Date".equalsIgnoreCase(header.getName()))
                .map(Header::toString)
                .sorted()
                .forEach(out::println);

        // Content
        out.println();
        out.println(content);

        return out.toString();
    }

    private String hash(String s) {
        final long hash = XxHash64.hash(s);
        return Hex.toString(Longs.toBytes(hash));
    }

    private String format(Throwable t) {
        final PrintString out = new PrintString();
        t.printStackTrace(out);
        return out.toString();
    }

    private boolean ok(int statusCode) {
        return statusCode >= 200 && statusCode <= 299;
    }

    private class Caller implements Runnable {
        final AtomicBoolean run = new AtomicBoolean(true);
        final CountDownLatch finished = new CountDownLatch(1);
        private final Runnable runnable;

        public Caller(Runnable runnable) {
            this.runnable = runnable;
        }

        public AtomicBoolean getRun() {
            return run;
        }

        public CountDownLatch getFinished() {
            return finished;
        }

        @Override
        public void run() {

            try {
                while (run.get()) {
                    runnable.run();
                }
            } finally {
                finished.countDown();
            }

        }
    }
}
