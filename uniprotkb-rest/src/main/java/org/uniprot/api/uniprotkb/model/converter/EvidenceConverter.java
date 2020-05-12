package org.uniprot.api.uniprotkb.model.converter;

import org.uniprot.api.uniprotkb.model.DbReferenceObject;
import org.uniprot.api.uniprotkb.model.Evidence;
import org.uniprot.core.uniprotkb.evidence.EvidenceDatabaseTypes;

/**
 * Created 07/05/2020
 *
 * @author Edd
 */
public class EvidenceConverter {
    private static final EvidenceDatabaseTypes DB_TYPES = EvidenceDatabaseTypes.INSTANCE;

    public static Evidence convertEvidence(
            org.uniprot.core.uniprotkb.evidence.Evidence e, String accession) {

return         Evidence.builder().code(e.getEvidenceCode().getCode()).source(map(e, accession))
        .build();
        EvidenceTypeMapping instance = EvidenceTypeMapping.getInstance();
        String ecoCode = ev.getEvidenceCode().getCodeValue();

        Evidence featureEvidence = new Evidence();
        featureEvidence.setCode(ecoCode);
        featureEvidence.setSource(instance.map(ev, acc));

        return featureEvidence;
    }

    /**
     * Transforms the given database cross-reference name, {@code xrefName} and database
     * cross-reference ID {@code xrefId} into the corresponding {@link DbReferenceObject}.
     *
     * @param xrefName database cross-reference name
     * @param xrefId database cross-reference ID
     * @return the corresponding {@link DbReferenceObject}
     */
    public static DbReferenceObject map(String xrefName, String xrefId) {
        return map(xrefName, xrefId, null);
    }

    /**
     * Transforms the given {@link EvidenceId}, {@code evidenceId}, and UniProt accession, {@code
     * uniProtAcc} into the corresponding {@link DbReferenceObject}.
     *
     * @param evidenceId the {@link EvidenceId}
     * @param uniProtAcc the UniProt accession
     * @return the corresponding {@link DbReferenceObject}
     */
    public static DbReferenceObject map(org.uniprot.core.uniprotkb.evidence.Evidence evidenceId, String uniProtAcc) {
        // if there is no sourceID return null;
        EvidenceAttribute evAttr = evidenceId.getAttribute();
        evidenceId.getEvidenceCode().getSource()
        if (evAttr == null) {
            LOGGER.debug(
                    "EvidenceId has null getAttribute() for uniProtAcc={} -- returning null DbReferenceObject",
                    uniProtAcc);
            return null;
        }

        String evAttrValue = evAttr.getValue();
        if (evAttrValue == null || evAttrValue.isEmpty()) {
            LOGGER.debug(
                    "EvidenceId has null/empty getAttribute().getValue() for uniProtAcc={} -- returning null DbReferenceObject",
                    uniProtAcc);
            return null;
        }

        String typeValue = evidenceId.getTypeValue();
        if (typeValue == null || typeValue.isEmpty()) {
            if (evAttrValue.startsWith(UNIPROT_REF_PREFIX)) {
                typeValue = UNIPROT_REF_PREFIX;
            } else {
                typeValue = evAttrValue;
            }
        }

        return map(typeValue, evAttrValue, uniProtAcc);
    }

    protected static DbReferenceObject map(String xref, String xrefId, String uniprotAcc) {
        String url = null;
        String name = null;

        if (xref == null) return null;

        DbReferenceObject dbReferenceObject = new DbReferenceObject();

        if (evType2URLMap.containsKey(xref)) {
            CanonicalEvUrlTuple canonicalEvUrlTuple = evType2URLMap.get(xref);
            name = canonicalEvUrlTuple.canonicalXrefName;
            url = this.map2Url(canonicalEvUrlTuple, xrefId, uniprotAcc);
        } else {
            LOGGER.warn(
                    "Could not create DbReferenceObject.url for xref={}, xrefId={}, uniprotAcc={}.",
                    new Object[] {xref, xrefId, uniprotAcc});
        }

        dbReferenceObject.setId(xrefId);
        dbReferenceObject.setUrl(url);
        dbReferenceObject.setName(name);

        if (xref.equalsIgnoreCase("PubMed")) {
            // deal with pubmed's specific case of alternative sequence.
            // Issue: TRM-12863
            String altUrl = String.format(PUBMED_ALT_URL, xrefId);
            dbReferenceObject.setAlternativeUrl(altUrl);
        }

        return dbReferenceObject;
    }

    /**
     * Gets URL string associated with an XrefName (in {@link CanonicalEvUrlTuple}), xrefId and
     * UniProt accession.
     *
     * <p>This method orchestrates calls to getUrlForType. Special cases are handled first, before a
     * general use of the former method.
     *
     * @param evUrlTuple
     * @param xrefId
     * @param uniprotAcc
     * @return
     */
    private static  String map2Url(CanonicalEvUrlTuple evUrlTuple, String xrefId, String uniprotAcc) {
        if (xrefId != null) {
            if (xrefId.startsWith(UNIPROT_REF_PREFIX)) {
                return getUrlForType(evUrlTuple, uniprotAcc, xrefId.substring(4));
            } else if (evUrlTuple.canonicalXrefName.equals(COSMIC_STUDY_SOURCE)
                    && xrefId.indexOf(":") > 0) {
                return getUrlForType(evUrlTuple, xrefId.substring(xrefId.indexOf(":") + 1));
            } else if (evUrlTuple.canonicalXrefName.equals(COSMIC_SOURCE)) {
                return getUrlForType(evUrlTuple, xrefId.substring(4));
            } else if (evUrlTuple.canonicalXrefName.equals(COSMIC_CURATED_SOURCE)) {
                return getUrlForType(evUrlTuple, xrefId.substring(4));
            } else {
                return getUrlForType(evUrlTuple, xrefId);
            }
        }

        LOGGER.warn("Could not map null xrefId to URL string for uniprotAcc={}", uniprotAcc);
        return null;
    }

    private static String getUrlForType(CanonicalEvUrlTuple evUrlTuple, String... ids) {
        try {
            return String.format(evUrlTuple.url, (Object[]) ids);
        } catch (Exception e) {
            LOGGER.error(
                    "Error computing URL: xrefName={}, xrefUrlFormat={}, ids={}",
                    new Object[] {evUrlTuple.canonicalXrefName, evUrlTuple.url, ids});
            LOGGER.error("-->", e);
        }
        return null;
    }
}
