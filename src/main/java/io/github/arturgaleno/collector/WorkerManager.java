package io.github.arturgaleno.collector;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
class WorkerManager {

    private PriorityBlockingQueue<ServiceQueueData> serviceQueue = new PriorityBlockingQueue<>(10);

    private Integer collectInterval;

    private TimeUnit timeUnit;

    WorkerManager(@Value("${collectInterval:5000}") Integer collectInterval,
                  @Value("${collectTimeUnit:MILLISECONDS}") TimeUnit timeUnit,
                  @Value("#{'${services:''}'.split(',')}") List<String> services) {
        this.collectInterval = collectInterval;
        this.timeUnit = timeUnit;
        services.forEach(s -> serviceQueue.offer(new ServiceQueueData(s, System.currentTimeMillis())));
    }

    List<String> servicesToCollect() {

        if (serviceQueue.isEmpty()) return null;

        List<String> servicesToCollect = new ArrayList<>();

        while (!serviceQueue.isEmpty() && serviceQueue.peek().timestamp <= System.currentTimeMillis()) {
            ServiceQueueData serviceQueueData = serviceQueue.poll();
            if (serviceQueueData != null) {
                servicesToCollect.add(serviceQueueData.service);
            }
        }

         return servicesToCollect;
    }

    void returnToList(Long lastCollectTimestamp, String service) {
        serviceQueue.offer(new ServiceQueueData(service, getNextCollectTimestamp(lastCollectTimestamp)));
    }

    private long getNextCollectTimestamp(Long lastCollectTimestamp) {
        return lastCollectTimestamp + TimeUnit.MILLISECONDS.convert(collectInterval, timeUnit);
    }

    private static class ServiceQueueData implements Comparable<ServiceQueueData> {
        String service;
        Long timestamp;

        ServiceQueueData(String service, Long timestamp) {
            this.service = service;
            this.timestamp = timestamp;
        }

        @Override
        public int compareTo(ServiceQueueData o) {
            int result = this.timestamp.compareTo(o.timestamp);

            if (result == 0) {
                result = this.service.compareTo(o.service);
            }

            return result;
        }
    }
}
