package org.uniprot.api.idmapping.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.uniprot.api.idmapping.service.impl.PIRServiceImpl.HTTP_HEADERS;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.ACC_ID_STR;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.UNIPROTKB_STR;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.rest.request.idmapping.IdMappingJobRequest;
import org.uniprot.store.config.idmapping.IdMappingFieldConfig;

class PIRServiceImplTest {
    private RestTemplate restTemplate;
    private PIRServiceImpl pirService;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        pirService = new PIRServiceImpl(restTemplate, 5, "http://localhost", 20, 10);
    }

    @Test
    void createsExpectedResult() {
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom("UniProtKB_AC-ID");
        request.setTo("EMBL");
        request.setIds("id");
        request.setTaxId("taxId");

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("ids", String.join(",", request.getIds()));
        map.add("from", IdMappingFieldConfig.convertDbNameToPIRDbName(request.getFrom()));
        map.add("to", IdMappingFieldConfig.convertDbNameToPIRDbName(request.getTo()));
        map.add("tax_off", "NO"); // we do not need PIR's header line, "Taxonomy ID:"
        map.add("taxid", request.getTaxId());
        map.add("async", "NO");

        when(restTemplate.postForEntity(
                        "http://localhost", new HttpEntity<>(map, HTTP_HEADERS), String.class))
                .thenReturn(ResponseEntity.ok().body("From1\tTo1\n"));

        IdMappingResult idMappingResult = pirService.mapIds(request, "dummyJobId");
        assertThat(
                idMappingResult.getMappedIds(), contains(new IdMappingStringPair("From1", "To1")));
    }

    @Test
    void createsSubSequenceExpectedResult() {
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom(ACC_ID_STR);
        request.setTo(UNIPROTKB_STR);
        request.setIds("P00001[10-20],P00002[20-30]");
        request.setTaxId("taxId");

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("ids", "P00001,P00002"); // submit to PIR without subsequence data
        map.add("from", IdMappingFieldConfig.convertDbNameToPIRDbName(request.getFrom()));
        map.add("to", IdMappingFieldConfig.convertDbNameToPIRDbName(request.getTo()));
        map.add("tax_off", "NO"); // we do not need PIR's header line, "Taxonomy ID:"
        map.add("taxid", request.getTaxId());
        map.add("async", "NO");

        when(restTemplate.postForEntity(
                        "http://localhost", new HttpEntity<>(map, HTTP_HEADERS), String.class))
                .thenReturn(ResponseEntity.ok().body("P00001\tP00001\nP00002\n"));

        IdMappingResult idMappingResult = pirService.mapIds(request, "dummyJobId");
        assertThat(
                idMappingResult.getMappedIds(),
                contains(new IdMappingStringPair("P00001[10-20]", "P00001")));
        assertThat(idMappingResult.getUnmappedIds(), contains("P00002[20-30]"));
    }

    @Test
    void createsWithUniProtKBVersionResult() {
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom(ACC_ID_STR);
        request.setTo(UNIPROTKB_STR);
        request.setIds("P00001.1,P00002.2");
        request.setTaxId("taxId");

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("ids", "P00001,P00002"); // submit to PIR without version data
        map.add("from", IdMappingFieldConfig.convertDbNameToPIRDbName(request.getFrom()));
        map.add("to", IdMappingFieldConfig.convertDbNameToPIRDbName(request.getTo()));
        map.add("tax_off", "NO"); // we do not need PIR's header line, "Taxonomy ID:"
        map.add("taxid", request.getTaxId());
        map.add("async", "NO");

        when(restTemplate.postForEntity(
                        "http://localhost", new HttpEntity<>(map, HTTP_HEADERS), String.class))
                .thenReturn(ResponseEntity.ok().body("P00001\tP00001\nP00002\n"));

        IdMappingResult idMappingResult = pirService.mapIds(request, "dummyJobId");
        assertThat(
                idMappingResult.getMappedIds(),
                contains(new IdMappingStringPair("P00001", "P00001")));
        assertThat(idMappingResult.getUnmappedIds(), contains("P00002"));
    }

    @Test
    void createsWithUniProtKBTremblIdResult() {
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom(ACC_ID_STR);
        request.setTo(UNIPROTKB_STR);
        request.setIds("P00001_TREMBL,P00002_TREMBL");
        request.setTaxId("taxId");

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("ids", "P00001,P00002"); // submit to PIR only with accession prefix
        map.add("from", IdMappingFieldConfig.convertDbNameToPIRDbName(request.getFrom()));
        map.add("to", IdMappingFieldConfig.convertDbNameToPIRDbName(request.getTo()));
        map.add("tax_off", "NO"); // we do not need PIR's header line, "Taxonomy ID:"
        map.add("taxid", request.getTaxId());
        map.add("async", "NO");

        when(restTemplate.postForEntity(
                "http://localhost", new HttpEntity<>(map, HTTP_HEADERS), String.class))
                .thenReturn(ResponseEntity.ok().body("P00001\tP00001\nP00002\n"));

        IdMappingResult idMappingResult = pirService.mapIds(request, "dummyJobId");
        assertThat(
                idMappingResult.getMappedIds(),
                contains(new IdMappingStringPair("P00001_TREMBL", "P00001")));
        assertThat(idMappingResult.getUnmappedIds(), contains("P00002_TREMBL"));
    }

    @Test
    void testGetIdsFromRequestForStringID() {
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom("STRING");
        request.setTo(UNIPROTKB_STR);
        String origIds =
                "511145.b0584,208964.PA2398,196627.cg3353,246196.MSMEI_0939,55601.VANGNB10_67p009,9606.ENSP00000318585";
        request.setIds(origIds);
        String processedIds = pirService.getIdsFromRequest(request);
        assertThat(processedIds, equalTo(origIds));
    }

    @Test
    void testGetIdsFromRequestForAccessionId() {
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom(ACC_ID_STR);
        request.setTo(UNIPROTKB_STR);
        String origIds = "P00001.1,P00002.2";
        request.setIds(origIds);
        String processedIds = pirService.getIdsFromRequest(request);
        assertThat(processedIds, not(origIds));
        assertThat(processedIds, equalTo("P00001,P00002"));
    }

    @Test
    void testGetIdsFromRequestWithMixedAccessions() {
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom(ACC_ID_STR);
        request.setTo(UNIPROTKB_STR);
        String origIds = "P00001.1,9606.ENSP00000318585,P00002.2,9606.ENSP00000318555,P12345";
        request.setIds(origIds);
        String processedIds = pirService.getIdsFromRequest(request);
        assertThat(processedIds, not(origIds));
        assertThat(
                processedIds,
                equalTo("P00001,9606.ENSP00000318585,P00002,9606.ENSP00000318555,P12345"));
    }

    @Test
    void testKeepsOnlyOneVersionDuplicateIds() {
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom(ACC_ID_STR);
        request.setTo(UNIPROTKB_STR);
        request.setIds("P00001.1,P00001.2");
        request.setTaxId("taxId");

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("ids", "P00001"); // submit to PIR without version data
        map.add("from", IdMappingFieldConfig.convertDbNameToPIRDbName(request.getFrom()));
        map.add("to", IdMappingFieldConfig.convertDbNameToPIRDbName(request.getTo()));
        map.add("tax_off", "NO"); // we do not need PIR's header line, "Taxonomy ID:"
        map.add("taxid", request.getTaxId());
        map.add("async", "NO");

        when(restTemplate.postForEntity(
                        "http://localhost", new HttpEntity<>(map, HTTP_HEADERS), String.class))
                .thenReturn(ResponseEntity.ok().body("P00001\tP00001"));

        IdMappingResult idMappingResult = pirService.mapIds(request, "dummyJobId");
        assertThat(
                idMappingResult.getMappedIds(),
                contains(new IdMappingStringPair("P00001", "P00001")));
        assertThat(idMappingResult.getUnmappedIds(), is(empty()));
    }

    @Test
    void createsSubSequenceWithDuplicateAccessionExpectedResult() {
        IdMappingJobRequest request = new IdMappingJobRequest();
        request.setFrom(ACC_ID_STR);
        request.setTo(UNIPROTKB_STR);
        request.setIds("P00001[10-20],P00002[20-30],P00001[20-30]");
        request.setTaxId("taxId");

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("ids", "P00001,P00002"); // submit to PIR without subsequence data
        map.add("from", IdMappingFieldConfig.convertDbNameToPIRDbName(request.getFrom()));
        map.add("to", IdMappingFieldConfig.convertDbNameToPIRDbName(request.getTo()));
        map.add("tax_off", "NO"); // we do not need PIR's header line, "Taxonomy ID:"
        map.add("taxid", request.getTaxId());
        map.add("async", "NO");

        when(restTemplate.postForEntity(
                        "http://localhost", new HttpEntity<>(map, HTTP_HEADERS), String.class))
                .thenReturn(ResponseEntity.ok().body("P00001\tP00001\nP00002\tP00002\n"));

        IdMappingResult idMappingResult = pirService.mapIds(request, "dummyJobId");
        assertThat(
                idMappingResult.getMappedIds(),
                contains(
                        new IdMappingStringPair("P00001[10-20]", "P00001"),
                        new IdMappingStringPair("P00001[20-30]", "P00001"),
                        new IdMappingStringPair("P00002[20-30]", "P00002")));
    }
}
