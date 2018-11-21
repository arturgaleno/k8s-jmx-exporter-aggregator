package io.github.arturgaleno.collector;

import org.springframework.stereotype.Component;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class ResultsCollector {

    private ConcurrentHashMap<String, String> metricsResults = new ConcurrentHashMap<>();

    private ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock(true);

    private ReentrantReadWriteLock.WriteLock writeLockForResultFlush;

    void putMetric(AbstractMap.SimpleEntry<String, String> metricEntry) {
        ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();
        writeLock.lock();

        metricsResults.put(metricEntry.getKey(), metricEntry.getValue());

        writeLock.unlock();
    }

    public String pollMetric() {
        if (!metricsResults.isEmpty()) {
            Set<Map.Entry<String, String>> entrySet = metricsResults.entrySet();
            Map.Entry<String, String> entry = entrySet.stream().findFirst().orElse(null);
            if (entry != null) {
                metricsResults.remove(entry.getKey());
                return entry.getValue();
            }
        }

        return null;
    }

    public boolean hasResults() {
        return !metricsResults.isEmpty();
    }

    public void lockForResultFlush() {
        writeLockForResultFlush = reentrantReadWriteLock.writeLock();
        writeLockForResultFlush.lock();
    }

    public void unlockForResultFlush() {
        if (writeLockForResultFlush != null && writeLockForResultFlush.isHeldByCurrentThread()) {
            writeLockForResultFlush.unlock();
        }
    }
}
