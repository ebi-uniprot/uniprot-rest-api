package org.uniprot.api.idmapping.service;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.uniprot.api.idmapping.model.PredefinedIdMappingStatus.ENRICHMENT_WARNING;
import static org.uniprot.api.idmapping.service.PIRResponseConverter.isValidIdPattern;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.uniprot.api.idmapping.controller.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;

class PIRResponseConverterTest {
    private PIRResponseConverter converter;
    private IdMappingJobRequest request;

    @BeforeEach
    void setUp() {
        converter = new PIRResponseConverter();
        request = new IdMappingJobRequest();
        request.setTo("EMBL");
    }

    @Test
    void httpNot200CausesEmpty() {
        ResponseEntity<String> responseEntity =
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body("WRONG");

        assertThrows(
                HttpServerErrorException.class,
                () -> converter.convertToIDMappings(request, 20, 40, responseEntity));
    }

    @Test
    void emptyResponseGivesEmptyResult() {
        ResponseEntity<String> responseEntity = ResponseEntity.status(HttpStatus.OK).build();

        IdMappingResult result = converter.convertToIDMappings(request, 20, 40, responseEntity);

        assertThat(result.getMappedIds(), is(emptyList()));
        assertThat(result.getUnmappedIds(), is(emptyList()));
        assertThat(result.getWarnings(), is(emptyList()));
    }

    @Test
    void multipleItemsInResponseProduceCorrectResult() {
        ResponseEntity<String> responseEntity =
                ResponseEntity.status(HttpStatus.OK).body("From1\tTo1\n" + "From2\tTo1;To2\n");

        IdMappingResult result = converter.convertToIDMappings(request, 20, 40, responseEntity);

        assertThat(
                result.getMappedIds(),
                contains(
                        new IdMappingStringPair("From1", "To1"),
                        new IdMappingStringPair("From2", "To1"),
                        new IdMappingStringPair("From2", "To2")));
        assertThat(result.getUnmappedIds(), is(emptyList()));
        assertThat(result.getWarnings(), is(emptyList()));
    }

    @Test
    void filterInvalidIdsFromResponse() {
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setTo("UniProtKB");

        ResponseEntity<String> responseEntity =
                ResponseEntity.status(HttpStatus.OK)
                        .body(
                                "From1\tP12345\n"
                                        + "From2\tP12345;UPI0000000001;P21802\n"
                                        + "From3\tUPI0000000001;P12346\n");

        IdMappingResult result = converter.convertToIDMappings(request, 20, 40, responseEntity);

        assertThat(
                result.getMappedIds(),
                contains(
                        new IdMappingStringPair("From1", "P12345"),
                        new IdMappingStringPair("From2", "P12345"),
                        new IdMappingStringPair("From2", "P21802"),
                        new IdMappingStringPair("From3", "P12346")));
        assertThat(result.getUnmappedIds(), is(emptyList()));
        assertThat(result.getWarnings(), is(emptyList()));
    }

    @Test
    void responseWithTaxIdHandledCorrectly() {
        ResponseEntity<String> responseEntity =
                ResponseEntity.status(HttpStatus.OK)
                        .body("Taxonomy ID: 9606\n" + "\n" + "From1\tTo1\n" + "From2\tTo1;To2\n");

        IdMappingResult result = converter.convertToIDMappings(request, 20, 40, responseEntity);

        assertThat(
                result.getMappedIds(),
                contains(
                        new IdMappingStringPair("From1", "To1"),
                        new IdMappingStringPair("From2", "To1"),
                        new IdMappingStringPair("From2", "To2")));
        assertThat(result.getUnmappedIds(), is(emptyList()));
        assertThat(result.getWarnings(), is(emptyList()));
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

        IdMappingResult result = converter.convertToIDMappings(request, 20, 40, responseEntity);

        assertThat(
                result.getMappedIds(),
                contains(
                        new IdMappingStringPair("From1", "To1"),
                        new IdMappingStringPair("From2", "To1"),
                        new IdMappingStringPair("From2", "To2")));
        assertThat(result.getUnmappedIds(), contains("gene 12", "gene 36"));
        assertThat(result.getWarnings(), is(emptyList()));
    }

    @Test
    void checkNoMatchesAreFoundCorrectly() {
        ResponseEntity<String> responseEntity =
                ResponseEntity.status(HttpStatus.OK)
                        .body("\n" + "\n" + "\n" + "\n" + "\n" + "MSG: 200 -- No Matches.");

        List<String> ids = List.of("id1", "id2", "id3", "id4");
        String idsStr = String.join(",", ids);
        request.setIds(idsStr);
        IdMappingResult result = converter.convertToIDMappings(request, 20, 40, responseEntity);

        assertThat(result.getMappedIds(), is(emptyList()));
        assertThat(result.getUnmappedIds(), is(ids));
        assertThat(result.getWarnings(), is(emptyList()));
    }

