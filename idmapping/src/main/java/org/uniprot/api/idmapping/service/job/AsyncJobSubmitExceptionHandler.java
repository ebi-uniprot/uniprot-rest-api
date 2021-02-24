package org.uniprot.api.idmapping.service.job;

import java.lang.reflect.Method;

import lombok.extern.slf4j.Slf4j;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.uniprot.api.idmapping.service.cache.IdMappingJobCacheService;

/**
 * @author sahmad
 * @created 23/02/2021
 */
@Slf4j
public class AsyncJobSubmitExceptionHandler implements AsyncUncaughtExceptionHandler {
    private final IdMappingJobCacheService cacheService;

    public AsyncJobSubmitExceptionHandler(IdMappingJobCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... objects) {
        // FIXME add code to update error in job object
        log.error("Exception message - " + throwable.getMessage());
        log.error("Method name - " + method.getName());
        for (Object param : objects) { // object is of type IdMappingJob
            log.error("Parameter value - " + param);
        }
    }
}
