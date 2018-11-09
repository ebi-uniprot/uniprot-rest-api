package uk.ac.ebi.uniprot.uniprotkb.output.model;

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

import uk.ac.ebi.kraken.interfaces.uniprot.comments.CommentType;
import uk.ac.ebi.kraken.interfaces.uniprot.comments.SequenceCautionType;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.comment.APComment;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.comment.BioPhyChemPropComment;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.comment.CofactorComment;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.comment.Comment;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.comment.DiseaseComment;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.comment.IntActComment;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.comment.MassSpecComment;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.comment.RnaEdComment;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.comment.SeqCautionComment;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.comment.SubcellLocationComment;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.comment.TextComment;
import uk.ac.ebi.uniprot.rest.output.model.NamedValueMap;

public class EntryCommentsMap implements NamedValueMap {
	public static final List<String> FIELDS = Arrays.asList("cc:alternative_products", "cc:mass_spectrometry",
			"cc:polymorphism", "cc:rna_editing", "cc:sequence_caution", "cc:catalytic_activity", "cc:cofactor",
			"cc:enzyme_regulation", "cc:function", "cc:pathway", "cc:miscellaneous", "cc:interaction", "cc:subunit",
			"cc:developmental_stage", "cc:induction", "cc:tissue_specificity", "cc:allergen", "cc:biotechnology",
			"cc:disruption_phenotype", "cc:disease", "cc:pharmaceutical", "cc:toxic_dose", "cc:subcellular_location",
			"cc:ptm", "cc:domain", "cc:similarity", "cc:caution", "absorption", "kinetics", "ph_dependence",
			"redox_potential", "temp_dependence", "error_gmodel_pred", "protein_families");

	private final List<Comment> comments;
	private static final Pattern PATTERN_FAMILY =Pattern.compile("(?:In the .+? section; )?[Bb]elongs to the (.+?family)\\.(?: (.+?family)\\.)?(?: (.+?family)\\.)?(?: Highly divergent\\.)?");

	public EntryCommentsMap(List<Comment> comments) {
		if (comments == null) {
			this.comments = Collections.emptyList();
		} else
			this.comments = Collections.unmodifiableList(comments);
	}

	@Override
	public Map<String, String> attributeValues() {
		if (comments.isEmpty())
			return Collections.emptyMap();

		Map<String, String> map = new HashMap<>();
		for (CommentType type : CommentType.values()) {
			switch (type) {
			case ALTERNATIVE_PRODUCTS:
				List<APComment> apComments = getComments(type);
				updateAPComments(map, type, apComments);
				break;
			case BIOPHYSICOCHEMICAL_PROPERTIES:
				List<BioPhyChemPropComment> bpcpComments = getComments(type);
				updateBioPhyChemPropComments(map, type, bpcpComments);
				break;
			case COFACTOR:
				List<CofactorComment> cfComments = getComments(type);
				updateCofactorComments(map, type, cfComments);
				break;
			case DISEASE:
				List<DiseaseComment> dsComments = getComments(type);
				updateDiseaseComments(map, type, dsComments);
				break;
			case INTERACTION:
				List<IntActComment> iaComments = getComments(type);
				updateInterActComments(map, type, iaComments);
				break;
			case MASS_SPECTROMETRY:
				List<MassSpecComment> msComments = getComments(type);
				updateMassSpecComments(map, type, msComments);
				break;
			case RNA_EDITING:
				List<RnaEdComment> reComments = getComments(type);
				updateRnaEdComments(map, type, reComments);
				break;
			case SEQUENCE_CAUTION:
				List<SeqCautionComment> scComments = getComments(type);
				updateSeqCautionComments(map, type, scComments);
				break;
			case SUBCELLULAR_LOCATION:
				List<SubcellLocationComment> sclComments = getComments(type);
				updateSubCellLocComments(map, type, sclComments);
				break;
			case WEBRESOURCE:
				// List<WRComment> wrComments = getComments(type);
				break;
			case SIMILARITY:
				List<TextComment> simiComments = getComments(type);
				updateTextComments(map, type, simiComments);
				updateProteinFamility(map, type, simiComments);
				
			default:
				List<TextComment> txtComments = getComments(type);
				updateTextComments(map, type, txtComments);
				
			}

		}
		return map;
	}

	private void updateAPComments(Map<String, String> map, CommentType type, List<APComment> apComments) {
		if ((apComments == null) || apComments.isEmpty())
			return;
		String result = apComments.stream().map(APComment::toString).collect(Collectors.joining("; "));
		map.put("cc:alternative_products", result);
	}

