package org.uniprot.api.uniprotkb.model.converter;

import org.uniprot.api.uniprotkb.model.DiseaseComment;
import org.uniprot.api.uniprotkb.model.EvidencedString;
import org.uniprot.api.uniprotkb.model.SubcellularLocationComment;
import org.uniprot.core.uniprotkb.evidence.Evidence;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created 06/05/2020
 *
 * @author Edd
 */
public class CommentConverter {
    public static DiseaseComment convertDiseaseComment(
            org.uniprot.core.uniprotkb.comment.DiseaseComment diseaseComment) {
        /*
            return new DiseaseComment(
        upComment.getMolecule(),
        upComment.getDisease().getDiseaseId().getValue(),

        upComment.getDisease().getAcronym().getValue(),

        upComment.getDisease().getReference().getDiseaseReferenceId().getValue().isEmpty() == true ? null
        		: new DbReference(upComment.getDisease().getReference().getDiseaseReferenceType().name(),
        				upComment.getDisease().getReference().getDiseaseReferenceId().getValue(),
        				Collections.emptyMap()),

        upComment.getDisease().getDescription().getValue().isEmpty() == true ? null
        		: ConverterHelper.convert(upComment.getDisease().getDescription()),

        convert(upComment.getNote().getTexts()));
             */

        DiseaseComment.builder().diseaseId(diseaseComment.getDisease().getDiseaseId())
        .acronym(diseaseComment.getDisease().getAcronym())
        .description(convertEvidence(diseaseComment.getDisease().getDescription(), diseaseComment.getDisease().getEvidences());
        diseaseComment.getDisease().getEvidences()
        return null;
    }


    public static SubcellularLocationComment convertSubcellComment(
            org.uniprot.core.uniprotkb.comment.SubcellularLocationComment
                    subcellularLocationComment) {
        return null;
    }
}
