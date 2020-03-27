package org.uniprot.api.unisave.repository.domain.impl;

import org.uniprot.api.unisave.repository.domain.AccessionEvent;
import org.uniprot.api.unisave.repository.domain.AccessionStatusInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: wudong
 * Date: 12/11/2013
 * Time: 14:31
 * To change this template use File | Settings | File Templates.
 */
public class AccessionStatusInfoImpl implements AccessionStatusInfo {

    private String accession;
    private List<AccessionEvent> events = new ArrayList<AccessionEvent>();

    @Override
    public String getAccession() {
        return accession;
    }

    @Override
    public List<AccessionEvent> getEvents() {
        return events;
    }

    public void setAccession(String acc){
        this.accession = acc;
    }

}
