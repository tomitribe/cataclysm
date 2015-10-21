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
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.tomitribe.util.Files;
import org.tomitribe.util.Hex;
import org.tomitribe.util.IO;
import org.tomitribe.util.Longs;
import org.tomitribe.util.PrintString;
import org.tomitribe.util.hash.XxHash64;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class Main {


    public static void main(String[] args) throws Exception {
        new Main().run();
    }

    private final Map<String, State> counts = new ConcurrentHashMap<>();
    private final File logs;

    public Main() {
        logs = new File("/tmp/issues");
        Files.mkdir(logs);
    }

    public void run() throws Exception {

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::printStatus, 2, 2, TimeUnit.SECONDS);

        final CloseableHttpClient httpClient = HttpClients.createDefault();

        final URI tokenEndpoint = new URI("https://soa-test.starbucks.com/soa-iag/oauth/token");
        final List<NameValuePair> form = new ArrayList<>();
        form.add(new BasicNameValuePair("client_id", "s-partner-app"));
        form.add(new BasicNameValuePair("client_secret", "UI@imp0P"));
//        form.add(new BasicNameValuePair("username", "UI9990648"));
        form.add(new BasicNameValuePair("username", "US1920648"));
        form.add(new BasicNameValuePair("password", "Pwd$100012"));
        form.add(new BasicNameValuePair("grant_type", "passwords"));

        final UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form);

        final HttpPost httppost = new HttpPost(tokenEndpoint);
        httppost.setEntity(entity);
        httppost.addHeader("Content-Type", "application/x-www-form-urlencoded");

        while (true) {
            exec(httpClient, httppost);
        }
    }

    private void printStatus() {
        System.out.print("\r" + States.printStates(counts));
    }

    private void exec(CloseableHttpClient httpClient, HttpPost httppost) {
        try {
            final HttpResponse response = httpClient.execute(httppost);

            final StatusLine statusLine = response.getStatusLine();
            final int statusCode = statusLine.getStatusCode();

            final String name = statusCode + "";

            final HttpEntity responseEntity = response.getEntity();
            final String content = EntityUtils.toString(responseEntity);

            if (ok(statusCode)) {

                count(name);

            } else {

                count(log(name, format(response, content)));

            }

        } catch (Throwable t) {
            final String name = t.getClass().getSimpleName();
            count(log(name, format(t)));
        }
    }

    private String count(String name) {
        counts.computeIfAbsent(name, aLong -> new State(name)).incrementAndGet();
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
}
