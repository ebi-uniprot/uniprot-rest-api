package uk.ac.ebi.uniprot.uuw.advanced.search.model.download;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import uk.ac.ebi.kraken.interfaces.uniprot.comments.CommentType;
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
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.comment.WRComment;

public class DownloadableComment implements Downloadable {
	public static final List<String> FIELDS = Arrays.asList(new String[] { "cc:alternative_products",
			"cc:mass_spectrometry", "cc:polymorphism", "cc:rna_editing", "cc:sequence_caution", "cc:catalytic_activity",
			"cc:cofactor", "cc:enzyme_regulation", "cc:function", "cc:pathway", "cc:miscellaneous", "cc:interaction",
			"cc:subunit", "cc:developmental_stage", "cc:induction", "cc:tissue_specificity", "cc:allergen",
			"cc:biotechnology", "cc:disruption_phenotype", "cc:disease", "cc:pharmaceutical", "cc:toxic_dose",
			"cc:subcellular_location", "cc:ptm", "cc:domain", "cc:similarity", "cc:caution", "absorption", "kinetics",
			"ph_dependence", "redox_potential", "temp_dependence", "error_gmodel_pred", "protein_families" });

	private final List<Comment> comments;

	public DownloadableComment(List<Comment> comments) {
		if (comments == null) {
			this.comments = Collections.emptyList();
		} else
			this.comments = Collections.unmodifiableList(comments);
	}

	@Override
	public Map<String, String> getData() {
		if (comments.isEmpty())
			return Collections.emptyMap();
		
		Map<String, String> map = new HashMap<>();
		for (CommentType type : CommentType.values()) {
			switch (type) {
			case ALTERNATIVE_PRODUCTS:
				List<APComment> apComments = getComments(type);

				break;
			case BIOPHYSICOCHEMICAL_PROPERTIES:
				List<BioPhyChemPropComment> bpcpComments = getComments(type);
				break;
			case COFACTOR:
				List<CofactorComment> cfComments = getComments(type);
				break;
			case DISEASE:
				List<DiseaseComment> dsComments = getComments(type);
				break;
			case INTERACTION:
				List<IntActComment> iaComments = getComments(type);
				break;
			case MASS_SPECTROMETRY:
				List<MassSpecComment> msComments = getComments(type);
				break;
			case RNA_EDITING:
				List<RnaEdComment> reComments = getComments(type);
				break;
			case SEQUENCE_CAUTION:
				List<SeqCautionComment> scComments = getComments(type);
				break;
			case SUBCELLULAR_LOCATION:
				List<SubcellLocationComment> sclComments = getComments(type);
				break;
			case WEBRESOURCE:
				List<WRComment> wrComments = getComments(type);
				break;
			default:
				List<TextComment> txtComments = getComments(type);	
				updateTextComments(map, type, txtComments);
			}

		}
		return null;
	}

	private void updateTextComments(Map<String, String> map, CommentType type, List<TextComment> txtComments) {
		if((txtComments ==null) || txtComments.isEmpty())
			return;
		String value =txtComments.stream()
		.map(this::getTextComment)
		.collect(Collectors.joining("; "));
		String field = "cc:" + type.name().toLowerCase();
		map.put(field, value);
	}
	private String getTextComment(TextComment text) {
		StringBuilder sb = new StringBuilder();
		sb.append(text.getType().toDisplayName()).append(": ");
		sb.append(
		text.getText().stream()
		.map(val -> DownloadableUtil.convertEvidencedString(val) +".")
		.collect(Collectors.joining("; ")));
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	public <T extends Comment> List<T> getComments(CommentType type) {
		return comments.stream().filter(comment -> (comment.getType() == type)).map(val -> (T) val)
				.collect(Collectors.toList());
	}
}
