package uk.ac.ebi.uniprot.configure.uniprot.domain.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.ebi.uniprot.configure.uniprot.domain.EvidenceGroup;
import uk.ac.ebi.uniprot.configure.uniprot.domain.SearchEvidences;



public enum AnnotationEvidences implements SearchEvidences {
	INSTANCE;
	private static final String FILENAME ="uniprot/annotation_evidence.json";
	private  List< EvidenceGroup> evidences = new ArrayList<>();  
 
	 AnnotationEvidences(){
		init();
		 }
	
	 void init() {
		 final ObjectMapper objectMapper = new ObjectMapper();
		 try ( InputStream is = AnnotationEvidences.class.getClassLoader()
				 .getResourceAsStream(FILENAME);){
			 List< EvidenceGroupImpl> evidences = objectMapper.readValue(is,  new TypeReference<List<EvidenceGroupImpl>>(){});
			 this.evidences.addAll(evidences);
		 }catch(Exception e) {
			 throw new RuntimeException (e);
		 }
		 
	
	}
	public List<EvidenceGroup> getEvidences() {
		return evidences;
	}

	
			 
}
