package org.uniprot.api.idmapping.service.impl;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.ACC_ID_STR;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.service.IdMappingPIRService;
import org.uniprot.api.idmapping.service.PIRResponseConverter;
import org.uniprot.api.rest.request.idmapping.IdMappingJobRequest;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.idmapping.IdMappingFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;

/**
 * Created 17/02/2021
 *
 * @author Edd
 */
@Profile("live")
@Service
@Slf4j
public class PIRServiceImpl extends IdMappingPIRService {
    static final HttpHeaders HTTP_HEADERS = new HttpHeaders();

    static {
        HTTP_HEADERS.setContentType(APPLICATION_FORM_URLENCODED);
    }
    // regex = ACCESSION+(Sequence or Version)
    //       =
    // ([OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z]([0-9][A-Z][A-Z0-9]{2}){1,2}[0-9])(-[0-9]+)?(\[[0-9]+\-[0-9]+\]|\.[0-9]+)
    public static final Pattern UNIPROTKB_ACCESSION_WITH_SEQUENCE_OR_VERSION =
            Pattern.compile(
                    SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB)
                                    .getSearchFieldItemByName("accession_id")
                                    .getValidRegex()
                            + "(\\[[0-9]+\\-[0-9]+\\]|\\.[0-9]+)");
    public static final Pattern UNIPROTKB_ACCESSION_REGEX =
            Pattern.compile(
                    SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB)
                            .getSearchFieldItemByName("accession_id")
                            .getValidRegex());

    public final String pirIdMappingUrl;
    private final RestTemplate restTemplate;
    private final PIRResponseConverter pirResponseConverter;
    private final Integer maxIdMappingToIdsCountEnriched;
    private final Integer maxIdMappingToIdsCount;

    @Autowired
    public PIRServiceImpl(
            RestTemplate idMappingRestTemplate,
            @Value("${search.default.page.size:#{null}}") Integer defaultPageSize,
            @Value(
                            "${id.mapping.pir.url:https://idmapping.uniprot.org/cgi-bin/idmapping_http_client_async}")
                    String pirMappingUrl,
            @Value("${id.mapping.max.to.ids.count:#{null}}") Integer maxIdMappingToIdsCount,
            @Value("${id.mapping.max.to.ids.enrich.count:#{null}}")
                    Integer maxIdMappingToIdsCountEnriched) {

        super(defaultPageSize);
        this.restTemplate = idMappingRestTemplate;
        this.pirResponseConverter = new PIRResponseConverter();
        this.pirIdMappingUrl = UriComponentsBuilder.fromHttpUrl(pirMappingUrl).toUriString();
        this.maxIdMappingToIdsCountEnriched = maxIdMappingToIdsCountEnriched;
        this.maxIdMappingToIdsCount = maxIdMappingToIdsCount;
    }

    @Override
    public IdMappingResult mapIds(IdMappingJobRequest request, String jobId) {
        HttpEntity<MultiValueMap<String, String>> requestBody =
                new HttpEntity<>(createPostBody(request), HTTP_HEADERS);
        log.info("Making PIR call for jobId {}", jobId);
        ResponseEntity<String> pirResponse =
                restTemplate.postForEntity(pirIdMappingUrl, requestBody, String.class);
        log.info("PIR call returned for jobId {}", jobId);
        return pirResponseConverter.convertToIDMappings(
                request,
                this.maxIdMappingToIdsCountEnriched,
                this.maxIdMappingToIdsCount,
                pirResponse);
    }

    private MultiValueMap<String, String> createPostBody(IdMappingJobRequest request) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("ids", getIdsFromRequest(request));

        map.add("from", IdMappingFieldConfig.convertDbNameToPIRDbName(request.getFrom()));
        map.add("to", IdMappingFieldConfig.convertDbNameToPIRDbName(request.getTo()));

        map.add("tax_off", "NO"); // we do not need PIR's header line, "Taxonomy ID:"
        map.add("taxid", request.getTaxId());
        map.add("async", "NO");

        return map;
    }

    String getIdsFromRequest(IdMappingJobRequest request) {
        String ids = String.join(",", request.getIds());
        if (request.getFrom().equals(ACC_ID_STR)) {
            Set<String> uniqueIds =
                    Arrays.stream(ids.split(","))
                            .map(this::cleanIdBeforeSubmit)
                            .collect(Collectors.toCollection(LinkedHashSet::new));
            ids = String.join(",", uniqueIds);
        }
        return ids;
    }

    private String cleanIdBeforeSubmit(String id) {
        String result = id;
        if (UNIPROTKB_ACCESSION_WITH_SEQUENCE_OR_VERSION.matcher(id).matches()) {
            if (id.contains(".")) {
                result = id.substring(0, id.indexOf("."));
            }
            if (id.contains("[")) {
                result = id.substring(0, id.indexOf("["));
            }
        } else if (id.contains("_")
                && UNIPROTKB_ACCESSION_REGEX.matcher(id.split("_")[0]).matches()) {
            result = id.substring(0, id.indexOf("_"));
        }
        return result.strip();
    }
}
