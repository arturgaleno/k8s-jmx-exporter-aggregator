package io.github.arturgaleno.collector;

import io.fabric8.kubernetes.api.model.EndpointAddress;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.AbstractMap;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class MetricsWorker {


    private static final String LABELS_TO_APPEND = "timestamp=%s,pod=%s,namespace=%s,service=%s}";

    private static final Pattern LABELED_PATTERN = Pattern.compile("^\\w+([{].*,)");

    private static final Pattern NOT_LABELED_PATTERN = Pattern.compile("^\\w+(\\s)");

    private MetricsCallBuilder metricsCallBuilder;

    private AbstractMap.SimpleEntry<String, EndpointAddress> endpointAddressEntry;

    MetricsWorker(MetricsCallBuilder metricsCallBuilder,
                  AbstractMap.SimpleEntry<String, EndpointAddress> endpointAddressEntry) {
        this.metricsCallBuilder = metricsCallBuilder;
        this.endpointAddressEntry = endpointAddressEntry;
    }

    CompletableFuture<AbstractMap.SimpleEntry<String, String>> callMetricsEndpoint() {
        Call call = metricsCallBuilder.buildCall(endpointAddressEntry.getValue().getIp());

        FutureResponse futureResponse = new FutureResponse(endpointAddressEntry);

        call.enqueue(futureResponse);

        return futureResponse.getResponseCompletableFuture();
    }

    private static class FutureResponse implements Callback {

        private final CompletableFuture<AbstractMap.SimpleEntry<String, String>> responseCompletableFuture = new CompletableFuture<>();

        private final AbstractMap.SimpleEntry<String, EndpointAddress> endpointAddressEntry;

        private FutureResponse(AbstractMap.SimpleEntry<String, EndpointAddress> endpointAddressEntry) {
            this.endpointAddressEntry = endpointAddressEntry;
        }

        @Override
        public void onFailure(Call call, IOException e) {
            responseCompletableFuture.complete(new AbstractMap.SimpleEntry<>(endpointAddressEntry.getKey(), ""));
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            ResponseBody responseBody = response.body();

            if (response.isSuccessful() && responseBody != null) {
                responseCompletableFuture.complete(handleMetricsResponse(responseBody));
            } else {
                responseCompletableFuture.complete(new AbstractMap.SimpleEntry<>(endpointAddressEntry.getKey(), ""));
            }
        }

        private AbstractMap.SimpleEntry<String, String> handleMetricsResponse(ResponseBody responseBody) {

            String formattedLabels = String.format(LABELS_TO_APPEND, System.currentTimeMillis(),
                    endpointAddressEntry.getValue().getTargetRef().getName(),
                    endpointAddressEntry.getValue().getTargetRef().getNamespace(),
                    endpointAddressEntry.getKey());

            InputStream inputStream = responseBody.byteStream();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            String metricsReturn = bufferedReader.lines()
                    .map(l -> {

                        if (l.startsWith("#")) return l + "\n";

                        Matcher m = LABELED_PATTERN.matcher(l);
                        StringBuffer s = new StringBuffer();
                        while (m.find()) {
                            m.appendReplacement(s, m.group(1) + formattedLabels);
                            l = l.replaceAll("\\{.*}", s.toString());
                        }
                        Matcher m2 = NOT_LABELED_PATTERN.matcher(l);
                        while (m2.find()) {
                            l = l.replaceAll("\\s", "{" + formattedLabels + " ");
                        }
                        return l + "\n";
                    })
                    .collect(Collectors.joining());

            responseBody.close();

            return new AbstractMap.SimpleEntry<>(endpointAddressEntry.getKey(), metricsReturn);
        }

        CompletableFuture<AbstractMap.SimpleEntry<String, String>> getResponseCompletableFuture() {
            return responseCompletableFuture;
        }
    }
}
