package io.github.arturgaleno.collector;

import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointAddressBuilder;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Test;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class MetricsWorkerTest {

    @Test
    public void shouldAddLabels() throws IOException, ExecutionException, InterruptedException {

        MockWebServer mockWebServer = new MockWebServer();
        MockResponse response = new MockResponse();
        response.setBody("a_b_c 1.0" + System.lineSeparator() + "x_y_z{label=foo,} 1.0");
        mockWebServer.enqueue(response);
        mockWebServer.url("/metrics");

        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

        EndpointAddress endpoint = new EndpointAddressBuilder()
                .withIp(mockWebServer.getHostName())
                .withNewTargetRef()
                .withName("pod_name")
                .withNamespace("namespace")
                .endTargetRef()
                .build();

        AbstractMap.SimpleEntry<String, EndpointAddress> entry = new AbstractMap.SimpleEntry<>("service", endpoint);

        MetricsCallBuilder metricsCallBuilder = new MetricsCallBuilder(mockWebServer.getPort(),
                "/metrics", "http", okHttpClient);

        MetricsWorker metricsWorker = new MetricsWorker(metricsCallBuilder, entry);

        CompletableFuture<AbstractMap.SimpleEntry<String, String>> future = metricsWorker.callMetricsEndpoint();

        AbstractMap.SimpleEntry<String, String> simpleEntry = future.get();

        assertEquals("service", simpleEntry.getKey());

        String expectedString = "a_b_c{pod=pod_name,namespace=namespace,service=service} 1.0" +
                System.lineSeparator() +
                "x_y_z{label=foo,pod=pod_name,namespace=namespace,service=service} 1.0";

        String actualString = simpleEntry.getValue().replaceAll("timestamp=\\d+,", "").trim();

        assertEquals(expectedString, actualString);
    }
}
