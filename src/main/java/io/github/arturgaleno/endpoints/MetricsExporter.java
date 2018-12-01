package io.github.arturgaleno.endpoints;

import io.github.arturgaleno.collector.ResultsCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@RestController
public class MetricsExporter {

    @Autowired
    private ResultsCollector resultsCollector;

    @GetMapping(path = "/metrics", produces = "text/plain")
    public void export(HttpServletResponse httpServletResponse) throws IOException {

        resultsCollector.lockForResultFlush();

        PrintWriter servletResponseWriter = httpServletResponse.getWriter();

        while (resultsCollector.hasResults()) {
            String metric = resultsCollector.pollMetric();
            if (metric != null) {
                servletResponseWriter.write(metric);
            }
        }

        httpServletResponse.setContentType(MediaType.TEXT_PLAIN_VALUE);
        httpServletResponse.setStatus(HttpStatus.OK.value());

        resultsCollector.unlockForResultFlush();
    }
}