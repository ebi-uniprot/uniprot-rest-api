package uk.ac.ebi.uniprot.configure.api.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import uk.ac.ebi.uniprot.configure.uniprot.domain.*;
import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.*;
import uk.ac.ebi.uniprot.cv.xdb.DatabaseCategory;
import uk.ac.ebi.uniprot.cv.xdb.UniProtXDbTypeDetail;
import uk.ac.ebi.uniprot.cv.xdb.UniProtXDbTypes;

@Service
public class UniProtConfigureService {
	private static final String ANY_CROSS_REFERENCE_NAME = "Any cross-reference";
	private static final String ANY_CROSS_REFERENCE_VALUE = "any";
	private static final String ANY_DB_GROUP_NAME = "Any";
	private static final DatabaseGroup ANY_DB_GROUP = new DatabaseGroupImpl(ANY_DB_GROUP_NAME,
			Arrays.asList(new TupleImpl(ANY_CROSS_REFERENCE_NAME, ANY_CROSS_REFERENCE_VALUE)));

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

		List<DatabaseGroup> databases = Arrays.stream(DatabaseCategory.values())
				.filter(dbCat -> dbCat != DatabaseCategory.UNKNOWN)
				.map(dbCat -> getDatabaseGroup(dbCat))
				.filter(dbGroup -> !dbGroup.getItems().isEmpty())
				.collect(Collectors.toList());

		// add the Any DB Group
		databases.add(ANY_DB_GROUP);

		return databases;
	}

	public List<FieldGroup> getDatabaseFields() {
		return UniProtResultFields.INSTANCE.getDatabaseFields();
	}
	public List<FieldGroup> getResultFields() {
		return UniProtResultFields.INSTANCE.getResultFields();
	}

	private Tuple convertToTuple(UniProtXDbTypeDetail dbType) {
		return new TupleImpl(dbType.getName(), dbType.getName().toLowerCase());
	}

	private DatabaseGroup getDatabaseGroup(DatabaseCategory dbCategory){
		List<UniProtXDbTypeDetail> dbTypes = UniProtXDbTypes.INSTANCE.getDBTypesByCategory(dbCategory);
		List<Tuple> databaseTypes = dbTypes.stream().map(this::convertToTuple).collect(Collectors.toList());
		return new DatabaseGroupImpl(dbCategory.getDisplayName(), databaseTypes);
	}
}
