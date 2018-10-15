package uk.ac.ebi.uniprot.uuw.advanced.search.model.download;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.EvidencedString;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.Organism;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.Organism.OrganismName;
import uk.ac.ebi.uniprot.dataservice.restful.features.domain.Evidence;

public class DownloadableUtil {

	public static String evidencesToString(List<Evidence> evidences) {
		if((evidences ==null) || evidences.isEmpty())
			return "";
		return evidences.stream().map(Evidence::toString).collect(Collectors.joining(", ", "{", "}"));
	}
	public static String convertEvidencedString(EvidencedString val) {
		StringBuilder sb = new StringBuilder();
		sb.append(val.getValue());		
		String evStr =DownloadableUtil.evidencesToString(val.getEvidences());
		if(!Strings.isNullOrEmpty(evStr)) {
			sb.append(" ").append(evStr);
		}
		return sb.toString();
	}
	
	public static String convertOrganism(Organism organism) {
		StringBuilder sb = new StringBuilder();
		Optional<OrganismName> scientific = organism.getNames().stream()
				.filter(val -> val.getType().equals("scientific")).findFirst();
		if (scientific.isPresent()) {
			sb.append(scientific.get().getValue());
		}
		Optional<OrganismName> common = organism.getNames().stream().filter(val -> val.getType().equals("common"))
				.findFirst();
		if (common.isPresent()) {
			sb.append(" (");
			sb.append(common.get().getValue());
			sb.append(")");
		}
		List<OrganismName> synonyms = organism.getNames().stream().filter(val -> val.getType().equals("synonym"))
				.collect(Collectors.toList());
		if (!synonyms.isEmpty()) {
			sb.append(" (").append(synonyms.stream().map(val -> val.getValue()).collect(Collectors.joining(", ")))
					.append(")");
		}
		return sb.toString();
	}
	
}
