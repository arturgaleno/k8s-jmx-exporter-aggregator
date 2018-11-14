package io.github.arturgaleno.configurations;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KubernetesClientConfig {

    @Bean
    public KubernetesClient k8sClient(@Value("${k8s.master:'http://localhost'}") String masterUrl,
                                      @Value("${k8s.namespace:default}") String nameSpace) {

        Config config = new ConfigBuilder()
                .withMasterUrl(masterUrl)
                .withNamespace(nameSpace)
                .build();

        return new DefaultKubernetesClient(config);
    }
}
