package uk.ac.ebi.uniprot.api.uniprotkb.service.filters;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum FilterComponentType {
	PROTEIN_EXISTENCE,
	SECONDARY_ACCESSION,
	PROTEIN_NAME,
	GENE,
	ORGANISM,
	ORGANISM_HOST,
	GENE_LOCATION,
	COMMENT(true),
	KEYWORD,
	FEATURE(true),
	SEQUENCE,
	LENGTH,
	MASS,
	XREF (true),
	REFERENCE;
	boolean hasSubcomponent;
	FilterComponentType(){
		this(false);
	}
	FilterComponentType(boolean hasSubcomponent){
		this.hasSubcomponent =hasSubcomponent;
	}
	public List<String> getAllFilterNames(){
		return Arrays.stream(
		FilterComponentType.values())
		.map(val ->val.name().toLowerCase())
		.collect(Collectors.toList());
	}
}
