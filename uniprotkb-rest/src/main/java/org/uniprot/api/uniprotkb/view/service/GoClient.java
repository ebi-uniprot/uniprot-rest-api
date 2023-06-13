package org.uniprot.api.uniprotkb.view.service;

import org.uniprot.api.uniprotkb.view.GoRelation;

import java.util.List;

public interface GoClient {
    List<GoRelation> getChildren(String goId);
}
