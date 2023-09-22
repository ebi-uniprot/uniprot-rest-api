package org.uniprot.api.uniprotkb.groupby.service.go;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.uniprot.api.uniprotkb.groupby.service.go.client.GOClient;
import org.uniprot.api.uniprotkb.groupby.service.go.client.GoRelation;

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
