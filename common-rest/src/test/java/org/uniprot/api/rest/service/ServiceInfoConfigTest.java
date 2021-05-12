package org.uniprot.api.rest.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.uniprot.api.rest.service.ServiceInfoConfig.ServiceInfo.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Created 01/04/20
 *
 * @author Edd
 */
class ServiceInfoConfigTest {

    @Test
    void validatingServiceInfoChecksForReleaseAndSucceeds() {
        Map<String, Object> map = new HashMap<>();
        map.put(RELEASE_NUMBER, "value");
        map.put(RELEASE_DATE, "value");
        map.put(CACHE_CONTROL_MAX_AGE, "200");
        ServiceInfoConfig.ServiceInfo serviceInfo =
                ServiceInfoConfig.ServiceInfo.builder().map(map).build();
        assertDoesNotThrow(serviceInfo::validate);
    }

    @Test
    void validatingServiceInfoChecksForReleaseNumberAndThrowsException() {
        Map<String, Object> map = new HashMap<>();
        map.put(RELEASE_DATE, "value");
        ServiceInfoConfig.ServiceInfo serviceInfo =
                ServiceInfoConfig.ServiceInfo.builder().map(map).build();
        assertThrows(IllegalStateException.class, serviceInfo::validate);
    }

    @Test
    void validatingServiceInfoChecksForReleaseDateAndThrowsException() {
        Map<String, Object> map = new HashMap<>();
        map.put(RELEASE_NUMBER, "value");
        ServiceInfoConfig.ServiceInfo serviceInfo =
                ServiceInfoConfig.ServiceInfo.builder().map(map).build();
        assertThrows(IllegalStateException.class, serviceInfo::validate);
    }
}
