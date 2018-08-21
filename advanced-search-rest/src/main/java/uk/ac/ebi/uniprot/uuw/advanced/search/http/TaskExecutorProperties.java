package uk.ac.ebi.uniprot.uuw.advanced.search.http;

import lombok.Data;
import org.springframework.core.task.TaskExecutor;

/**
 * Records properties that can be used to configure a {@link TaskExecutor}.
 *
 * Created 23/01/17
 * @author Edd
 */
@Data
public class TaskExecutorProperties {
    static final int DEFAULT_CORE_POOL_SIZE = 3;
    static final int MAX_POOL_SIZE = 15;
    static final int QUEUE_CAPACITY = 100;
    static final int KEEP_ALIVE_SECONDS = 20 * 60;
    static final boolean ALLOW_CORE_THREAD_TIMEOUT = false;
    static final boolean WAIT_FOR_TASKS_TO_COMPLETE_ON_SHUTDOWN = true;

    private int corePoolSize = DEFAULT_CORE_POOL_SIZE;
    private int maxPoolSize = MAX_POOL_SIZE;
    private int queueCapacity = QUEUE_CAPACITY;
    private int keepAliveSeconds = KEEP_ALIVE_SECONDS;
    private boolean allowCoreThreadTimeout = ALLOW_CORE_THREAD_TIMEOUT;
    private boolean waitForTasksToCompleteOnShutdown = WAIT_FOR_TASKS_TO_COMPLETE_ON_SHUTDOWN;
}
