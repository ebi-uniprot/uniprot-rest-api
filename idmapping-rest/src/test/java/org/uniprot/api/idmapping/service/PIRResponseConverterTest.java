package org.uniprot.api.idmapping.service;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.uniprot.api.idmapping.service.PIRResponseConverter.isValidIdPattern;
import static org.uniprot.api.rest.output.PredefinedAPIStatus.ENRICHMENT_WARNING;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.ACC_ID_STR;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.UNIPROTKB_STR;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.rest.request.idmapping.IdMappingJobRequest;

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
        request.setIds("ID1");

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

    @Test
    void checkUniProtKBSubSequenceFoundCorrectly() {
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom(ACC_ID_STR);
        request.setTo(UNIPROTKB_STR);

        ResponseEntity<String> responseEntity =
                ResponseEntity.status(HttpStatus.OK)
                        .body(
                                "Taxonomy ID: 9606\n"
                                        + "\n"
                                        + "P00001\tP00001\n"
                                        + "P00002\tP00002;Q00002\n"
                                        + "P00003\n"
                                        + "P00004\n"
                                        + "\n"
                                        + "\n"
                                        + "\n"
                                        + "\n"
                                        + "\n"
                                        + "MSG: 200 -- 2 IDs have no matches: \"P00003,P00004,\".\n");

        List<String> ids =
                List.of("P00001[10-20]", "P00002[20-30]", "P00003[30-40]", "P00004[40-50]");
        String idsStr = String.join(",", ids);
        request.setIds(idsStr);
        IdMappingResult result = converter.convertToIDMappings(request, 20, 40, responseEntity);

        assertThat(
                result.getMappedIds(),
                contains(
                        new IdMappingStringPair("P00001[10-20]", "P00001"),
                        new IdMappingStringPair("P00002[20-30]", "P00002"),
                        new IdMappingStringPair("P00002[20-30]", "Q00002")));
        assertThat(result.getUnmappedIds(), contains("P00003[30-40]", "P00004[40-50]"));
        assertThat(result.getWarnings(), is(emptyList()));
    }

    @Test
    void checkUniProtKBTremblIdCorrectly() {
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom(ACC_ID_STR);
        request.setTo(UNIPROTKB_STR);

        ResponseEntity<String> responseEntity =
                ResponseEntity.status(HttpStatus.OK)
                        .body(
                                "Taxonomy ID: 9606\n"
                                        + "\n"
                                        + "P00001\tP00001\n"
                                        + "P00002\tP00002\n"
                                        + "P00003\n"
                                        + "P00004\n"
                                        + "\n"
                                        + "\n"
                                        + "\n"
                                        + "\n"
                                        + "\n"
                                        + "MSG: 200 -- 2 IDs have no matches: \"P00003,P00004,\".\n");

        List<String> ids =
                List.of("P00001_TREMBL1", "P00002_TREMBL2", "P00003_TREMBL3", "P00004_TREMBL4");
        String idsStr = String.join(",", ids);
        request.setIds(idsStr);
        IdMappingResult result = converter.convertToIDMappings(request, 20, 40, responseEntity);

        assertThat(
                result.getMappedIds(),
                contains(
                        new IdMappingStringPair("P00001_TREMBL1", "P00001"),
                        new IdMappingStringPair("P00002_TREMBL2", "P00002")));
        assertThat(result.getUnmappedIds(), contains("P00003_TREMBL3", "P00004_TREMBL4"));
        assertThat(result.getWarnings(), is(emptyList()));
    }

    @Test
    void checkUniProtKBSubSequenceMixedInput() {
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom(ACC_ID_STR);
        request.setTo(UNIPROTKB_STR);

        ResponseEntity<String> responseEntity =
                ResponseEntity.status(HttpStatus.OK)
                        .body(
                                "Taxonomy ID: 9606\n"
                                        + "\n"
                                        + "P00001\tP00001\n"
                                        + "P00002\tP00002;Q00002\n"
                                        + "P00003\tP00003\n"
                                        + "P00004\tP00004\n"
                                        + "SWISS_IDFIVE\tP00005\n"
                                        + "P00006\n"
                                        + "P00007\n"
                                        + "\n"
                                        + "\n"
                                        + "\n"
                                        + "\n"
                                        + "\n"
                                        + "MSG: 200 -- 2 IDs have no matches: \"P00006,P00007,\".\n");

        List<String> ids =
                List.of(
                        "P00001[10-20]",
                        "P00002[20-30]",
                        "P00003.3",
                        "P00004_TREMBL",
                        "SWISS_IDFIVE",
                        "P00006[10-20]",
                        "P00007");
        String idsStr = String.join(",", ids);
        request.setIds(idsStr);
        IdMappingResult result = converter.convertToIDMappings(request, 20, 40, responseEntity);

        assertThat(
                result.getMappedIds(),
                contains(
                        new IdMappingStringPair("P00001[10-20]", "P00001"),
                        new IdMappingStringPair("P00002[20-30]", "P00002"),
                        new IdMappingStringPair("P00002[20-30]", "Q00002"),
                        new IdMappingStringPair("P00003", "P00003"),
                        new IdMappingStringPair("P00004_TREMBL", "P00004"),
                        new IdMappingStringPair("SWISS_IDFIVE", "P00005")));
        assertThat(result.getUnmappedIds(), contains("P00006[10-20]", "P00007"));
        assertThat(result.getWarnings(), is(emptyList()));
    }

    @Test
    void checkUniProtKBWithVersionFoundCorrectly() {
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom(ACC_ID_STR);
        request.setTo(UNIPROTKB_STR);

        ResponseEntity<String> responseEntity =
                ResponseEntity.status(HttpStatus.OK)
                        .body(
                                "Taxonomy ID: 9606\n"
                                        + "\n"
                                        + "P00001\tP00001\n"
                                        + "P00002\tP00002;Q00002\n"
                                        + "P00003\n"
                                        + "P00004\n"
                                        + "\n"
                                        + "\n"
                                        + "\n"
                                        + "\n"
                                        + "\n"
                                        + "MSG: 200 -- 2 IDs have no matches: \"P00003,P00004,\".\n");

        List<String> ids = List.of("P00001.1", "P00002.2", "P00003.3", "P00004.4");
        String idsStr = String.join(",", ids);
        request.setIds(idsStr);
        IdMappingResult result = converter.convertToIDMappings(request, 20, 40, responseEntity);

        assertThat(
                result.getMappedIds(),
                contains(
                        new IdMappingStringPair("P00001", "P00001"),
                        new IdMappingStringPair("P00002", "P00002"),
                        new IdMappingStringPair("P00002", "Q00002")));
        assertThat(result.getUnmappedIds(), contains("P00003", "P00004"));
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
        request.setIds("ID1");
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
                ENRICHMENT_WARNING.getErrorMessage(maxCountForDataEnrich),
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
                "Id Mapping API is not supported for mapping results with more than 4 \"mapped to\" IDs",
                result.getErrors().get(0).getMessage());
        assertEquals(40, result.getErrors().get(0).getCode());
    }

    @Test
    void testStringIdToUniProtKB() {
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setTo(UNIPROTKB_STR);

        ResponseEntity<String> responseEntity =
                ResponseEntity.status(HttpStatus.OK)
                        .body(
                                "9606.ENSP00000244751\tQ9HCH3\n"
                                        + "9606.ENSP00000353292\tP37088\n"
                                        + "9606.ENSP00000246672\tP20393\n"
                                        + "9606.ENSP00000344430\tA6NMD0\n");

        List<String> ids =
                List.of(
                        "9606.ENSP00000244751",
                        "9606.ENSP00000353292",
                        "9606.ENSP00000246672",
                        "9606.ENSP00000344430");
        String idsStr = String.join(",", ids);
        request.setIds(idsStr);
        IdMappingResult result = converter.convertToIDMappings(request, 20, 40, responseEntity);
        assertThat(
                result.getMappedIds(),
                contains(
                        new IdMappingStringPair("9606.ENSP00000244751", "Q9HCH3"),
                        new IdMappingStringPair("9606.ENSP00000353292", "P37088"),
                        new IdMappingStringPair("9606.ENSP00000246672", "P20393"),
                        new IdMappingStringPair("9606.ENSP00000344430", "A6NMD0")));
        assertThat(result.getUnmappedIds(), is(emptyList()));
        assertThat(result.getWarnings(), is(emptyList()));
    }

    @Test
    void testGetMappedRequestIds() {
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setIds("P12345.6,9606.ENSP00000386219,9606.ENSP00000318585,Q98765[12-22],Q12345");
        request.setFrom(ACC_ID_STR);
        request.setTo(UNIPROTKB_STR);
        Map<String, Set<String>> mappedRequestIds = converter.getMappedRequestIds(request);
        assertTrue(Set.of("P12345", "Q98765").containsAll(mappedRequestIds.keySet()));
    }

    @Test
    void testGetMappedRequestIdsWithDuplicateIds() {
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setIds("P12345.6,P12345.3");
        request.setFrom(ACC_ID_STR);
        request.setTo(UNIPROTKB_STR);
        Map<String, Set<String>> mappedIds = converter.getMappedRequestIds(request);
        assertEquals(Set.of("P12345"), mappedIds.get("P12345"));
    }

    @Test
    void testGetMappedRequestIdsWithRepeatedAccessionWithSequence() {
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setIds("P12345[10-20],P05067[10-20],P12345[20-30],P12345[10-20]");
        request.setFrom(ACC_ID_STR);
        request.setTo(UNIPROTKB_STR);
        Map<String, Set<String>> mappedRequestIds = converter.getMappedRequestIds(request);
        assertTrue(Set.of("P12345", "P05067").containsAll(mappedRequestIds.keySet()));
        assertEquals(Set.of("P12345[10-20]", "P12345[20-30]"), mappedRequestIds.get("P12345"));
        assertEquals(Set.of("P05067[10-20]"), mappedRequestIds.get("P05067"));
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
