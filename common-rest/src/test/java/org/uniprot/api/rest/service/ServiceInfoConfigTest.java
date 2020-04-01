package org.uniprot.api.rest.service;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.uniprot.api.rest.service.ServiceInfoConfig.ServiceInfo.RELEASE;

/**
 * Created 01/04/20
 *
 * @author Edd
 */
class ServiceInfoConfigTest {

    @Test
    void validatingServiceInfoChecksForReleaseAndSucceeds() {
        Map<String, Object> map = new HashMap<>();
        map.put(RELEASE, "value");
        ServiceInfoConfig.ServiceInfo serviceInfo =
                ServiceInfoConfig.ServiceInfo.builder().map(map).build();
        assertDoesNotThrow(serviceInfo::validate);
    }

    @Test
    void validatingServiceInfoChecksForReleaseAndThrowsException() {
        ServiceInfoConfig.ServiceInfo serviceInfo =
                ServiceInfoConfig.ServiceInfo.builder().map(new HashMap<>()).build();
        assertThrows(IllegalStateException.class, serviceInfo::validate);
    }
}
