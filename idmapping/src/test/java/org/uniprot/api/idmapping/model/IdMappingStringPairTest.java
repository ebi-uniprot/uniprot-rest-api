package org.uniprot.api.idmapping.model;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class IdMappingStringPairTest {
    @Test
    void canCreatePair() {
        IdMappingStringPair pair =
                IdMappingStringPair.builder().fromValue("from").toValue("to").build();
        assertThat(pair.getKey(), is("from"));
        assertThat(pair.getValue(), is("to"));
    }
}
