package org.uniprot.api.idmapping.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PIRResponseConverterTest {
    private PIRResponseConverter converter;

    @BeforeEach
    void setUp() {
        converter = new PIRResponseConverter();
    }

    @Test
    void httpNot200CausesEmpty() {
        ResponseEntity<String> responseEntity =
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body("WRONG");

        assertThrows(
                HttpServerErrorException.class,
                () -> converter.convertToIDMappings(responseEntity));
    }

    @Test
    void emptyResponseGivesEmptyResult() {
        ResponseEntity<String> responseEntity = ResponseEntity.status(HttpStatus.OK).build();

        IdMappingResult result = converter.convertToIDMappings(responseEntity);

        assertThat(result.getMappedIds(), is(emptyList()));
        assertThat(result.getUnmappedIds(), is(emptyList()));
    }

    @Test
    void multipleItemsInResponseProduceCorrectResult() {
        ResponseEntity<String> responseEntity =
                ResponseEntity.status(HttpStatus.OK).body("From1\tTo1\n" + "From2\tTo1;To2\n");

        IdMappingResult result = converter.convertToIDMappings(responseEntity);

        assertThat(
                result.getMappedIds(),
                contains(
                        new IdMappingStringPair("From1", "To1"),
                        new IdMappingStringPair("From2", "To1"),
                        new IdMappingStringPair("From2", "To2")));
        assertThat(result.getUnmappedIds(), is(emptyList()));
    }

    @Test
    void responseWithTaxIdHandledCorrectly() {
        ResponseEntity<String> responseEntity =
                ResponseEntity.status(HttpStatus.OK)
                        .body("Taxonomy ID: 9606\n" + "\n" + "From1\tTo1\n" + "From2\tTo1;To2\n");

        IdMappingResult result = converter.convertToIDMappings(responseEntity);

        assertThat(
                result.getMappedIds(),
                contains(
                        new IdMappingStringPair("From1", "To1"),
                        new IdMappingStringPair("From2", "To1"),
                        new IdMappingStringPair("From2", "To2")));
        assertThat(result.getUnmappedIds(), is(emptyList()));
    }

    @Test
    void failedMappingsReturnedInResponse() {
        ResponseEntity<String> responseEntity =
                ResponseEntity.status(HttpStatus.OK)
                        .body(
                                "Taxonomy ID: 9606\n"
                                        + "\n"
                                        + "From1\tTo1\n"
                                        + "From2\tTo1;To2\n"
                                        + "gene 12\n"
                                        + "gene 36\n"
                                        + "\n"
                                        + "\n"
                                        + "\n"
                                        + "\n"
                                        + "\n"
                                        + "MSG: 200 -- 2 IDs have no matches: \"gene 12,gene 36,\".\n");

        IdMappingResult result = converter.convertToIDMappings(responseEntity);

        assertThat(
                result.getMappedIds(),
                contains(
                        new IdMappingStringPair("From1", "To1"),
                        new IdMappingStringPair("From2", "To1"),
                        new IdMappingStringPair("From2", "To2")));
        assertThat(result.getUnmappedIds(), contains("gene 12", "gene 36"));
    }
}
