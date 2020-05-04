package org.uniprot.api.unisave.repository.domain;

import java.util.List;

/**
 * Created with IntelliJ IDEA. User: wudong Date: 12/11/2013 Time: 14:12 To change this template use
 * File | Settings | File Templates.
 */
public interface AccessionStatusInfo {
    public String getAccession();

    public List<AccessionEvent> getEvents();
}
