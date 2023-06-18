package org.uniprot.api.uniprotkb.groupby.service.go.client;

import com.google.common.base.Strings;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
public class GOClientImpl implements GOClient {
    public static final String GO_0008150 = "GO:0008150";
    public static final String GO_0005575 = "GO:0005575";
    public static final String GO_0003674 = "GO:0003674";
    public static final String BIOLOGICAL_PROCESS = "biological_process";
    public static final String CELLULAR_COMPONENT = "cellular_component";
    public static final String MOLECULAR_FUNCTION = "molecular_function";
    private static final String GO_API_PREFIX =
            "https://www.ebi.ac.uk/QuickGO/services/ontology/go/terms/";
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public List<GoRelation> getChildren(String goId) {
        if (Strings.isNullOrEmpty(goId)) {
            return rootChildren().getChildren();
        }

        String url = GO_API_PREFIX + goId + "/children";
        GoTermResult result = restTemplate.getForObject(url, GoTermResult.class);
        if (result == null || result.getResults().isEmpty()) {
            return List.of();
        } else {
            return result.getResults().get(0).getChildren();
        }
    }

    private GoTerm rootChildren() {
        GoTerm goTerm = new GoTerm();
        goTerm.setId("GO:00000");
        goTerm.setName("root");
        List<GoRelation> children = new ArrayList<>();
        children.add(getGoRelation(GO_0008150, BIOLOGICAL_PROCESS));
        children.add(getGoRelation(GO_0005575, CELLULAR_COMPONENT));
        children.add(getGoRelation(GO_0003674, MOLECULAR_FUNCTION));
        goTerm.setChildren(children);
        return goTerm;
    }

    private GoRelation getGoRelation(String id, String name) {
        GoRelation goRelation = new GoRelation();
        goRelation.setId(id);
        goRelation.setName(name);
        goRelation.setRelation("is_a");
        goRelation.setHasChildren(true);
        return goRelation;
    }
}
