package org.uniprot.api.idmapping.common.response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.uniprot.api.idmapping.common.response.IdMappingStringPairTSVMapper.FROM_FIELD;
import static org.uniprot.api.idmapping.common.response.IdMappingStringPairTSVMapper.TO_FIELD;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;

class IdMappingStringPairTSVMapperTest {

    private IdMappingStringPairTSVMapper tsvMapper;

    @BeforeEach
    void setUp() {
        tsvMapper = new IdMappingStringPairTSVMapper();
    }

    @Test
    void fromToFieldsAreReturned() {
        String from = "from1";
        String to = "to1";
        Map<String, String> entity = tsvMapper.mapEntity(new IdMappingStringPair(from, to), null);
        assertThat(entity, hasEntry(FROM_FIELD, from));
        assertThat(entity, hasEntry(TO_FIELD, to));
    }
}
