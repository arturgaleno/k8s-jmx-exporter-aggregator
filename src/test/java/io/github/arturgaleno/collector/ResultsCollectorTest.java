package io.github.arturgaleno.collector;

import org.junit.Test;

import java.util.AbstractMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ResultsCollectorTest {

    @Test
    public void shouldReturnNullWhenHasNotMetrics() {
        ResultsCollector resultsCollector = new ResultsCollector();
        assertNull(resultsCollector.pollMetric());
    }

    @Test
    public void shouldReturnMetrics() {
        ResultsCollector resultsCollector = new ResultsCollector();
        resultsCollector.putMetric(new AbstractMap.SimpleEntry<>("service", "metric"));
        assertTrue(resultsCollector.hasResults());
        assertEquals("metric", resultsCollector.pollMetric());
    }
}
