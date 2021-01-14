package org.uniprot.api.support.data.disease.response;

import java.util.Collections;

import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.Xref;
import org.obolibrary.oboformat.parser.OBOFormatConstants;
import org.uniprot.api.rest.output.converter.AbstractOBOMessageConverter;
import org.uniprot.core.cv.disease.DiseaseCrossReference;
import org.uniprot.core.cv.disease.DiseaseEntry;
import org.uniprot.core.util.Utils;

public class DiseaseOBOMessageConverter extends AbstractOBOMessageConverter<DiseaseEntry> {
    private static final String DISEASE_NAMESPACE = "uniprot:diseases";

    public DiseaseOBOMessageConverter() {
        super(DiseaseEntry.class);
    }

    @Override
    public Frame getTermFrame(DiseaseEntry diseaseEntry) {
        Frame frame = new Frame(Frame.FrameType.TERM);

        frame.setId(diseaseEntry.getId());
        frame.addClause(getIdClause(diseaseEntry));
        frame.addClause(getNameClause(diseaseEntry));

        if (Utils.notNullNotEmpty(diseaseEntry.getAlternativeNames())) {
            for (String syn : diseaseEntry.getAlternativeNames()) {
                frame.addClause(getSynonymClause(syn));
            }
        }

        frame.addClause(getDefClause(diseaseEntry));

        // add xref
        for (DiseaseCrossReference xref : diseaseEntry.getCrossReferences()) {
            frame.addClause(getXRefClause(xref));
        }

        return frame;
    }

    @Override
    public String getHeaderNamespace() {
        return DISEASE_NAMESPACE;
    }

    public Clause getSynonymClause(String synonym) {
        Clause clause = new Clause(OBOFormatConstants.OboFormatTag.TAG_SYNONYM, synonym);
        Xref xref = new Xref("UniProt");
        clause.setXrefs(Collections.singletonList(xref));
        return clause;
    }

    public Clause getDefClause(DiseaseEntry diseaseEntry) {
        Clause clause =
                new Clause(OBOFormatConstants.OboFormatTag.TAG_DEF, diseaseEntry.getDefinition());
        return clause;
    }

    public Clause getIdClause(DiseaseEntry disease) {
        Clause clause = new Clause(OBOFormatConstants.OboFormatTag.TAG_ID, disease.getId());
        return clause;
    }

    public Clause getNameClause(DiseaseEntry disease) {
        Clause clause = new Clause(OBOFormatConstants.OboFormatTag.TAG_NAME, disease.getName());
        return clause;
    }

    public Clause getXRefClause(DiseaseCrossReference crossRef) {
        Clause clause = new Clause(OBOFormatConstants.OboFormatTag.TAG_XREF);
        Xref xref = new Xref(crossRef.getDatabaseType() + ":" + crossRef.getId());
        if (Utils.notNullNotEmpty(crossRef.getProperties())) {
            xref.setAnnotation(crossRef.getProperties().get(0));
        }
        clause.setValues(Collections.singletonList(xref));
        return clause;
    }
}
