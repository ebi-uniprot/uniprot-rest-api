package uk.ac.ebi.uniprot.common.repository.search.facet;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author lgonzales
 */
public class FakeFacetConfigConverter extends GenericFacetConfig implements FacetConfigConverter{

    @Override
    public Map<String, FacetProperty> getFacetPropertyMap() {
        Map<String,FacetProperty> result = new HashMap<>();

        FacetProperty fragment = new FacetProperty();
        fragment.setLabel("Fragment");
        fragment.setAllowmultipleselection(false);
        Map<String,String> fragmentValues = new HashMap<>();
        fragmentValues.put("true","Yes");
        fragmentValues.put("false","No");
        fragment.setValue(fragmentValues);
        result.put("fragment",fragment);

        FacetProperty reviewed = new FacetProperty();
        reviewed.setLabel("Status");
        reviewed.setAllowmultipleselection(false);
        Map<String,String> reviewedValues = new HashMap<>();
        reviewedValues.put("true","Reviewed (Swiss-Prot)");
        reviewedValues.put("false","Unreviewed (TrEMBL)");
        reviewed.setValue(reviewedValues);
        result.put("reviewed",reviewed);


        FacetProperty organism = new FacetProperty();
        organism.setLabel("Popular organisms");
        organism.setAllowmultipleselection(true);
        result.put("popular_organism",organism);

        return result;
}
}
