package org.uniprot.api.common.repository.search.facet;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.TupleStream;

public class FacetTupleStreamConverter
        extends FacetConverter<TupleStream, SolrStreamFacetResponse> {
    private static final String LENGTH_STR = "length";
    private static final String COUNT_STAR_STR = "count(*)";
    private static final String RANGE_1_200 = "[1,200]";
    private static final String RANGE_201_400 = "[201,400]";
    private static final String RANGE_401_600 = "[401,600]";
    private static final String RANGE_601_800 = "[601,800]";
    private static final String RANGE_801_PLUS = "[801,*]";
    private static final Map<String, Integer> RANGE_INDEX = new HashMap<>();

    static {
        RANGE_INDEX.put(RANGE_1_200, 5);
        RANGE_INDEX.put(RANGE_201_400, 4);
        RANGE_INDEX.put(RANGE_401_600, 3);
        RANGE_INDEX.put(RANGE_601_800, 2);
        RANGE_INDEX.put(RANGE_801_PLUS, 1);
    }

    private final FacetConfig facetConfig;
    private final String idFieldName;

    public FacetTupleStreamConverter(String idFieldName, FacetConfig facetConfig) {
        this.idFieldName = idFieldName;
        this.facetConfig = facetConfig;
    }

    @Override
    protected FacetConfig getFacetConfig() {
        return this.facetConfig;
    }

    @Override
    public SolrStreamFacetResponse convert(TupleStream tupleStream, List<String> facetList) {
        Map<String, List<Pair<String, Long>>> facetNameValuesMap =
                computeFacetValuesMap(tupleStream);
        List<String> accessions =
                facetNameValuesMap.getOrDefault(idFieldName, new ArrayList<>()).stream()
                        .map(Pair::getLeft)
                        .collect(Collectors.toList());
        List<Facet> facets =
                facetNameValuesMap.entrySet().stream()
                        .filter(entry -> !idFieldName.equals(entry.getKey()))
                        .map(this::convertSolrStreamFacet)
                        .collect(Collectors.toList());
        return new SolrStreamFacetResponse(facets, accessions);
    }

    private Map<String, List<Pair<String, Long>>> computeFacetValuesMap(TupleStream tupleStream) {
        Map<String, List<Pair<String, Long>>> facetNameValue = new LinkedHashMap<>();
        try {
            tupleStream.open();
            while (true) {
                Tuple tuple = tupleStream.read();
                if (tuple.EOF) {
                    break;
                }
                Map<Object, Object> map = tuple.getMap();
                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    String facetName = String.valueOf(entry.getKey());
                    if (idFieldName.equals(facetName)
                            || facetConfig.getFacetPropertyMap().containsKey(facetName)) {
                        String facetValue = String.valueOf(entry.getValue());
                        Long facetCount = (long) map.getOrDefault(COUNT_STAR_STR, 0L);
                        List<Pair<String, Long>> valueCount = new ArrayList<>();
                        if (facetNameValue.containsKey(facetName)) {
                            valueCount = facetNameValue.get(facetName);
                        }
                        Pair<String, Long> pair = Pair.of(facetValue, facetCount);
                        valueCount.add(pair);
                        facetNameValue.put(facetName, valueCount);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        } finally {
            closeTupleStream(tupleStream);
        }
        return facetNameValue;
    }

    private void closeTupleStream(TupleStream tupleStream) {
        try {
            tupleStream.close();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private Facet convertSolrStreamFacet(Map.Entry<String, List<Pair<String, Long>>> facetValues) {
        if (!LENGTH_STR.equals(facetValues.getKey())) {
            List<FacetItem> values =
                    facetValues.getValue().stream()
                            .map(pair -> createFacetItem(pair, facetValues.getKey()))
                            .collect(Collectors.toList());

            int limit = getFacetLimit(facetValues.getKey());
            if (values.size() > limit) {
                values = values.subList(0, limit);
            }

            // build a facet
            return Facet.builder()
                    .name(facetValues.getKey())
                    .label(getFacetLabel(facetValues.getKey()))
                    .allowMultipleSelection(allowMultipleSelection(facetValues.getKey()))
                    .values(values)
                    .build();
        } else { // handle length buckets differently
            return convertSolrStreamToRangeFacet(facetValues);
        }
    }

    private int getFacetLimit(String facetName) {
        int limit = facetConfig.getLimit();
        FacetProperty facetProperty =
                getFacetConfig().getFacetPropertyMap().getOrDefault(facetName, null);
        if (facetProperty != null && facetProperty.getLimit() > 0) {
            limit = facetProperty.getLimit();
        }
        return limit;
    }

    private FacetItem createFacetItem(Pair<String, Long> pair, String facetName) {
        return FacetItem.builder()
                .value(pair.getLeft())
                .label(getFacetItemLabel(facetName, pair.getLeft().toLowerCase()))
                .count(pair.getRight())
                .build();
    }

    private Facet convertSolrStreamToRangeFacet(
            Map.Entry<String, List<Pair<String, Long>>> facetValues) {
        Map<String, Long> lengthBucketCount = computeLengthBucketCount(facetValues);
        List<FacetItem> values =
                lengthBucketCount.entrySet().stream()
                        .filter(entry -> entry.getValue() > 0L)
                        .sorted(Map.Entry.comparingByKey())
                        .map(
                                entry ->
                                        FacetItem.builder()
                                                .value(entry.getKey().replace(",", " TO "))
                                                .label(
                                                        getIntervalFacetItemLabel(
                                                                LENGTH_STR, entry.getKey()))
                                                .count(entry.getValue())
                                                .build())
                        .collect(Collectors.toList());
        // return an Interval facet
        return Facet.builder()
                .name(LENGTH_STR)
                .label(getFacetLabel(LENGTH_STR))
                .allowMultipleSelection(allowMultipleSelection(LENGTH_STR))
                .values(values)
                .build();
    }

    private Map<String, Long> computeLengthBucketCount(
            Map.Entry<String, List<Pair<String, Long>>> facetValues) {
        Long bucket1To200 = 0L;
        Long bucket201To400 = 0L;
        Long bucket401To600 = 0L;
        Long bucket601To800 = 0L;
        Long bucket801Onwards = 0L;
        for (Pair<String, Long> pair : facetValues.getValue()) {
            Long value = Long.valueOf(pair.getLeft());
            if (value >= 1L && value <= 200L) {
                bucket1To200 += pair.getRight();
            } else if (value >= 201L && value <= 400L) {
                bucket201To400 += pair.getRight();
            } else if (value >= 401L && value <= 600L) {
                bucket401To600 += pair.getRight();
            } else if (value >= 601L && value <= 800L) {
                bucket601To800 += pair.getRight();
            } else if (value >= 801L) {
                bucket801Onwards += pair.getRight();
            }
        }
        Map<String, Long> buckets = new HashMap<>();
        buckets.put(RANGE_1_200, bucket1To200);
        buckets.put(RANGE_201_400, bucket201To400);
        buckets.put(RANGE_401_600, bucket401To600);
        buckets.put(RANGE_601_800, bucket601To800);
        buckets.put(RANGE_801_PLUS, bucket801Onwards);
        return buckets;
    }
}