	private void updateBioPhyChemPropComments(Map<String, String> map, CommentType type,
			List<BioPhyChemPropComment> bpcpComments) {
		if ((bpcpComments == null) || bpcpComments.isEmpty())
			return;
		String absorption = absorptionToString(bpcpComments);
		if (!Strings.isNullOrEmpty(absorption)) {
			map.put("absorption", "BIOPHYSICOCHEMICAL PROPERTIES: ;  " + absorption);
		}
		String kinetics = kineticsToString(bpcpComments);
		if (!Strings.isNullOrEmpty(kinetics)) {
			map.put("kinetics", "BIOPHYSICOCHEMICAL PROPERTIES:  " + kinetics);
		}
		String phDependence = phDependenceToString(bpcpComments);
		if (!Strings.isNullOrEmpty(phDependence)) {
			map.put("ph_dependence", "BIOPHYSICOCHEMICAL PROPERTIES: ;  pH dependence: " + phDependence);
		}
		String redoxPotential = redoxPotentialToString(bpcpComments);
		if (!Strings.isNullOrEmpty(redoxPotential)) {
			map.put("redox_potential", "BIOPHYSICOCHEMICAL PROPERTIES: ;  Redox potential: " + redoxPotential);
		}
		String tempDependence = tempDependenceToString(bpcpComments);
		if (!Strings.isNullOrEmpty(tempDependence)) {
			map.put("temp_dependence", "BIOPHYSICOCHEMICAL PROPERTIES: ;  Temperature dependence: " + tempDependence);
		}
	}

	private String absorptionToString(List<BioPhyChemPropComment> bpcpComments) {
		return bpcpComments.stream().map(val -> val.getAbsorption()).filter(val -> val != null)
				.map(val -> val.toString()).collect(Collectors.joining("; "));

	}

	private String kineticsToString(List<BioPhyChemPropComment> bpcpComments) {
		return bpcpComments.stream().map(val -> val.getKinetics()).filter(val -> val != null).map(val -> val.toString())
				.collect(Collectors.joining("; "));
	}

	private String phDependenceToString(List<BioPhyChemPropComment> bpcpComments) {
		return bpcpComments.stream().map(val -> val.getPhDependence()).filter(val -> val != null)
				.flatMap(val -> val.stream()).map(val -> val.toString()).collect(Collectors.joining("; "));
	}

	private String redoxPotentialToString(List<BioPhyChemPropComment> bpcpComments) {
		return bpcpComments.stream().map(val -> val.getRedoxPotential()).filter(val -> val != null)
				.flatMap(val -> val.stream()).map(val -> val.toString()).collect(Collectors.joining("; "));
	}

	private String tempDependenceToString(List<BioPhyChemPropComment> bpcpComments) {
		return bpcpComments.stream().map(val -> val.getTemperatureDependence()).filter(val -> val != null)
				.flatMap(val -> val.stream()).map(val -> val.toString()).collect(Collectors.joining("; "));
	}

	private void updateCofactorComments(Map<String, String> map, CommentType type, List<CofactorComment> cfComments) {
		if ((cfComments == null) || cfComments.isEmpty())
			return;
		String result = cfComments.stream().map(CofactorComment::toString).collect(Collectors.joining("; "));
		map.put("cc:cofactor", result);
	}

	private void updateDiseaseComments(Map<String, String> map, CommentType type, List<DiseaseComment> dsComments) {
		if ((dsComments == null) || dsComments.isEmpty())
			return;
		String result = dsComments.stream().map(DiseaseComment::toString).collect(Collectors.joining("; "));
		map.put("cc:disease", result);
	}

	private void updateInterActComments(Map<String, String> map, CommentType type, List<IntActComment> iaComments) {
		if ((iaComments == null) || iaComments.isEmpty())
			return;
		String result = iaComments.stream().flatMap(val -> val.getInteractions().stream()).map(this::getInterAct)
				.collect(Collectors.joining("; "));
		map.put("cc:interaction", result);
	}

	private String getInterAct(IntActComment.IntAct interAct) {
		if ("SELF".equals(interAct.getInteractionType())) {
			return "Self";
		} else
			return interAct.getId();
	}

