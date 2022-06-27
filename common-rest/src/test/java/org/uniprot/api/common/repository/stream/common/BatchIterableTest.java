package org.uniprot.api.common.repository.stream.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author sahmad
 * @created 27/06/2022
 */
public class BatchIterableTest {

    @Test
    void testUPIsLoad(){
        Assertions.assertEquals(10, BatchIterable.FILTERED_UPIDS.size());
    }
}
