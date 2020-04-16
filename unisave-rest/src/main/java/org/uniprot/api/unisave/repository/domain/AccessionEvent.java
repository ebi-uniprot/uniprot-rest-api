package org.uniprot.api.unisave.repository.domain;

/**
 * Created with IntelliJ IDEA. User: wudong Date: 12/11/2013 Time: 14:14 To change this template use
 * File | Settings | File Templates.
 */
public interface AccessionEvent {
    EventTypeEnum getEventType();

    String getTargetAccession();

    Release getEventRelease();
}
