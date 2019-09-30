package org.uniprot.api.uniprotkb.view;

import java.util.List;

import lombok.Data;

@Data
public class GoTermResult {
    private List<GoTerm> results;
}
