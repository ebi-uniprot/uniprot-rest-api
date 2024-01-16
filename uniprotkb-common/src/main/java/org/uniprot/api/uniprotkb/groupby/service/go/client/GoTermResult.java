package org.uniprot.api.uniprotkb.groupby.service.go.client;

import java.util.List;

import lombok.Data;

@Data
public class GoTermResult {
    private List<GoTerm> results;
}
