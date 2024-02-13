package org.uniprot.api.uniprotkb.common.service.go.client;

import java.util.List;
import java.util.Optional;

import org.uniprot.api.uniprotkb.common.service.go.model.GoRelation;

public interface GOClient {
    List<GoRelation> getChildren(String goId);

    Optional<GoRelation> getGoEntry(String goId);
}
