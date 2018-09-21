package uk.ac.ebi.uniprot.uuw.advanced.search.model.download;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.Protein;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.ProteinName.Name;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.ProteinName;

public class DownloadableProtein implements Downloadable {
	private static final String EC2 = "EC";
	private static final String SQUARE_BLACKET_RIGHT = "]";
	private static final String SQUARE_BLACKET_LEFT = "[";
	private static final String SEMICOLON = "; ";
	private static final String CLEAVED_INTO = "Cleaved into:";
	private static final String INCLUDES = "Includes:";
	private static final String CD_ANTIGEN = "CD antigen";
	private static final String BIOTECH = "biotech";
	private static final String ALLERGEN = "allergen";
	private static final String BLACKET_RIGHT = ")";
	private static final String BLACKET_LEFT = "(";
	private static final String SPACE = " ";
	private static final String DELIMITER = ", ";
	public static final String FIELD = "protein_name";
	private final Protein protein;

	public DownloadableProtein(Protein protein) {
		this.protein = protein;
	}

	@Override
	public Map<String, String> getData() {
		StringBuilder sb = new StringBuilder();
		if (protein.getRecommendedName() != null) {
			sb.append(getDownloadStringFromName(protein.getRecommendedName()));
		}
		if ((protein.getAlternativeName() != null) && !protein.getAlternativeName().isEmpty() ){
			if (sb.length() > 0) {
				sb.append(SPACE);
			}
			sb.append(protein.getAlternativeName().stream()
					.map(val -> BLACKET_LEFT + getDownloadStringFromName(val) + BLACKET_RIGHT)
					.collect(Collectors.joining(SPACE)));
		}
		if ((protein.getSubmittedName() != null) && !protein.getSubmittedName().isEmpty()) {
			sb.append(getDownloadStringFromName(protein.getSubmittedName().get(0)));
			String data = protein.getSubmittedName().stream().skip(1)
					.map(val -> BLACKET_LEFT + getDownloadStringFromName(val) + BLACKET_RIGHT)
					.collect(Collectors.joining(SPACE));
			if (!Strings.isNullOrEmpty(data)) {
				sb.append(SPACE).append(data);
			}
		}
		if (protein.getAllergenName() != null) {
			sb.append(SPACE).append(BLACKET_LEFT).append(ALLERGEN).append(SPACE)
					.append(protein.getAllergenName().getValue()).append(BLACKET_RIGHT);
		}
		if (protein.getBiotechName() != null) {
			sb.append(SPACE).append(BLACKET_LEFT).append(BIOTECH).append(SPACE)
					.append(protein.getBiotechName().getValue()).append(BLACKET_RIGHT);
		}
		if ((protein.getCdAntigenName() != null) && !protein.getCdAntigenName().isEmpty()) {
			sb.append(SPACE);
			sb.append(protein.getCdAntigenName().stream()
					.map(val -> BLACKET_LEFT + CD_ANTIGEN + SPACE + val.getValue() + BLACKET_RIGHT)
					.collect(Collectors.joining(SPACE)));
		}
		if ((protein.getInnName() != null) && !protein.getInnName().isEmpty()) {
			sb.append(SPACE);
			sb.append(protein.getInnName().stream().map(val -> BLACKET_LEFT + val.getValue() + BLACKET_RIGHT)
					.collect(Collectors.joining(SPACE)));
		}
		if ((protein.getComponent() != null) && !protein.getComponent().isEmpty()) {
			sb.append(SPACE).append(SQUARE_BLACKET_LEFT).append(CLEAVED_INTO).append(SPACE)
					.append(protein.getComponent().stream().map(val -> getDownloadStringFromProteinName(val))
							.collect(Collectors.joining(SEMICOLON)))
					.append(SPACE).append(SQUARE_BLACKET_RIGHT);
		}

		if ((protein.getDomain() != null) && !protein.getDomain().isEmpty()) {
			sb.append(SPACE).append(SQUARE_BLACKET_LEFT).append(INCLUDES).append(SPACE).append(protein.getDomain()
					.stream().map(val -> getDownloadStringFromProteinName(val)).collect(Collectors.joining(SEMICOLON)))
					.append(SPACE).append(SQUARE_BLACKET_RIGHT);
		}
		Map<String, String> map = new HashMap<>();
		map.put(FIELD, sb.toString());
		return map;
	}

	private String getDownloadStringFromProteinName(ProteinName pname) {
		StringBuilder sb = new StringBuilder();
		sb.append(getDownloadStringFromName(pname.getRecommendedName()));
		if ((pname.getAlternativeName() != null) && !pname.getAlternativeName().isEmpty()){
			if (sb.length() > 0) {
				sb.append(SPACE);
			}
			sb.append(pname.getAlternativeName().stream()
					.map(val -> BLACKET_LEFT + getDownloadStringFromName(val) + BLACKET_RIGHT)
					.collect(Collectors.joining(SPACE)));
		}

		return sb.toString();
	}

	private String getDownloadStringFromName(Name name) {
		StringBuilder sb = new StringBuilder();
		sb.append(name.getFullName().getValue());
		String sname = null;
		if (name.getShortName() != null)

			sname = name.getShortName().stream().map(val -> val.getValue()).collect(Collectors.joining(DELIMITER));
		String ec = null;
		if (name.getEcNumber() != null)
			ec = name.getEcNumber().stream().map(val -> EC2+ SPACE+ val.getValue()).collect(Collectors.joining(DELIMITER));
		if (!Strings.isNullOrEmpty(sname)) {
			sb.append(DELIMITER).append(sname);
		}
		if (!Strings.isNullOrEmpty(ec)) {
			sb.append(DELIMITER).append(ec);
		}
		return sb.toString();
	}

}
