package org.example;

import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final long timeIntervalInMillis;
    private final int requestLimit;
    private int currentRequests;
    private long lastRequestTimeMillis;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        if (timeUnit == null || requestLimit <= 0) {
            throw new IllegalArgumentException("Invalid time unit or request limit");
        }
        this.timeIntervalInMillis = timeUnit.toMillis(1);
        this.requestLimit = requestLimit;
        this.currentRequests = 0;
        this.lastRequestTimeMillis = System.currentTimeMillis();
    }

    public synchronized void createDocument(Object document, String signature) {
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - lastRequestTimeMillis > timeIntervalInMillis) {
            currentRequests = 0;
            lastRequestTimeMillis = currentTimeMillis;
        }

        while (currentRequests >= requestLimit) {
            try {
                wait(timeIntervalInMillis);
                currentRequests = 0;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }


        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost("https://ismp.crpt.ru/api/v3/lk/documents/create");
            httpPost.setHeader("Content-Type", "application/json");

            Gson gson = new Gson();
            String jsonDocument = gson.toJson(document);

            StringEntity requestEntity = new StringEntity(jsonDocument, ContentType.APPLICATION_JSON);
            httpPost.setEntity(requestEntity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                HttpEntity responseEntity = response.getEntity();
                String jsonResponse = EntityUtils.toString(responseEntity);
                System.out.println(jsonResponse);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        currentRequests++;
        lastRequestTimeMillis = System.currentTimeMillis();
        notifyAll();
    }
}
