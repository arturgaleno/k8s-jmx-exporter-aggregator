package io.github.arturgaleno.collector;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class MetricsCallBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsCallBuilder.class);

    private OkHttpClient okHttpClient;

    private String metricsUrl;

    @Autowired
    MetricsCallBuilder( @Value("${metricsPort:1111}") Integer metricsPort,
                        @Value("${metricsPath:/metrics}")String metricsPath,
                        @Value("${metricsProtocol:http}")String metricsProtocol,
                        OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
        this.metricsUrl = metricsProtocol + "://%s:" + metricsPort + metricsPath;
    }

    Call buildCall(String ip) {
        String url = String.format(metricsUrl, ip);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Create a call to: {}", url);
        }

        return okHttpClient.newCall(new Request.Builder()
                .get()
                .url(url)
                .build());
    }
}
