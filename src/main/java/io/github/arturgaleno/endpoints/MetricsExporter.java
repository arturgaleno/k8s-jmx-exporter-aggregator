package io.github.arturgaleno.endpoints;

import io.github.arturgaleno.collector.ResultsCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetricsExporter {

    @Autowired
    private ResultsCollector resultsCollector;

    @GetMapping(path = "/metrics", produces = "text/plain")
    public String export() {

        resultsCollector.lockForResultFlush();

        StringBuilder stringBuilder = new StringBuilder();

        while (resultsCollector.hasResults()) {
            String metric = resultsCollector.pollMetric();
            if (metric != null) {
                stringBuilder.append(metric);
            }
        }

        resultsCollector.unlockForResultFlush();

        return stringBuilder.toString();
    }
}