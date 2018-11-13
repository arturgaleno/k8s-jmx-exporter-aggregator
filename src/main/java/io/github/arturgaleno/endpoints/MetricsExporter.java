package io.github.arturgaleno.endpoints;

import io.fabric8.kubernetes.client.KubernetesClient;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
public class MetricsExporter {

    @Autowired
    private KubernetesClient k8sClient;

    @GetMapping(path = "/metrics", produces = "text/plain")
    public String export() throws IOException {

//        List<Endpoints> endpoints = k8sClient.endpoints().list().getItems();
//
//        endpoints.parallelStream()
//                .flatMap(e -> e.getSubsets().parallelStream());


        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(1, TimeUnit.SECONDS)
                .writeTimeout(1, TimeUnit.SECONDS)
                .build();

        String url = "http://localhost:1111/metrics";
        Call call = okHttpClient.newCall(new Request.Builder()
                .get()
                .url(url)
                .build());

        Response response = call.execute();

        InputStream inputStream = response.body().byteStream();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        Pattern p1 = Pattern.compile("^\\w+([{].*,)");
        Pattern p2 = Pattern.compile("^\\w+(\\s)");
        String collect = bufferedReader.lines()
                .map(l -> {
                    Matcher m = p1.matcher(l);
                    StringBuffer s = new StringBuffer();
                    while (m.find()) {
                        m.appendReplacement(s, m.group(1) + "server='" + url + "',}");
                        l = l.replaceAll("\\{.*}",  s.toString());
                    }
                    Matcher m2 = p2.matcher(l);
                    while (m2.find()) {
                        l = l.replaceAll("\\s", "{server='" + url + "',} ");
                    }
                    return l + "\n";
                })
                .collect(Collectors.joining());


        return collect;
    }
}