package org.uniprot.api.uniprotkb.groupby.service.go.client;

import java.util.List;
import java.util.Optional;

public interface GOClient {
    List<GoRelation> getChildren(String goId);

    Optional<GoRelation> getGoEntry(String goId);
}
