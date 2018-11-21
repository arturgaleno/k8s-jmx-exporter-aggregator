package io.github.arturgaleno.collector;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Class created to mock the query for endpoint when execute test class,
 * because fabric8 test k8s client is not creating endpoints
 */
@Component
class EndpointsGetter {

    private KubernetesClient k8sClient;

    @Autowired
    EndpointsGetter(KubernetesClient k8sClient) {
        this.k8sClient = k8sClient;
    }

    Endpoints getEndpointsFrom(Service service) {
        return k8sClient.endpoints()
                .withName(service.getMetadata().getName())
                .get();
    }
}
