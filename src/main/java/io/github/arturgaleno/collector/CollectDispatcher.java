package io.github.arturgaleno.collector;

import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
class CollectDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectDispatcher.class);

    private KubernetesClient k8sClient;

    private EndpointsGetter endpointsGetter;

    private MetricsCallBuilder metricsCallBuilder;

    private WorkerManager workerManager;

    private ResultsCollector resultsCollector;

    private ExecutorService executorService;

    CollectDispatcher(KubernetesClient k8sClient, EndpointsGetter endpointsGetter,
                      MetricsCallBuilder metricsCallBuilder, WorkerManager workerManager,
                      ResultsCollector resultsCollector, ExecutorService executorService) {
        this.k8sClient = k8sClient;
        this.endpointsGetter = endpointsGetter;
        this.metricsCallBuilder = metricsCallBuilder;
        this.workerManager = workerManager;
        this.resultsCollector = resultsCollector;
        this.executorService = executorService;
    }

    @Scheduled(fixedDelayString = "${executionInterval:1000}")
    void execute() {

        List<String> servicesToCollect = workerManager.servicesToCollect();

        List<Service> services = k8sClient.services().list().getItems();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Dispatcher {} services to collect", servicesToCollect != null ? servicesToCollect.size() : 0);
        }

        if (servicesToCollect != null) {
            services.stream()
                    .filter(service -> servicesToCollect.contains(service.getMetadata().getName()))
                    .flatMap(this::getAddressesStream)
                    .map(endpointAddress -> new MetricsWorker(metricsCallBuilder, endpointAddress))
                    .map(MetricsWorker::callMetricsEndpoint)
                    .collect(Collectors.toList())
                    .forEach(stringCompletableFuture ->
                            stringCompletableFuture.whenCompleteAsync(completeFeature(), executorService)
                    );
        }
    }

    private Stream<? extends AbstractMap.SimpleEntry<String, EndpointAddress>> getAddressesStream(Service service) {
        Endpoints endpoints = endpointsGetter.getEndpointsFrom(service);

        return endpoints.getSubsets().stream()
                .flatMap(endpointSubset -> endpointSubset.getAddresses().stream())
                .map(endpointAddress -> new AbstractMap.SimpleEntry<>(service.getMetadata().getName(), endpointAddress));
    }

    private BiConsumer<AbstractMap.SimpleEntry<String, String>, Throwable> completeFeature() {
        return (result, exception) -> {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("{} handling response of service {}", Thread.currentThread().getName(), result.getKey());
            }

            if (result != null ) {
                if (result.getValue() != null && !result.getValue().isEmpty()) {
                    resultsCollector.putMetric(result);
                }
                workerManager.returnToList(System.currentTimeMillis(), result.getKey());
            }
        };
    }
}
