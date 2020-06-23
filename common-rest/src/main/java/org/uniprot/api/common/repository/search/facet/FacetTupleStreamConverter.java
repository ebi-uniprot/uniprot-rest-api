package org.uniprot.api.common.repository.search.facet;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.TupleStream;

public class FacetTupleStreamConverter extends FacetConverter<TupleStream, List<Facet>> {
    private static final String LENGTH_STR = "length";
    private static final Map<String, Integer> RANGE_INDEX = new HashMap<>();

    static {
        RANGE_INDEX.put("[1,200]", 5);
        RANGE_INDEX.put("[201,400]", 4);
        RANGE_INDEX.put("[401,600]", 3);
        RANGE_INDEX.put("[601,800]", 2);
        RANGE_INDEX.put("[801,*]", 1);
    }

    private FacetConfig facetConfig;

    public FacetTupleStreamConverter(FacetConfig facetConfig) {
        this.facetConfig = facetConfig;
    }

    @Override
    protected FacetConfig getFacetConfig() {
        return this.facetConfig;
    }

    @Override
    public List<Facet> convert(TupleStream tupleStream) {
        Map<String, List<Pair<String, Long>>> facetNameValuesMap =
                computeFacetValuesMap(tupleStream);
        List<Facet> facetResult =
                facetNameValuesMap.entrySet().stream()
                        .map(this::convertSolrStreamFacet)
                        .collect(Collectors.toList());
        return facetResult;
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
                    if (this.facetConfig.getFacetPropertyMap().containsKey(facetName)) {
                        String facetValue = String.valueOf(entry.getValue());
                        Long facetCount = (long) map.get("count(*)");
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
            try {
                tupleStream.close();
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return facetNameValue;
    }

    private Facet convertSolrStreamFacet(Map.Entry<String, List<Pair<String, Long>>> facetValues) {
        if (!LENGTH_STR.equals(facetValues.getKey())) {
            List<FacetItem> values =
                    facetValues.getValue().stream()
                            .map(
                                    pair ->
                                            FacetItem.builder()
                                                    .value(pair.getLeft())
                                                    .label(
                                                            getFacetItemLabel(
                                                                    facetValues.getKey(),
                                                                    pair.getLeft()))
                                                    .count(pair.getRight())
                                                    .build())
                            .collect(Collectors.toList());
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

    private Facet convertSolrStreamToRangeFacet(
            Map.Entry<String, List<Pair<String, Long>>> facetValues) {
        Map<String, Long> lengthBucketCount = computeLengthBucketCount(facetValues);
        List<FacetItem> values =
                lengthBucketCount.entrySet().stream()
                        .filter(entry -> entry.getValue() > 0L)
                        .sorted(new LengthBucketComparator())
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
        buckets.put("[1,200]", bucket1To200);
        buckets.put("[201,400]", bucket201To400);
        buckets.put("[401,600]", bucket401To600);
        buckets.put("[601,800]", bucket601To800);
        buckets.put("[801,*]", bucket801Onwards);
        return buckets;
    }

    private class LengthBucketComparator implements Comparator<Map.Entry<String, Long>> {
        @Override
        public int compare(Map.Entry<String, Long> o1, Map.Entry<String, Long> o2) {
            int valueCompare = Long.compare(o2.getValue(), o1.getValue());
            if (valueCompare != 0) {
                return valueCompare;
            } else {
                return RANGE_INDEX.get(o2.getKey()).compareTo(RANGE_INDEX.get(o1.getKey()));
            }
        }
    }
}
