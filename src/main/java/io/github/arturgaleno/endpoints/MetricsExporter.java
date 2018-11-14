package io.github.arturgaleno.endpoints;

import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class MetricsExporter {

    private static final String METRICS_URL = "http://%s:1111/metrics";

    private static final String LABELS_TO_APPEND = "ip=%s,pod=%s,namespace=%s,}";

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .readTimeout(1, TimeUnit.SECONDS)
            .writeTimeout(1, TimeUnit.SECONDS)
            .build();

    @Autowired
    private KubernetesClient k8sClient;

    @GetMapping(path = "/metrics", produces = "text/plain")
    public String export() {

        List<Service> services = k8sClient.services().list().getItems();

        return services.parallelStream()
                .flatMap(this::getEndpointSubsetStream)
                .flatMap(endpointSubset -> endpointSubset.getAddresses().parallelStream())
                .map(this::callMetricsEndpoint)
                .collect(Collectors.joining());
    }

    private String callMetricsEndpoint(EndpointAddress endpointAddress) {
        Call call = httpClient.newCall(new Request.Builder()
                .get()
                .url(String.format(METRICS_URL, endpointAddress.getIp()))
                .build());

        try {
            Response response = call.execute();
            ResponseBody responseBody = response.body();

            if (response.isSuccessful() && responseBody != null) {
                return handleMetricsResponse(endpointAddress, responseBody);
            } else {
                return "";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private Stream<? extends EndpointSubset> getEndpointSubsetStream(Service s) {
        Endpoints endpoints = k8sClient.endpoints()
                .withName(s.getMetadata().getName())
                .get();

        return endpoints.getSubsets().parallelStream();
    }

    private String handleMetricsResponse(EndpointAddress endpointAddress, ResponseBody responseBody) {
        String formattedLabels = String.format(LABELS_TO_APPEND, endpointAddress.getIp(),
                endpointAddress.getTargetRef().getName(),
                endpointAddress.getTargetRef().getNamespace());

        InputStream inputStream = responseBody.byteStream();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        Pattern p1 = Pattern.compile("^\\w+([{].*,)");
        Pattern p2 = Pattern.compile("^\\w+(\\s)");

        return bufferedReader.lines()
                .map(l -> {
                    Matcher m = p1.matcher(l);
                    StringBuffer s = new StringBuffer();
                    while (m.find()) {
                        m.appendReplacement(s, m.group(1) + formattedLabels);
                        l = l.replaceAll("\\{.*}", s.toString());
                    }
                    Matcher m2 = p2.matcher(l);
                    while (m2.find()) {
                        l = l.replaceAll("\\s", formattedLabels + " ");
                    }
                    return l + "\n";
                })
                .collect(Collectors.joining());
    }

}