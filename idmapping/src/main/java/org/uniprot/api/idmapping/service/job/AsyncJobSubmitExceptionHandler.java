package org.uniprot.api.idmapping.service.job;

import java.lang.reflect.Method;

import lombok.extern.slf4j.Slf4j;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

/**
 * @author sahmad
 * @created 23/02/2021
 */
@Slf4j
public class AsyncJobSubmitExceptionHandler implements AsyncUncaughtExceptionHandler {
    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... objects) {
        // FIXME add code to update error in job object
        System.out.println("Exception message - " + throwable.getMessage());
        System.out.println("Method name - " + method.getName());
        for (Object param : objects) {
            System.out.println("Parameter value - " + param);
        }
    }
}
