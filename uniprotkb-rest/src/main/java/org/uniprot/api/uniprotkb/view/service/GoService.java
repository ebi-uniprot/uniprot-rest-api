package org.uniprot.api.uniprotkb.view.service;

import com.google.common.base.Strings;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.uniprotkb.view.GoRelation;
import org.uniprot.api.uniprotkb.view.GoTerm;
import org.uniprot.api.uniprotkb.view.GoTermResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GoService {
    private final RestTemplate restTemplate;
    private static final String GO_API_PREFIX =
            "https://www.ebi.ac.uk/QuickGO/services/ontology/go/terms/";

    public GoService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    Optional<GoTerm> getChildren(String goId) {
        if (Strings.isNullOrEmpty(goId)) {
            return rootChildren();
        }

        String url = GO_API_PREFIX + goId + "/children";
        GoTermResult result = restTemplate.getForObject(url, GoTermResult.class);
        if ((result == null) || result.getResults().isEmpty()) {
            return Optional.empty();
        } else return Optional.of(result.getResults().get(0));
    }

    private Optional<GoTerm> rootChildren() {
        GoTerm goTerm = new GoTerm();
        goTerm.setId("GO:00000");
        goTerm.setName("root");
        List<GoRelation> children = new ArrayList<>();
        children.add(createGoRelation("GO:0008150", "biological_process", "is_a", true));
        children.add(createGoRelation("GO:0005575", "cellular_component", "is_a", true));
        children.add(createGoRelation("GO:0003674", "molecular_function", "is_a", true));
        goTerm.setChildren(children);
        return Optional.of(goTerm);
    }

    private GoRelation createGoRelation(
            String id, String name, String relation, boolean hasChildren) {
        GoRelation goRelation = new GoRelation();
        goRelation.setId(id);
        goRelation.setName(name);
        goRelation.setRelation(relation);
        goRelation.setHasChildren(hasChildren);
        return goRelation;
    }
}
