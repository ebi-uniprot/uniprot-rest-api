package uk.ac.ebi.uniprot.api.uniprotkb.view.model;

import java.util.List;
import java.util.stream.Collectors;

import lombok.Data;

@Data
public class TaxonomyNode {
	private long taxonomyId;
	private String mnemonic;
	private String scientificName;
	private String commonName;
	private String synonym;
	private String rank;
	private String superregnum;
	private String parentLink;
	private List<String> childrenLinks;
	private List<String> siblingsLinks;

	public String getFullName() {
    	StringBuilder sb = new StringBuilder();
    	sb.append(scientificName);
    	if((commonName !=null) && !commonName.isEmpty()) {   
             sb.append(" (");
             sb.append(commonName);
             sb.append(")");
         }
    	if((synonym !=null) && !synonym.isEmpty()) {           
         		sb.append(" (");
             sb.append(synonym);
             sb.append(")");
         }
    	return sb.toString();
    }
}
