package io.github.arturgaleno.collector;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class WorkerManagerTest {

    @Test
    public void shouldReturnServicesToCollect() {

        List<String> services = new ArrayList<>();
        services.add("service1");
        services.add("service2");
        services.add("service3");
        services.add("service4");
        services.add("service5");
        services.add("service6");

        WorkerManager workerManager = new WorkerManager(5000, TimeUnit.MILLISECONDS, services);

        List<String> servicesToCollect = workerManager.servicesToCollect();

        assertNotNull(servicesToCollect);
        assertEquals(6, servicesToCollect.size());
    }

    @Test
    public void shouldReturnServicesToCollectInTimestampLimits() {

        List<String> services = new ArrayList<>();
        services.add("service1");
        services.add("service2");
        services.add("service3");
        services.add("service4");
        services.add("service5");
        services.add("service6");

        WorkerManager workerManager = new WorkerManager(5000, TimeUnit.MILLISECONDS, services);

        List<String> servicesToCollect = workerManager.servicesToCollect();

        workerManager.returnToList(System.currentTimeMillis(), servicesToCollect.get(0));
        workerManager.returnToList(System.currentTimeMillis(), servicesToCollect.get(1));
        workerManager.returnToList(System.currentTimeMillis(), servicesToCollect.get(2));
        workerManager.returnToList(System.currentTimeMillis() - 5000, servicesToCollect.get(3));
        workerManager.returnToList(System.currentTimeMillis() - 5000, servicesToCollect.get(4));
        workerManager.returnToList(System.currentTimeMillis() - 5000, servicesToCollect.get(5));

        servicesToCollect = workerManager.servicesToCollect();

        assertNotNull(servicesToCollect);
        assertEquals(3, servicesToCollect.size());
        assertEquals(services.get(3), servicesToCollect.get(0));
        assertEquals(services.get(4), servicesToCollect.get(1));
        assertEquals(services.get(5), servicesToCollect.get(2));
    }

    @Test
    public void shouldReturnNullOnEmptyServiceList() {
        WorkerManager workerManager = new WorkerManager(5000, TimeUnit.MILLISECONDS, new ArrayList<>());

        List<String> servicesToCollect = workerManager.servicesToCollect();

        assertNull(servicesToCollect);
    }
}