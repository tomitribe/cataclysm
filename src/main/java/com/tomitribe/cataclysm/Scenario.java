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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.johnzon.mapper.Mapper;
import org.apache.johnzon.mapper.MapperBuilder;
import org.tomitribe.util.IO;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

public enum Scenario implements Callable<Callable<Response>> {

    ResourceOwnerPasswordGrant() {
        @Override
        public Callable<Response> call() throws Exception {
            final URI tokenEndpoint = URI.create("https://soa-test.starbucks.com/soa-iag/oauth/token");
            final HttpClient httpClient = HttpClients.createDefault();
            final List<NameValuePair> form = new ArrayList<>();
            form.add(new BasicNameValuePair("client_id", "s-partner-app"));
            form.add(new BasicNameValuePair("client_secret", "UI@imp0P"));
            form.add(new BasicNameValuePair("username", "US1920648"));
            form.add(new BasicNameValuePair("password", "Pwd$100012"));
            form.add(new BasicNameValuePair("grant_type", "password"));

            HttpPost httppost = new HttpPost(tokenEndpoint);
            httppost.setEntity(new UrlEncodedFormEntity(form));
            httppost.addHeader("Content-Type", "application/x-www-form-urlencoded");

            return () -> {
                final HttpResponse response = httpClient.execute(httppost);
                final HttpEntity responseEntity = response.getEntity();
                final String content = EntityUtils.toString(responseEntity);
                return new Response(response, content);
            };
        }
    },

    RefreshGrant() {
        @Override
        public Callable<Response> call() throws Exception {

            final AtomicReference<Response> lastCall = new AtomicReference<>();

            final URI tokenEndpoint = URI.create("https://soa-test.starbucks.com/soa-iag/oauth/token");
            final HttpClient httpClient = HttpClients.createDefault();

            return () -> {

                if (lastCall.get() == null || lastCall.get().getHttpResponse().getStatusLine().getStatusCode() != 200) {
                    final Response response = ResourceOwnerPasswordGrant.call().call();
                    lastCall.set(response);

                    if (response.getHttpResponse().getStatusLine().getStatusCode() != 200) {
                        lastCall.set(null);
                        return response;
                    }
                }

                final Response lastResponse = lastCall.getAndSet(null);

                final Mapper mapper = new MapperBuilder().build();
                final Tokens tokens = mapper.readObject(IO.read(lastResponse.getContent()), Tokens.class);

                final List<NameValuePair> form = new ArrayList<>();
                final String refresh_token = tokens.getRefresh_token();

                if (refresh_token == null || refresh_token.length() == 0) {
                    throw new IllegalStateException("Empty refresh_token returned");
                }

                form.add(new BasicNameValuePair("refresh_token", refresh_token));
                form.add(new BasicNameValuePair("grant_type", "refresh_token"));

                final HttpPost httppost = new HttpPost(tokenEndpoint);
                httppost.setEntity(new UrlEncodedFormEntity(form));
                httppost.addHeader("Content-Type", "application/x-www-form-urlencoded");

                final HttpResponse response = httpClient.execute(httppost);
                final HttpEntity responseEntity = response.getEntity();
                final String content = EntityUtils.toString(responseEntity);

                final Response r = new Response(response, content);
                lastCall.set(r);
                return r;
            };
        }
    },

    BadGrantType() {
        @Override
        public Callable<Response> call() throws Exception {
            final URI tokenEndpoint = URI.create("https://soa-test.starbucks.com/soa-iag/oauth/token");
            final HttpClient httpClient = HttpClients.createDefault();
            final List<NameValuePair> form = new ArrayList<>();
            form.add(new BasicNameValuePair("grant_type", "passwords"));

            HttpPost httppost = new HttpPost(tokenEndpoint);
            httppost.setEntity(new UrlEncodedFormEntity(form));
            httppost.addHeader("Content-Type", "application/x-www-form-urlencoded");

            return () -> {
                final HttpResponse response = httpClient.execute(httppost);
                final HttpEntity responseEntity = response.getEntity();
                final String content = EntityUtils.toString(responseEntity);
                return new Response(response, content);
            };
        }
    };
}
