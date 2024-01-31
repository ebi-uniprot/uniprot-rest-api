package org.uniprot.api.uniprotkb.common.service.go;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.uniprot.api.uniprotkb.common.service.go.client.GOClient;
import org.uniprot.api.uniprotkb.common.service.go.model.GoRelation;

@Service
public class GOService {
    private final GOClient goClient;

    public GOService(GOClient goClient) {
        this.goClient = goClient;
    }

    public List<GoRelation> getChildren(String parentGo) {
        return goClient.getChildren(parentGo);
    }

    public Optional<GoRelation> getGoRelation(String goId) {
        return goClient.getGoEntry(goId);
    }
}
