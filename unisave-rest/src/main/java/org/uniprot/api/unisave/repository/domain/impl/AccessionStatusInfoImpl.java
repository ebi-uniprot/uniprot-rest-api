package org.uniprot.api.unisave.repository.domain.impl;

import java.util.ArrayList;
import java.util.List;

import org.uniprot.api.unisave.repository.domain.AccessionEvent;
import org.uniprot.api.unisave.repository.domain.AccessionStatusInfo;

import lombok.Data;

/**
 * Created with IntelliJ IDEA. User: wudong Date: 12/11/2013 Time: 14:31 To change this template use
 * File | Settings | File Templates.
 */
@Data
public class AccessionStatusInfoImpl implements AccessionStatusInfo {
    private String accession;
    private List<AccessionEvent> events = new ArrayList<>();
}
