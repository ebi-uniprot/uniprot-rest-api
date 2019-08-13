package org.uniprot.api.common.repository.search.facet;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.FacetProperty;

/**
 *
 * @author lgonzales
 */
public class FakeFacetConfig extends FacetConfig {

    @Override
    public Collection<String> getFacetNames() {
        return null;
    }

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


        // Interval facet
        FacetProperty length = new FacetProperty();
        length.setLabel("Sequence Length");
        length.setAllowmultipleselection(false);
        Map<String, String> lengthIntervals = new HashMap<>();
        lengthIntervals.put("1", "[1,200]");
        lengthIntervals.put("2", "[201,400]");
        lengthIntervals.put("3", "[401,600]");
        lengthIntervals.put("4", "[601,800]");
        lengthIntervals.put("5", "[801,*]");

        length.setInterval(lengthIntervals);

        Map<String, String> lengthLabels = new HashMap<>();
        lengthLabels.put("1", "1 - 200");
        lengthLabels.put("2", "201 - 400");
        lengthLabels.put("3", "401 - 600");
        lengthLabels.put("4", "601 - 800");
        lengthLabels.put("5", ">= 801");

        length.setValue(lengthLabels);

        result.put("length", length);
        return result;
    }
}
