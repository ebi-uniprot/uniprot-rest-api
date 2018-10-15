package uk.ac.ebi.uniprot.uuw.advanced.search.model.download;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

import uk.ac.ebi.uniprot.dataservice.restful.features.domain.Feature;

public class DownloadableFeatures implements Downloadable {
	public static final List<String> FIELDS = Arrays.asList("ft:var_seq", "ft:variant", "ft:non_con", "ft:non_std",
			"ft:non_ter", "ft:conflict", "ft:unsure", "ft:act_site", "ft:binding", "ft:ca_bind", "ft:dna_bind",
			"ft:metal", "ft:np_bind", "ft:site", "ft:mutagen", "ft:intramem", "ft:top_dom", "ft:transmem", "ft:chain",
			"ft:crosslnk", "ft:disulfide", "ft:carbohyd", "ft:init_met", "ft:lipid", "ft:mod_res", "ft:peptide",
			"ft:propep", "ft:signal", "ft:transit", "ft:strand", "ft:helix", "ft:turn", "ft:coiled", "ft:compbias",
			"ft:domain", "ft:motif", "ft:region", "ft:repeat", "ft:zn_fing", "dr:dbsnp"

	);

	private static final List<String> FEATURE_HAS_ALTERNATIVE_SEQ = Arrays.asList("VARIANT", "VAR_SEQ", "MUTAGEN",
			"CONFLICT");
	private static final List<String> FEATURE_TYPE_NEED_BRACKET = Arrays.asList("VARIANT", "VAR_SEQ", "CONFLICT");

	private static final Pattern DBSNP_PATTERN = Pattern.compile("(.+)dbSNP(\\:)(rs(\\d+))(.*)");
	private static final Map<String, String> FEATURETYPE_2_NAME = new HashMap<>();
	static {
		FEATURETYPE_2_NAME.put("INIT_MET", "Initiator methionine");
		FEATURETYPE_2_NAME.put("SIGNAL", "Signal peptide");
		FEATURETYPE_2_NAME.put("PROPEP", "Propeptide");
		FEATURETYPE_2_NAME.put("TRANSIT", "Transit peptide");
		FEATURETYPE_2_NAME.put("CHAIN", "Chain");
		FEATURETYPE_2_NAME.put("PEPTIDE", "Peptide");
		FEATURETYPE_2_NAME.put("TOPO_DOM", "Topological domain");
		FEATURETYPE_2_NAME.put("TRANSMEM", "Transmembrane");
		FEATURETYPE_2_NAME.put("DOMAIN", "Domain");

		FEATURETYPE_2_NAME.put("REPEAT", "Repeat");
		FEATURETYPE_2_NAME.put("CA_BIND", "Calcium-binding");
		FEATURETYPE_2_NAME.put("ZN_FING", "Zinc finger");
		FEATURETYPE_2_NAME.put("DNA_BIND", "DNA-binding");
		FEATURETYPE_2_NAME.put("NP_BIND", "Nucleotide-binding");
		FEATURETYPE_2_NAME.put("REGION", "Region");
		FEATURETYPE_2_NAME.put("COILED", "Coiled coil");
		FEATURETYPE_2_NAME.put("MOTIF", "Motif");

		FEATURETYPE_2_NAME.put("COMPBIAS", "Compositional biased");
		FEATURETYPE_2_NAME.put("ACT_SITE", "Active site");
		FEATURETYPE_2_NAME.put("METAL", "Metal binding");
		FEATURETYPE_2_NAME.put("BINDING", "binding site");
		FEATURETYPE_2_NAME.put("SITE", "Site");
		FEATURETYPE_2_NAME.put("NON_STD", "Non-standard residue");
		FEATURETYPE_2_NAME.put("MOD_RES", "Modified residue");
		FEATURETYPE_2_NAME.put("LIPID", "Lipidation");

		FEATURETYPE_2_NAME.put("CARBOHYD", "Glycosylation");
		FEATURETYPE_2_NAME.put("DISULFID", "Disulfide bond");
		FEATURETYPE_2_NAME.put("CROSSLNK", "Cross-link");
		FEATURETYPE_2_NAME.put("VAR_SEQ", "Alternative sequence");
		FEATURETYPE_2_NAME.put("VARIANT", "Natural variant");
		FEATURETYPE_2_NAME.put("MUTAGEN", "Mutagenesis");
		FEATURETYPE_2_NAME.put("UNSURE", "Sequence uncertainty");

		FEATURETYPE_2_NAME.put("CONFLICT", "Sequence conflict");
		FEATURETYPE_2_NAME.put("NON_CONS", "Non-adjacent residues");
		FEATURETYPE_2_NAME.put("NON_TER", "Non-terminal residue");
		FEATURETYPE_2_NAME.put("HELIX", "Helix");
		FEATURETYPE_2_NAME.put("TURN", "Turn");
		FEATURETYPE_2_NAME.put("STRAND", "Beta strand");
		FEATURETYPE_2_NAME.put("INTRAMEM", "Intramembrane");

	}