    @ParameterizedTest
    @MethodSource("validToAndIdPairs")
    void checkValidPairs(String to, String id) {
        assertThat(isValidIdPattern(to, id), is(true));
    }

    private static Stream<Arguments> validToAndIdPairs() {
        return Stream.of(
                Arguments.of("EMBL", "AAAAA10001.1"),
                Arguments.of(
                        "EMBL",
                        "CRAZY-IS-OKAY-FOR-NON-UNIPROTKB/UNIPARC/UNIREF because we do not need to fetch these ids from our store layer"),
                Arguments.of("UniProtKB", "P12345"),
                Arguments.of("UniRef50", "UniRef50_P12345"),
                Arguments.of("UniRef90", "UniRef90_P12345"),
                Arguments.of("UniRef100", "UniRef100_P12345"),
                Arguments.of("UniParc", "UPI0000000001"));
    }

    @ParameterizedTest
    @MethodSource("invalidToAndIdPairs")
    void checkInvalidPairs(String to, String id) {
        assertThat(isValidIdPattern(to, id), is(false));
    }

    @Test
    void checkIdMappingResultWithWarning() {
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setTo("UniProtKB");
        // when  more than allowed ids (20 for tests) for enrichment
        ResponseEntity<String> responseEntity =
                ResponseEntity.status(HttpStatus.OK)
                        .body(
                                "From1\tP00001;P00002;P00003;P00004;P00005\n"
                                        + "From2\tP00006;P00007;P00008;P00009;P00010\n"
                                        + "From3\tP00011;P00012;P00014;P00015;P00016\n"
                                        + "From4\tP00016;P00017;P00018;P00019;P00020\n"
                                        + "From5\tP00021\n");
        int maxCountForDataEnrich = 20;
        IdMappingResult result =
                converter.convertToIDMappings(request, maxCountForDataEnrich, 40, responseEntity);

        assertFalse(result.getMappedIds().isEmpty());
        assertEquals(21, result.getMappedIds().size());
        assertFalse(result.getWarnings().isEmpty());
        assertEquals(1, result.getWarnings().size());
        assertEquals(
                ENRICHMENT_WARNING.getMessage() + maxCountForDataEnrich,
                result.getWarnings().get(0).getMessage());
        assertThat(result.getUnmappedIds(), is(emptyList()));
    }

    @Test
    void checkIdMappingResultWithoutWarningForNonUniProtId() {
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setTo("EMBL");
        // when  more than allowed ids (20 for tests) for enrichment
        ResponseEntity<String> responseEntity =
                ResponseEntity.status(HttpStatus.OK).body("From1\t00001;00002;00003;00004;00005\n");

        IdMappingResult result = converter.convertToIDMappings(request, 4, 40, responseEntity);

        assertFalse(result.getMappedIds().isEmpty());
        assertEquals(5, result.getMappedIds().size());
        assertTrue(result.getWarnings().isEmpty());
        assertThat(result.getUnmappedIds(), is(emptyList()));
    }

    @Test
    void checkIdMappingResultWithLimitExceedError() {
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setTo("EMBL");
        // when  more than allowed ids (20 for tests) for enrichment
        ResponseEntity<String> responseEntity =
                ResponseEntity.status(HttpStatus.OK).body("From1\t00001;00002;00003;00004;00005\n");

        IdMappingResult result = converter.convertToIDMappings(request, 4, 4, responseEntity);

        assertTrue(result.getMappedIds().isEmpty());
        assertTrue(result.getWarnings().isEmpty());
        assertThat(result.getUnmappedIds(), is(emptyList()));
        assertEquals(1, result.getErrors().size());
        assertEquals(
                "Id Mapping API is not supported for mapping results with \"mapped to\" IDs more than 4",
                result.getErrors().get(0).getMessage());
        assertEquals(40, result.getErrors().get(0).getCode());
    }

    private static Stream<Arguments> invalidToAndIdPairs() {
        return Stream.of(
                Arguments.of("UniProtKB", "UPI0000000001"),
                Arguments.of("UniRef50", "UPI0000000001"),
                Arguments.of("UniRef90", "UPI0000000001"),
                Arguments.of("UniRef100", "UPI0000000001"),
                Arguments.of("UniParc", "P12345"),
                Arguments.of("UniParc", "UniRef100_P12345"));
    }
}
