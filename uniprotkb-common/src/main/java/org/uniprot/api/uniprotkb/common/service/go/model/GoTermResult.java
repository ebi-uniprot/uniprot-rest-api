package org.uniprot.api.uniprotkb.common.service.go.model;

import java.util.List;

import lombok.Data;

@Data
public class GoTermResult {
    private List<GoTerm> results;
}
