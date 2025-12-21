package org.uniprot.api.idmapping.common.service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.service.impl.IdMappingJobServiceImpl;
import org.uniprot.api.idmapping.common.service.impl.PIRServiceImpl;
import org.uniprot.api.rest.output.PredefinedAPIStatus;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.idmapping.IdMappingFieldConfig;
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
    public static final String SEQ_SEP = "[";
    public static final String VERSION_SEP = ".";
    public static final String ID_SEP = "_";

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
        IdMappingResult.IdMappingResultBuilder idMappingResultBuilder = IdMappingResult.builder();
        HttpStatus statusCode = response.getStatusCode();
        if (statusCode.equals(HttpStatus.OK)) {
            if (response.hasBody()) {
                Map<String, Set<String>> mappedRequestIds = getMappedRequestIds(request);
                IdMappingResult.IdMappingResultBuilder finalIdMappingResultBuilder =
                        idMappingResultBuilder;
                response.getBody()
                        .lines()
                        .filter(line -> !line.startsWith("Taxonomy ID:"))
                        .filter(Utils::notNullNotEmpty)
                        .forEach(
                                line ->
                                        convertLine(
                                                line,
                                                request,
                                                mappedRequestIds,
                                                finalIdMappingResultBuilder));
                idMappingResultBuilder =
                        populateErrorOrWarning(
                                request,
                                idMappingResultBuilder,
                                maxToUniProtIdsAllowed,
                                maxToIdsAllowed);
            }
        } else {
            throw new HttpServerErrorException(statusCode, "PIR id-mapping service error");
        }

        return idMappingResultBuilder.build();
    }

    public IdMappingResult.IdMappingResultBuilder populateErrorOrWarning(
            IdMappingJobRequest request,
            IdMappingResult.IdMappingResultBuilder idMappingResultBuilder,
            Integer maxToIdsEnriched,
            Integer maxToIdsAllowed) {
        // populate  error  if needed
        Optional<ProblemPair> optError =
                getOptionalLimitError(maxToIdsAllowed, idMappingResultBuilder.build());
        if (optError.isPresent()) {
            idMappingResultBuilder.clearMappedIds();
            idMappingResultBuilder.clearUnmappedIds();
            idMappingResultBuilder.error(optError.get());
        } else {
            // populate warning if needed
            Optional<ProblemPair> optWarning =
                    getOptionalEnrichmentWarning(
                            request, maxToIdsEnriched, idMappingResultBuilder.build());
            if (optWarning.isPresent()) {
                idMappingResultBuilder.warning(optWarning.get());
            }
        }
        return idMappingResultBuilder;
    }

    private void convertLine(
            String line,
            IdMappingJobRequest request,
            Map<String, Set<String>> mappedIds,
            IdMappingResult.IdMappingResultBuilder builder) {
        if (line.startsWith("MSG:")) {
            if (line.endsWith(NO_MATCHES_PIR_RESPONSE)) {
                builder.unmappedIds(Arrays.asList(request.getIds().split(",")));
            }
        } else {
            String[] rowParts = line.split("\t");
            Set<String> fromValues = getFromValue(mappedIds, rowParts[0]);
            if (rowParts.length == 1) {
                fromValues.stream().forEach(builder::unmappedId);
            } else {
                for (String fromValue : fromValues) {
                    Arrays.stream(rowParts[1].split(";"))
                            .map(
                                    toValue ->
                                            IdMappingStringPair.builder()
                                                    .from(fromValue)
                                                    .to(toValue)
                                                    .build())
                            .forEach(pair -> convertIdMappingPair(request.getTo(), pair, builder));
                }
            }
        }
    }

    private void convertIdMappingPair(
            String to, IdMappingStringPair pair, IdMappingResult.IdMappingResultBuilder builder) {
        if (isValidIdPattern(to, pair.getTo())) {
            builder.mappedId(pair);
        } else {
            builder.suggestedId(pair);
        }
    }

    private Set<String> getFromValue(Map<String, Set<String>> mappedIds, String fromValue) {
        return mappedIds.getOrDefault(fromValue, Set.of(fromValue));
    }

    Map<String, Set<String>> getMappedRequestIds(IdMappingJobRequest request) {
        Map<String, Set<String>> mappedIds = new HashMap<>();
        if (IdMappingFieldConfig.ACC_ID_STR.equals(request.getFrom())
                && (request.getIds().contains(SEQ_SEP)
                        || request.getIds().contains(VERSION_SEP)
                        || request.getIds().contains(ID_SEP))) {
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
                        return (firstValue.contains(VERSION_SEP))
                                ? Set.of(firstValue.substring(0, firstValue.indexOf(VERSION_SEP)))
                                : values;
                    });
        }
        return mappedIds;
    }

    private Map.Entry<String, String> getMappedId(String id) {
        Map.Entry<String, String> result = null;
        if (PIRServiceImpl.UNIPROTKB_ACCESSION_WITH_SEQUENCE_OR_VERSION.matcher(id).matches()) {
            if (id.contains(VERSION_SEP)) {
                result =
                        new AbstractMap.SimpleEntry<>(id.substring(0, id.indexOf(VERSION_SEP)), id);
            }
            if (id.contains(SEQ_SEP)) {
                result = new AbstractMap.SimpleEntry<>(id.substring(0, id.indexOf(SEQ_SEP)), id);
            }
        } else if (id.contains(ID_SEP)
                && PIRServiceImpl.UNIPROTKB_ACCESSION_REGEX
                        .matcher(id.split(ID_SEP)[0])
                        .matches()) {
            result = new AbstractMap.SimpleEntry<>(id.substring(0, id.indexOf(ID_SEP)), id);
        }
        return result;
    }

    private Optional<ProblemPair> getOptionalLimitError(
            Integer maxToIdsAllowed, IdMappingResult result) {
        if (Utils.notNullNotEmpty(result.getMappedIds())
                && result.getMappedIds().size() > maxToIdsAllowed) {
            return Optional.of(
                    new ProblemPair(
                            PredefinedAPIStatus.LIMIT_EXCEED_ERROR.getCode(),
                            PredefinedAPIStatus.LIMIT_EXCEED_ERROR.getErrorMessage(
                                    maxToIdsAllowed)));
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
                            PredefinedAPIStatus.ENRICHMENT_WARNING.getCode(),
                            PredefinedAPIStatus.ENRICHMENT_WARNING.getErrorMessage(
                                    maxToUniProtIdsAllowed)));
        }
        return Optional.empty();
    }

    private boolean isMappedToUniProtDBId(String toDB) {
        return IdMappingJobServiceImpl.UNIREF_SET.contains(toDB)
                || IdMappingJobServiceImpl.UNIPARC.contains(toDB)
                || IdMappingJobServiceImpl.UNIPROTKB_SET.contains(toDB);
    }
}