	private final List<Feature> features;

	public DownloadableFeatures(List<Feature> features) {
		if (features == null) {
			this.features = Collections.emptyList();
		} else
			this.features = Collections.unmodifiableList(features);
	}

	@Override
	public Map<String, String> map() {
		if (features.isEmpty()) {
			Collections.emptyMap();
		}
		Map<String, String> map = new HashMap<>();
		Map<String, List<Feature>> featureMap = features.stream().collect(Collectors.groupingBy(val -> val.getType()));
		featureMap.entrySet().stream().forEach(val -> {
			String key = "ft:" + val.getKey().toLowerCase();
			String value = val.getValue().stream().map(DownloadableFeatures::featureToString)
					.collect(Collectors.joining("; "));
			map.put(key, value);
			if (val.getKey().toLowerCase().equals("variant")) {
				String dbSnps = variantTodbSnp(val.getValue());
				if (!Strings.isNullOrEmpty(dbSnps)) {
					map.put("dr:dbsnp", dbSnps);
				}
			}
		});

		return map;
	}

	private String variantTodbSnp(List<Feature> features) {
		return features.stream().map(feature -> feature.getDescription()).map(this::getDbsnpFromFeatureDescription)
				.filter(val -> !Strings.isNullOrEmpty(val)).collect(Collectors.joining(" "));

	}

	private String getDbsnpFromFeatureDescription(String description) {
		Matcher matcher = DBSNP_PATTERN.matcher(description);
		if (matcher.matches()) {
			return matcher.group(3);
		} else {
			return null;
		}
	}

	public static List<String> getFeatures(List<Feature> features) {
		if (features == null) {
			return Collections.emptyList();
		}

		Map<String, Long> values = features.stream().map(val -> FEATURETYPE_2_NAME.get(val.getType()))
				.filter(val -> val != null)
				.collect(Collectors.groupingBy(val -> val, TreeMap::new, Collectors.counting()));
		return values.entrySet().stream().map(val -> (val.getKey() + " (" + val.getValue().toString() + ")"))
				.collect(Collectors.toList());

	}

	public static String featureToString(Feature feature) {
		StringBuilder sb = new StringBuilder();
		sb.append(feature.getType()).append(" ").append(feature.getBegin()).append(" ").append(feature.getEnd());
		if (FEATURE_HAS_ALTERNATIVE_SEQ.contains(feature.getType())) {
			sb.append(" ").append(getAlternativeSequence(feature));
		}
		if (!Strings.isNullOrEmpty(feature.getDescription())) {
			if (feature.getType().equals("MUTAGEN"))
				sb.append(":");
			sb.append(" ");
			if (FEATURE_TYPE_NEED_BRACKET.contains(feature.getType())) {
				sb.append("(");
			}
			sb.append(feature.getDescription());
			if (FEATURE_TYPE_NEED_BRACKET.contains(feature.getType())) {
				sb.append(")");
			}

			sb.append(".");
		}
		if ((feature.getEvidences() != null) && !feature.getEvidences().isEmpty()) {
			sb.append(" ").append(feature.getEvidences().stream().map(val -> val.toString())
					.collect(Collectors.joining(", ", "{", "}"))).append(".");
		}
		if (!Strings.isNullOrEmpty(feature.getFtId())) {
			sb.append(" /FTId=").append(feature.getFtId()).append(".");
		}

		return sb.toString();
	}

	private static String getAlternativeSequence(Feature feature) {
		if (FEATURE_HAS_ALTERNATIVE_SEQ.contains(feature.getType())) {
			if (Strings.isNullOrEmpty(feature.getAlternativeSequence()))
				return "Missing";
			else {
				StringBuilder sb = new StringBuilder();
				sb.append(feature.getOrginalSequence());
				if (feature.getType().equals("MUTAGEN")) {
					sb.append("->");
				} else {
					sb.append(" -> ");
				}
				sb.append(feature.getAlternativeSequence());
				return sb.toString();
			}
		} else
			return "";
	}
	public static  boolean contains(List<String> fields) {
		return fields.stream().anyMatch(val -> FIELDS.contains(val));
		
	}
}
