package io.github.arturgaleno.endpoints;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MetricsExporterTest {


    @Rule
    public KubernetesServer server = new KubernetesServer(true, true);


    @Test
    public void test() {

        KubernetesClient client = server.getClient();


        Pod pod1 = new PodBuilder()
                .withNewMetadata()
                    .withName("pod1")
                    .withLabels(Collections.singletonMap("app", "app1"))
                .endMetadata()
                .withNewSpec()
                    .withContainers(new ContainerBuilder()
                            .withName("container")
                            .withImage("nginx")
                            .withPorts(new ContainerPortBuilder()
                                    .withContainerPort(8080)
                                    .build())
                            .build()
                    )
                .endSpec()
                .build();

        Pod pod2 = new PodBuilder()
                .withNewMetadata()
                    .withName("pod2")
                    .withLabels(Collections.singletonMap("app", "app2"))
                .endMetadata()
                .build();


        Service service1 = new ServiceBuilder()
                .withNewMetadata()
                    .withName("svc1")
                .and()
                .withNewSpec()
                    .addNewPort().withProtocol("TCP").withName("port").withNewPort(80)
                    .withNewTargetPort().withIntVal(8080).and()
                .and()
                .withSelector(Collections.singletonMap("app", "app1"))
                .withType("ClusterIP")
                .endSpec()
                .build();

        Endpoints endpoint1 = new EndpointsBuilder()
                .withSubsets(
                        new EndpointSubsetBuilder()
                                .withAddresses(
                                        new EndpointAddressBuilder()
                                                .withIp("127.0.0.1").build()
                                ).
                                withPorts(
                                        new EndpointPortBuilder().withPort(8080).withProtocol("TCP").build()
                                ).build()
                ).build();

        client.pods().create(pod1);
        client.services().create(service1);


        for (Endpoints endpoints : client.endpoints().list().getItems()) {
            for (EndpointSubset endpointsSubset : endpoints.getSubsets()) {
                System.out.println(endpointsSubset.toString());
            }
        }
    }
}