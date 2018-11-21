package io.github.arturgaleno.collector;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CollectDispatcherTest {

    @Rule
    public KubernetesServer server = new KubernetesServer(true, true);

    @Test
    public void shouldCollectMetrics() {

        MockWebServer mockWebServer = new MockWebServer();
        MockResponse response = new MockResponse();
        response.setBody("a_b_c 1.0" + System.lineSeparator() + "x_y_z{label=foo,} 1.0");
        mockWebServer.enqueue(response);
        mockWebServer.url("/metrics");

        Service service = new ServiceBuilder()
                .withNewMetadata()
                .withName("service")
                .and()
                .withNewSpec()
                .addNewPort().withProtocol("TCP").withName("port").withNewPort(80)
                .withNewTargetPort().withIntVal(mockWebServer.getPort()).and()
                .and()
                .withSelector(Collections.singletonMap("app", "app1"))
                .withType("ClusterIP")
                .endSpec()
                .build();

        EndpointAddress endpointAddress = new EndpointAddressBuilder()
                .withIp(mockWebServer.getHostName())
                .withNewTargetRef()
                .withName("pod_name")
                .endTargetRef()
                .build();

        Endpoints endpoints = new EndpointsBuilder()
                .withNewMetadata()
                .withNamespace("test")
                .withName("service")
                .endMetadata()
                .addNewSubset()
                .withAddresses(endpointAddress)
                .endSubset()
                .build();


        KubernetesClient k8sClient = server.getClient();

        k8sClient.services().create(service);

        EndpointsGetter endpointsGetter = new EndpointsGetter(k8sClient);

        endpointsGetter = Mockito.spy(endpointsGetter);

        Mockito.when(endpointsGetter.getEndpointsFrom(service))
                .thenAnswer((Answer<Endpoints>) invocationOnMock -> endpoints);

        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

        WorkerManager workerManager = new WorkerManager(1, TimeUnit.MILLISECONDS,
                Collections.singletonList("service"));

        ResultsCollector resultsCollector = new ResultsCollector();

        ExecutorService executorService = Executors.newFixedThreadPool(1);

        MetricsCallBuilder metricsCallBuilder = new MetricsCallBuilder(mockWebServer.getPort(),
                "/metrics", "http", okHttpClient);

        CollectDispatcher collectDispatcher = new CollectDispatcher(k8sClient, endpointsGetter, metricsCallBuilder,
                workerManager, resultsCollector, executorService);

        collectDispatcher.execute();

        await().until(resultsCollector::hasResults);

        assertTrue(resultsCollector.hasResults());
        assertNotNull(resultsCollector.pollMetric());
    }
}