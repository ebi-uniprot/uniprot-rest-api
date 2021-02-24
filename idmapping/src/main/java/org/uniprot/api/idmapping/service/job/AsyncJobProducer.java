package org.uniprot.api.idmapping.service.job;

import java.util.concurrent.BlockingQueue;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.uniprot.api.idmapping.model.IdMappingJob;

/**
 * @author sahmad
 * @created 23/02/2021
 */
@Component
public class AsyncJobProducer {
    private final BlockingQueue<IdMappingJob> queue;

    public AsyncJobProducer(BlockingQueue<IdMappingJob> queue) {
        this.queue = queue;
    }

    @Async("threadPoolTaskExecutor")
    public void enqueueJob(IdMappingJob idMappingJob) throws InterruptedException {
        this.queue.put(idMappingJob);
    }
}
