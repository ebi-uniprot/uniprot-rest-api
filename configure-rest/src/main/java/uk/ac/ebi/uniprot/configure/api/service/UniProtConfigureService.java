package uk.ac.ebi.uniprot.configure.api.service;

import java.util.List;

import org.springframework.stereotype.Service;

import uk.ac.ebi.uniprot.configure.uniprot.domain.DatabaseGroup;
import uk.ac.ebi.uniprot.configure.uniprot.domain.EvidenceGroup;
import uk.ac.ebi.uniprot.configure.uniprot.domain.FieldGroup;
import uk.ac.ebi.uniprot.configure.uniprot.domain.SearchItem;
import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.AnnotationEvidences;
import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.Databases;
import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.GoEvidences;
import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.UniProtResultFields;
import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.UniProtSearchItems;

@Service
public class UniProtConfigureService {

	public List<SearchItem> getUniProtSearchItems() {
		return UniProtSearchItems.INSTANCE.getSearchItems();
	}

	public List<EvidenceGroup> getAnnotationEvidences() {
		return AnnotationEvidences.INSTANCE.getEvidences();
	}

	public List<EvidenceGroup> getGoEvidences() {
		return GoEvidences.INSTANCE.getEvidences();
	}

	public List<DatabaseGroup> getDatabases() {
		return Databases.INSTANCE.getDatabases();
	}

	public List<FieldGroup> getDatabaseFields() {
		return UniProtResultFields.INSTANCE.getDatabaseFields();
	}
	public List<FieldGroup> getResultFields() {
		return UniProtResultFields.INSTANCE.getResultFields();
	}
}
