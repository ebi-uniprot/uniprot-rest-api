package org.uniprot.api.configure.uniprot.domain.model;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AdvanceSearchTerm implements Serializable, Comparable<AdvanceSearchTerm> {
    private String id;
    @JsonIgnore private String parentId;
    @JsonIgnore private Integer childNumber;
    @JsonIgnore private Integer seqNumber;
    private String label;
    private String itemType;
    private String term;
    private String dataType;
    private String fieldType;
    private String description;
    private String example;
    private String autoComplete;
    private String regex;
    private List<Value> values;
    private List<AdvanceSearchTerm> items;

    @Override
    public int compareTo(AdvanceSearchTerm that) {
        Integer thisChildNumber = this.getChildNumber();
        Integer thatChildNumber = that.getChildNumber();
        if (shouldCompareSeqNumber(thisChildNumber, thatChildNumber)) {
            return this.getSeqNumber().compareTo(that.getSeqNumber());
        } else {
            return thisChildNumber.compareTo(thatChildNumber);
        }
    }

    private boolean shouldCompareSeqNumber(Integer thisChildNumber, Integer thatChildNumber) {
        return thisChildNumber == null
                || thatChildNumber == null
                || thisChildNumber.compareTo(thatChildNumber) == 0;
    }

    @Data
    @AllArgsConstructor
    public static class Value {
        private String name;
        private String value;
    }
}
