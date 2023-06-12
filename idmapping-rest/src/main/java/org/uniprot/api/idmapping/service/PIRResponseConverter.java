package org.uniprot.api.idmapping.service;

import static java.util.Arrays.asList;
import static org.uniprot.api.idmapping.service.impl.PIRServiceImpl.UNIPROTKB_ACCESSION_REGEX;
import static org.uniprot.api.idmapping.service.impl.PIRServiceImpl.UNIPROTKB_ACCESSION_WITH_SEQUENCE_OR_VERSION;
import static org.uniprot.api.rest.output.PredefinedAPIStatus.ENRICHMENT_WARNING;
import static org.uniprot.api.rest.output.PredefinedAPIStatus.LIMIT_EXCEED_ERROR;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.*;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.service.impl.IdMappingJobServiceImpl;
import org.uniprot.api.rest.request.idmapping.IdMappingJobRequest;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;

/**
 * Created 17/02/2021
 *
 * @author Edd
 */
public class PIRResponseConverter {
    private static final Pattern UNIREF_ID_PATTERN =
            Pattern.compile(
                    SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIREF)
                            .getSearchFieldItemByName("id")
                            .getValidRegex());
    private static final Pattern UNIPARC_ID_PATTERN =
            Pattern.compile(
                    SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPARC)
                            .getSearchFieldItemByName("upi")
                            .getValidRegex());
    private static final Pattern UNIPROTKB_ID_PATTERN =
            Pattern.compile(
                    SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB)
                            .getSearchFieldItemByName("accession_id")
                            .getValidRegex());
    private static final String NO_MATCHES_PIR_RESPONSE = "MSG: 200 -- No Matches.";

    static boolean isValidIdPattern(String to, String toValue) {
        to = to.strip();
        if (to.startsWith("UniRef")) {
            return UNIREF_ID_PATTERN.matcher(toValue).matches();
        } else if (to.equals("UniParc")) {
            return UNIPARC_ID_PATTERN.matcher(toValue).matches();
        } else if (to.equals("UniProtKB")) {
            return UNIPROTKB_ID_PATTERN.matcher(toValue).matches();
        } else {
            return true;
        }
    }

    public IdMappingResult convertToIDMappings(
            IdMappingJobRequest request,
            Integer maxToUniProtIdsAllowed,
            Integer maxToIdsAllowed,
            ResponseEntity<String> response) {
        IdMappingResult.IdMappingResultBuilder builder = IdMappingResult.builder();
        HttpStatus statusCode = response.getStatusCode();
        if (statusCode.equals(HttpStatus.OK)) {
            if (response.hasBody()) {
                Map<String, Set<String>> mappedRequestIds = getMappedRequestIds(request);
                response.getBody()
                        .lines()
                        .filter(line -> !line.startsWith("Taxonomy ID:"))
                        .filter(Utils::notNullNotEmpty)
                        //                        .filter(line -> !line.startsWith("MSG:"))
                        .forEach(line -> convertLine(line, request, mappedRequestIds, builder));
                // populate  error  if needed
                Optional<ProblemPair> optError =
                        getOptionalLimitError(maxToIdsAllowed, builder.build());
                if (optError.isPresent()) {
                    builder.clearMappedIds();
                    builder.clearUnmappedIds();
                    builder.error(optError.get());
                } else {
                    // populate warning if needed
                    Optional<ProblemPair> optWarning =
                            getOptionalEnrichmentWarning(
                                    request, maxToUniProtIdsAllowed, builder.build());
                    if (optWarning.isPresent()) {
                        builder.warning(optWarning.get());
                    }
                }
            }
        } else {
            throw new HttpServerErrorException(statusCode, "PIR id-mapping service error");
        }

        return builder.build();
    }

    private void convertLine(
            String line,
            IdMappingJobRequest request,
            Map<String, Set<String>> mappedIds,
            IdMappingResult.IdMappingResultBuilder builder) {
        if (line.startsWith("MSG:")) {
            if (line.endsWith(NO_MATCHES_PIR_RESPONSE)) {
                builder.unmappedIds(asList(request.getIds().split(",")));
            }
        } else {
            String[] rowParts = line.split("\t");
            Set<String> fromValues = getFromValue(mappedIds, rowParts[0]);
            if (rowParts.length == 1) {
                fromValues.stream().forEach(builder::unmappedId);
            } else {
                for (String fromValue : fromValues) {
                    Arrays.stream(rowParts[1].split(";"))
                            // filter based on valid to
                            .filter(toValue -> isValidIdPattern(request.getTo(), toValue))
                            .map(
                                    toValue ->
                                            IdMappingStringPair.builder()
                                                    .from(fromValue)
                                                    .to(toValue)
                                                    .build())
                            .forEach(builder::mappedId);
                }
            }
        }
    }

    private Set<String> getFromValue(Map<String, Set<String>> mappedIds, String fromValue) {
        return mappedIds.getOrDefault(fromValue, Set.of(fromValue));
    }

    Map<String, Set<String>> getMappedRequestIds(IdMappingJobRequest request) {
        Map<String, Set<String>> mappedIds = new HashMap<>();
        if (ACC_ID_STR.equals(request.getFrom())
                && (request.getIds().contains("[") || request.getIds().contains(".") || request.getIds().contains("_"))) {
            String ids = String.join(",", request.getIds());
            mappedIds =
                    Arrays.stream(ids.split(","))
                            .map(this::getMappedId)
                            .filter(Objects::nonNull)
                            .collect(
                                    Collectors.groupingBy(
                                            Map.Entry::getKey,
                                            Collectors.mapping(
                                                    Map.Entry::getValue,
                                                    Collectors.toCollection(LinkedHashSet::new))));
            // remove more than one versions from values
            mappedIds.replaceAll(
                    (key, values) -> {
                        String firstValue = new ArrayList<>(values).get(0);
                        return (firstValue.contains("."))
                                ? Set.of(firstValue.substring(0, firstValue.indexOf(".")))
                                : values;
                    });
        }
        return mappedIds;
    }

    private Map.Entry<String, String> getMappedId(String id) {
        Map.Entry<String, String> result = null;
        if (UNIPROTKB_ACCESSION_WITH_SEQUENCE_OR_VERSION.matcher(id).matches()) {
            if (id.contains(".")) {
                result = new AbstractMap.SimpleEntry<>(id.substring(0, id.indexOf(".")), id);
            }
            if (id.contains("[")) {
                result = new AbstractMap.SimpleEntry<>(id.substring(0, id.indexOf("[")), id);
            }
        } else if(id.contains("_") && UNIPROTKB_ACCESSION_REGEX.matcher(id.split("_")[0]).matches()){
            result = new AbstractMap.SimpleEntry<>(id.substring(0, id.indexOf("_")), id);
        }
        return result;
    }

    private Optional<ProblemPair> getOptionalLimitError(
            Integer maxToIdsAllowed, IdMappingResult result) {
        if (Utils.notNullNotEmpty(result.getMappedIds())
                && result.getMappedIds().size() > maxToIdsAllowed) {
            return Optional.of(
                    new ProblemPair(
                            LIMIT_EXCEED_ERROR.getCode(),
                            LIMIT_EXCEED_ERROR.getErrorMessage(maxToIdsAllowed)));
        }
        return Optional.empty();
    }

    private Optional<ProblemPair> getOptionalEnrichmentWarning(
            IdMappingJobRequest request, Integer maxToUniProtIdsAllowed, IdMappingResult result) {
        if (isMappedToUniProtDBId(request.getTo())
                && Utils.notNullNotEmpty(result.getMappedIds())
                && result.getMappedIds().size() > maxToUniProtIdsAllowed) {
            return Optional.of(
                    new ProblemPair(
                            ENRICHMENT_WARNING.getCode(),
                            ENRICHMENT_WARNING.getErrorMessage(maxToUniProtIdsAllowed)));
        }
        return Optional.empty();
    }

    private boolean isMappedToUniProtDBId(String toDB) {
        return IdMappingJobServiceImpl.UNIREF_SET.contains(toDB)
                || IdMappingJobServiceImpl.UNIPARC.contains(toDB)
                || IdMappingJobServiceImpl.UNIPROTKB_SET.contains(toDB);
    }
}
