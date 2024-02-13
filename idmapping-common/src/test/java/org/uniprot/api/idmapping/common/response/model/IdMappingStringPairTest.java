package org.uniprot.api.idmapping.common.response.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class IdMappingStringPairTest {
    @Test
    void canCreatePair() {
        IdMappingStringPair pair = IdMappingStringPair.builder().from("from").to("to").build();
        assertThat(pair.getFrom(), is("from"));
        assertThat(pair.getTo(), is("to"));
    }
}