	private void updateMassSpecComments(Map<String, String> map, CommentType type, List<MassSpecComment> msComments) {
		if ((msComments == null) || msComments.isEmpty())
			return;
		String result = msComments.stream().map(MassSpecComment::toString).collect(Collectors.joining(";  "));
		map.put("cc:mass_spectrometry", result);
	}

	private void updateRnaEdComments(Map<String, String> map, CommentType type, List<RnaEdComment> reComments) {
		if ((reComments == null) || reComments.isEmpty())
			return;
		String result = reComments.stream().map(RnaEdComment::toString).collect(Collectors.joining(";  "));
		map.put("cc:rna_editing", result);
	}

	private void updateSeqCautionComments(Map<String, String> map, CommentType type,
			List<SeqCautionComment> scComments) {
		if ((scComments == null) || scComments.isEmpty())
			return;
		List<String> result= scComments.stream()
				.filter(val -> !SequenceCautionType.ERRONEOUS_PREDICTION.toDisplayName().equals(val.getConflictType()))
				.map(SeqCautionComment::toString)
				.collect(Collectors.toList());
		if(!result.isEmpty()) {
		String result3 = result.stream()
				.collect(Collectors.joining(";  ", "SEQUENCE CAUTION:  ", ""));
		map.put("cc:sequence_caution", result3);
		}
		List<String> result1= scComments.stream()
				.filter(val -> SequenceCautionType.ERRONEOUS_PREDICTION.toDisplayName().equals(val.getConflictType()))
				.map(SeqCautionComment::toString)
				.collect(Collectors.toList());
		if(!result1.isEmpty()) {
				String result2 =result1.stream().collect(Collectors.joining(";  ", "SEQUENCE CAUTION:  ", ""));
			map.put("error_gmodel_pred", result2);
		}
	}

	private void updateSubCellLocComments(Map<String, String> map, CommentType type,
			List<SubcellLocationComment> sclComments) {
		if ((sclComments == null) || sclComments.isEmpty())
			return;

		String result = sclComments.stream().map(SubcellLocationComment::toString).collect(Collectors.joining(";  "));
		map.put("cc:subcellular_location", result);

	}

	private void updateTextComments(Map<String, String> map, CommentType type, List<TextComment> txtComments) {
		if ((txtComments == null) || txtComments.isEmpty())
			return;
		String value = txtComments.stream().map(TextComment::toString).collect(Collectors.joining("; "));
		String field = "cc:" + type.name().toLowerCase();
		map.put(field, value);
	}

	private void updateProteinFamility(Map<String, String> map, CommentType type, List<TextComment> txtComments) {
		if ((txtComments == null) || txtComments.isEmpty() || type != CommentType.SIMILARITY)
			return;
		
		String value = txtComments.stream().flatMap(val->val.getText().stream())
				.map(val -> convertToProteinFamily(val.getValue()))
				.filter(val ->!Strings.isNullOrEmpty(val))
				.collect(Collectors.joining("; "));
		String field = "protein_families";
		map.put(field, value);
	}

	private String convertToProteinFamily(String text) {
		String val = text;
		if(!val.endsWith(".")) {
			val +=".";
		}
		Matcher m = PATTERN_FAMILY.matcher(val);
		if (m.matches()){
			StringBuilder line = new StringBuilder();
			line.append(m.group(1));
			if (m.group(2) != null)
				line.append(", ").append(m.group(2));
			if (m.group(3) != null)
				line.append(", ").append(m.group(3));
			String result = line.toString();
			return result.substring(0, 1).toUpperCase() + result.substring(1);
		}
		return null;
	}
	public static List<String> getSequenceCautionTypes(List<Comment> comments) {
		if (comments == null)
			return Collections.emptyList();
		Map<String, Long> values = comments.stream()
				.filter(comment -> (comment.getType() == CommentType.SEQUENCE_CAUTION))
				.map(val -> (SeqCautionComment) val).map(val -> val.getConflictType()).filter(val -> val != null)
				.collect(Collectors.groupingBy(val -> val, TreeMap::new, Collectors.counting()));
		return values.entrySet().stream().map(val -> (val.getKey() + " (" + val.getValue().toString() + ")"))
				.collect(Collectors.toList());

	}

	@SuppressWarnings("unchecked")
	private <T extends Comment> List<T> getComments(CommentType type) {
		return comments.stream().filter(comment -> (comment.getType() == type)).map(val -> (T) val)
				.collect(Collectors.toList());
	}

	public static boolean contains(List<String> fields) {
		return fields.stream().anyMatch(val -> FIELDS.contains(val));
		
	}
}
