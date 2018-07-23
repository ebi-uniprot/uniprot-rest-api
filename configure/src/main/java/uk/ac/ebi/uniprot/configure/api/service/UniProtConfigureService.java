package uk.ac.ebi.uniprot.configure.api.service;

import java.io.File;
import java.util.List;

import org.springframework.stereotype.Service;

import uk.ac.ebi.uniprot.configure.uniprot.domain.DatabaseGroup;
import uk.ac.ebi.uniprot.configure.uniprot.domain.EvidenceGroup;
import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.AnnotationEvidences;
import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.Databases;
import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.GoEvidences;
import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.UniProtSearchItems;

@Service
public class UniProtConfigureService {
	private static final String DEFAULT_FILEPATH = "uniprot";
	private static final String UNIPROT_SEARCH_FILE = "uniprot_search.json";
	private final String filepath;
	private UniProtSearchItems searchItems = null;

	public UniProtConfigureService() {
		this(DEFAULT_FILEPATH);
	}

	public UniProtConfigureService(String filepath) {
		this.filepath = filepath;
		fetch();

	}

	public UniProtSearchItems getUniProtSearchItems() {
		return searchItems;
	}
	public List<EvidenceGroup>  getAnnotationEvidences(){
		return AnnotationEvidences.INSTANCE.getEvidences();
	}

	
	public List<EvidenceGroup>  getGoEvidences(){
		return GoEvidences.INSTANCE.getEvidences();
	}

	public List<DatabaseGroup>  getDatabases(){
		return Databases.INSTANCE.getDatabases();
	}
	
	private void fetch() {
		try {
			String filename = this.getClass().getClassLoader().getResource(filepath).getFile() + File.separator
					+ UNIPROT_SEARCH_FILE;
			searchItems = UniProtSearchItems.readFromFile(filename);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
