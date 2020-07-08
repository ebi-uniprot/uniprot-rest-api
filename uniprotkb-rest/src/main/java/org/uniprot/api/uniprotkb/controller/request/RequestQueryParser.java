package org.uniprot.api.uniprotkb.controller.request;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.uniprot.core.cv.xdb.UniProtDatabaseDetail;
import org.uniprot.core.uniprotkb.ProteinExistence;
import org.uniprot.core.uniprotkb.comment.CommentType;
import org.uniprot.core.uniprotkb.feature.UniprotKBFeatureType;
import org.uniprot.cv.xdb.UniProtCrossReferenceDisplayOrder;

/**
 * This class is responsible to parse FROM old uniprot lucene query string format TO new uniprot
 * solr query string format format
 *
 * @author lgonzales
 */
public class RequestQueryParser {

    private static final Map<String, String> commentMappingType = new HashMap<>();

    private static final Map<String, String> featureMappingType = new HashMap<>();

    static {
        initFeatureMappingType();
        initCommentMappingType();
    }

    public static String parse(String queryString) {
        try {
            QueryParser queryParser = new QueryParser("", new StandardAnalyzer());
            Query inputQuery = queryParser.parse(queryString);
            Query parsedQuery =
                    parse(
                            inputQuery,
                            queryString.contains("location"),
                            queryString.contains("cofactor"));
            return parsedQuery.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing requested Query ", e);
        }
    }

    private static Query parse(
            Query inputQuery, boolean hasLocationFilter, boolean hasCofactorFilter) {
        Query parsedQuery;
        if (inputQuery instanceof TermQuery) {
            parsedQuery =
                    parseTermQuery((TermQuery) inputQuery, hasLocationFilter, hasCofactorFilter);
        } else if (inputQuery instanceof PhraseQuery) {
            parsedQuery =
                    parsePhraseQuery(
                            (PhraseQuery) inputQuery, hasLocationFilter, hasCofactorFilter);
        } else if (inputQuery instanceof TermRangeQuery) {
            parsedQuery = parseTermRangeQuery((TermRangeQuery) inputQuery);
        } else if (inputQuery instanceof BooleanQuery) {
            parsedQuery =
                    parseBooleanQuery(
                            (BooleanQuery) inputQuery, hasLocationFilter, hasCofactorFilter);
        } else {
            throw new IllegalArgumentException("Unsuported query type: " + inputQuery.getClass());
        }
        return parsedQuery;
    }

    private static Query parseTermQuery(
            TermQuery termQuery, boolean hasLocationFilter, boolean hasCofactorFilter) {
        String fieldName = termQuery.getTerm().field();
        String value = termQuery.getTerm().text();
        Query parsedQuery;
        if (isCommentOrFeatureType(fieldName, value)) { // for features and comment
            String prefix = getTypePrefix(termQuery.getTerm().text());
            String type = getFeatureOrCommentType(termQuery.getTerm().text());
            Term parsedTerm = new Term(prefix + "_" + type, "*");
            parsedQuery = new TermQuery(parsedTerm);
        } else if (isCitation(fieldName)) {
            String citationFieldName = getCitationFieldName(fieldName);
            Term parsedTerm = new Term(citationFieldName, termQuery.getTerm().bytes());
            parsedQuery = new TermQuery(parsedTerm);
        } else if (isCofactorTermQuery(fieldName)) {
            Term parsedTerm = new Term("cc_cofactor_chebi", termQuery.getTerm().bytes());
            parsedQuery = new TermQuery(parsedTerm);
        } else if (isLocationTermQuery(fieldName)) {
            Term parsedTerm = new Term("cc_scl_term_location", termQuery.getTerm().bytes());
            parsedQuery = new TermQuery(parsedTerm);
        } else if (isNoteTermQuery(fieldName)) {
            String noteFieldName = getNoteFieldName(hasLocationFilter, hasCofactorFilter);
            Term parsedTerm = new Term(noteFieldName, termQuery.getTerm().bytes());
            parsedQuery = new TermQuery(parsedTerm);
        } else if (isTaxonomyRelatedQuery(fieldName)) {
            parsedQuery = parseTaxonomyRelatedQuery(false, fieldName, value);
        } else if (isDatabaseCrossReference(termQuery)) {
            Term parsedTerm = new Term("database", termQuery.getTerm().text());
            parsedQuery = new TermQuery(parsedTerm);
        } else if (termQuery.getTerm().field().equalsIgnoreCase("existence")) {
            Term parsedTerm =
                    new Term(
                            "existence",
                            ProteinExistence.typeOf(termQuery.getTerm().text())
                                    .name()
                                    .toLowerCase());
            parsedQuery = new TermQuery(parsedTerm);
        } else {
            Term parsedTerm = new Term(termQuery.getTerm().field(), termQuery.getTerm().bytes());
            parsedQuery = new TermQuery(parsedTerm);
        }
        return parsedQuery;
    }

    private static Query parsePhraseQuery(
            PhraseQuery phraseQuery, boolean hasLocationFilter, boolean hasCofactorFilter) {
        Query parsedQuery;

        String fieldName = phraseQuery.getTerms()[0].field();
        String value =
                Arrays.stream(phraseQuery.getTerms())
                        .map(Term::text)
                        .collect(Collectors.joining(" "));

        if (isCommentOrFeatureType(fieldName, value)) { // for features and comment
            String prefix = getTypePrefix(value);
            String type = getFeatureOrCommentType(value);
            Term parsedTerm = new Term(prefix + "_" + type, "*");
            parsedQuery = new TermQuery(parsedTerm);
        } else if (isTaxonomyRelatedQuery(fieldName)) {
            String[] values =
                    Arrays.stream(phraseQuery.getTerms()).map(Term::text).toArray(String[]::new);
            parsedQuery = parseTaxonomyRelatedQuery(true, fieldName, values);
        } else if (fieldName.equalsIgnoreCase("existence")) {
            Optional<ProteinExistence> proteinExistence =
                    Arrays.stream(ProteinExistence.values())
                            .filter(
                                    pExistence -> {
                                        String pExistenceName = pExistence.name();
                                        if (pExistenceName.indexOf("_LEVEL") > 0) {
                                            pExistenceName =
                                                    pExistenceName.substring(
                                                            0, pExistenceName.indexOf("_LEVEL"));
                                        }
                                        return value.toLowerCase()
                                                .contains(pExistenceName.toLowerCase());
                                    })
                            .findFirst();
            String existence = "";
            if (proteinExistence.isPresent()) {
                existence = proteinExistence.get().name().toLowerCase();
            }
            Term parsedTerm = new Term("existence", existence);
            parsedQuery = new TermQuery(parsedTerm);
        } else {
            String parsedFieldName = fieldName;
            if (isCitation(fieldName)) {
                parsedFieldName = getCitationFieldName(fieldName);
            } else if (isCofactorTermQuery(fieldName)) {
                parsedFieldName = "cc_cofactor_chebi";
            } else if (isLocationTermQuery(fieldName)) {
                parsedFieldName = "cc_scl_term_location";
            } else if (isNoteTermQuery(fieldName)) {
                parsedFieldName = getNoteFieldName(hasLocationFilter, hasCofactorFilter);
            }
            PhraseQuery.Builder builder = new PhraseQuery.Builder();
            builder.setSlop(phraseQuery.getSlop());
            for (Term term : phraseQuery.getTerms()) {
                builder.add(new Term(parsedFieldName, term.text()));
            }
            parsedQuery = builder.build();
        }
        return parsedQuery;
    }

    private static Query parseTermRangeQuery(TermRangeQuery termRangeQuery) {
        return new TermRangeQuery(
                termRangeQuery.getField(),
                termRangeQuery.getLowerTerm(),
                termRangeQuery.getUpperTerm(),
                termRangeQuery.includesLower(),
                termRangeQuery.includesUpper());
    }

    private static Query parseBooleanQuery(
            BooleanQuery booleanQuery, boolean hasLocationFilter, boolean hasCofactorFilter) {
        if (isAnnotation(booleanQuery)) {
            return parseAnnotationQuery(booleanQuery);
        } else if (isCofactor(booleanQuery)) {
            return parseCofactorBooleanQuery(booleanQuery);
        } else if (isLocation(booleanQuery)) {
            return parseLocationBooleanQuery(booleanQuery);
        } else if (isDatabase(booleanQuery)) {
            return parseDatabaseBooleanQuery(booleanQuery);
        } else if (isNote(booleanQuery)) {
            return parseNoteBooleanQuery(booleanQuery, hasLocationFilter, hasCofactorFilter);
        } else {
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            for (BooleanClause clause : booleanQuery.clauses()) {
                Query parsedClauseQuery =
                        parse(clause.getQuery(), hasLocationFilter, hasCofactorFilter);
                builder.add(parsedClauseQuery, clause.getOccur());
            }
            return builder.build();
        }
    }

    private static Query parseAnnotationQuery(BooleanQuery booleanQuery) {
        String type = "";
        String evidence = "";
        String annotation = "*";
        TermRangeQuery length = null;
        String[] phraseAnnotationns = null;
        for (BooleanClause clause : booleanQuery.clauses()) {
            if (clause.getQuery() instanceof TermQuery) {
                TermQuery query = (TermQuery) clause.getQuery();
                if (query.getTerm().field().equalsIgnoreCase("type")) {
                    type = getFeatureOrCommentType(query.getTerm().text());
                }
                if (query.getTerm().field().equalsIgnoreCase("annotation")) {
                    annotation = query.getTerm().text();
                }
                if (query.getTerm().field().equalsIgnoreCase("evidence")) {
                    evidence = query.getTerm().text();
                }
            } else if (clause.getQuery() instanceof TermRangeQuery) {
                TermRangeQuery query = (TermRangeQuery) clause.getQuery();
                if (query.getField().equalsIgnoreCase("length")) {
                    length = query;
                }
            } else if (clause.getQuery() instanceof PhraseQuery) {
                PhraseQuery phraseQuery = (PhraseQuery) clause.getQuery();
                String phraseField = phraseQuery.getTerms()[0].field();
                String phraseValue =
                        Arrays.stream(phraseQuery.getTerms())
                                .map(Term::text)
                                .collect(Collectors.joining(" "));
                if (phraseField.equalsIgnoreCase("type")) {
                    type = getFeatureOrCommentType(phraseValue);
                }
                if (phraseField.equalsIgnoreCase("annotation")) {
                    phraseAnnotationns =
                            Arrays.stream(phraseQuery.getTerms())
                                    .map(Term::text)
                                    .toArray(String[]::new);
                }
            }
        }
        String prefix = getTypePrefix(type);

        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        if (phraseAnnotationns != null) {
            PhraseQuery typeQuery = new PhraseQuery(prefix + "_" + type, phraseAnnotationns);
            builder.add(typeQuery, BooleanClause.Occur.MUST);
        } else {
            TermQuery typeQuery = new TermQuery(new Term(prefix + "_" + type, annotation));
            builder.add(typeQuery, BooleanClause.Occur.MUST);
        }
        if (!evidence.isEmpty()) {
            TermQuery evidenceQuery = new TermQuery(new Term(prefix + "ev_" + type, evidence));
            builder.add(evidenceQuery, BooleanClause.Occur.MUST);
        }
        if (length != null) {
            String fieldName = prefix + "len_" + type;
            TermRangeQuery lengthQuery =
                    new TermRangeQuery(
                            fieldName,
                            length.getLowerTerm(),
                            length.getUpperTerm(),
                            length.includesLower(),
                            length.includesUpper());
            builder.add(lengthQuery, BooleanClause.Occur.MUST);
        }
        return builder.build();
    }

    private static Query parseTaxonomyRelatedQuery(
            boolean isPhraseQuery, String term, String... values) {
        String value = Arrays.stream(values).collect(Collectors.joining(" "));
        if (value.matches("^(\\d+)$")) {
            term += "_id";
            return new TermQuery(new Term(term, value));
        } else if (value.matches("^.* (\\d+)(\\s)*$")) {
            term += "_id";
            Matcher m = Pattern.compile("^.* (\\d+)(\\s)*$").matcher(value);
            if (m.matches()) {
                value = m.group(1);
            }
            return new TermQuery(new Term(term, value));
        } else {
            term += "_name";
            if (isPhraseQuery) {
                return new PhraseQuery(term, values);
            } else {
                return new TermQuery(new Term(term, value));
            }
        }
    }

    private static Query parseDatabaseBooleanQuery(BooleanQuery booleanQuery) {
        String type = "";
        String id = "";
        for (BooleanClause clause : booleanQuery.clauses()) {
            if (clause.getQuery() instanceof TermQuery) {
                TermQuery query = (TermQuery) clause.getQuery();
                if (query.getTerm().field().equalsIgnoreCase("type")) {
                    type = query.getTerm().text();
                } else if (query.getTerm().field().equalsIgnoreCase("id")) {
                    id = query.getTerm().text();
                }
            }
        }
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        TermQuery typeQuery =
                new TermQuery(new Term("xref", type.toLowerCase() + "-" + id.toLowerCase()));
        builder.add(typeQuery, BooleanClause.Occur.MUST);
        return builder.build();
    }

    private static String getCitationFieldName(String fieldName) {
        if (fieldName.equalsIgnoreCase("citation")) {
            fieldName = "lit_title";
        } else if (fieldName.equalsIgnoreCase("published")) {
            fieldName = "lit_pubdate";
        } else { // for author, title or journal
            fieldName = "lit_" + fieldName;
        }
        return fieldName;
    }

    private static Query parseLocationBooleanQuery(BooleanQuery booleanQuery) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for (BooleanClause clause : booleanQuery.clauses()) {
            if (clause.getQuery() instanceof TermQuery) {
                TermQuery query = (TermQuery) clause.getQuery();
                if (query.getTerm().field().equalsIgnoreCase("evidence")) {
                    String evidence = query.getTerm().text();
                    TermQuery evidenceQuery =
                            new TermQuery(new Term("ccev_scl_term_location", evidence));
                    builder.add(evidenceQuery, BooleanClause.Occur.MUST);
                } else if (query.getTerm().field().equalsIgnoreCase("location")) {
                    String location = query.getTerm().text();
                    TermQuery typeQuery = new TermQuery(new Term("cc_scl_term_location", location));
                    builder.add(typeQuery, BooleanClause.Occur.MUST);
                }
            } else if (clause.getQuery() instanceof PhraseQuery) {
                PhraseQuery query = (PhraseQuery) clause.getQuery();
                String fieldName = query.getTerms()[0].field();
                if (fieldName.equalsIgnoreCase("location")) {
                    String[] values =
                            Arrays.stream(query.getTerms()).map(Term::text).toArray(String[]::new);
                    builder.add(
                            new PhraseQuery("cc_scl_term_location", values),
                            BooleanClause.Occur.MUST);
                }
            }
        }

        return builder.build();
    }

    private static Query parseCofactorBooleanQuery(BooleanQuery booleanQuery) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for (BooleanClause clause : booleanQuery.clauses()) {
            if (clause.getQuery() instanceof TermQuery) {
                TermQuery query = (TermQuery) clause.getQuery();
                if (query.getTerm().field().equalsIgnoreCase("evidence")) {
                    String evidence = query.getTerm().text();
                    TermQuery evidenceQuery =
                            new TermQuery(new Term("ccev_cofactor_chebi", evidence));
                    builder.add(evidenceQuery, BooleanClause.Occur.MUST);
                } else if (query.getTerm().field().equalsIgnoreCase("chebi")) {
                    String cofactor = query.getTerm().text();
                    TermQuery typeQuery = new TermQuery(new Term("cc_cofactor_chebi", cofactor));
                }
            } else if (clause.getQuery() instanceof PhraseQuery) {
                PhraseQuery query = (PhraseQuery) clause.getQuery();
                String fieldName = query.getTerms()[0].field();
                if (fieldName.equalsIgnoreCase("chebi")) {
                    String[] values =
                            Arrays.stream(query.getTerms()).map(Term::text).toArray(String[]::new);
                    builder.add(
                            new PhraseQuery("cc_cofactor_chebi", values), BooleanClause.Occur.MUST);
                }
            }
        }
        return builder.build();
    }

    private static Query parseNoteBooleanQuery(
            BooleanQuery booleanQuery, boolean hasLocationFilter, boolean hasCofactorFilter) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for (BooleanClause clause : booleanQuery.clauses()) {
            if (clause.getQuery() instanceof TermQuery) {
                TermQuery query = (TermQuery) clause.getQuery();
                if (query.getTerm().field().equalsIgnoreCase("evidence")) {
                    String evidence = query.getTerm().text();
                    Term evidenceTerm =
                            parseNoteEvidenceTerm(evidence, hasLocationFilter, hasCofactorFilter);
                    builder.add(new TermQuery(evidenceTerm), BooleanClause.Occur.MUST);
                } else if (query.getTerm().field().equalsIgnoreCase("note")) {
                    String noteFieldName = getNoteFieldName(hasLocationFilter, hasCofactorFilter);
                    builder.add(
                            new TermQuery(new Term(noteFieldName, query.getTerm().text())),
                            BooleanClause.Occur.MUST);
                }
            } else if (clause.getQuery() instanceof PhraseQuery) {
                PhraseQuery query = (PhraseQuery) clause.getQuery();
                String fieldName = query.getTerms()[0].field();
                if (fieldName.equalsIgnoreCase("note")) {
                    String noteFieldName = getNoteFieldName(hasLocationFilter, hasCofactorFilter);
                    String[] values =
                            Arrays.stream(query.getTerms()).map(Term::text).toArray(String[]::new);
                    builder.add(new PhraseQuery(noteFieldName, values), BooleanClause.Occur.MUST);
                }
            }
        }
        return builder.build();
    }

    private static String getNoteFieldName(boolean hasLocationFilter, boolean hasCofactorFilter) {
        String fieldName;
        if (hasCofactorFilter) {
            fieldName = "cc_cofactor_note";
        } else if (hasLocationFilter) {
            fieldName = "cc_scl_note";
        } else {
            fieldName = "note";
        }
        return fieldName;
    }

    private static Term parseNoteEvidenceTerm(
            String value, boolean hasLocationFilter, boolean hasCofactorFilter) {
        Term term;
        if (hasCofactorFilter) {
            term = new Term("ccev_cofactor_note", value);
        } else if (hasLocationFilter) {
            term = new Term("ccev_scl_note", value);
        } else {
            term = new Term("note", value);
        }
        return term;
    }

    private static boolean isDatabase(BooleanQuery booleanQuery) {
        for (BooleanClause clause : booleanQuery.clauses()) {
            if (clause.getQuery() instanceof TermQuery) {
                TermQuery query = (TermQuery) clause.getQuery();
                if (isDatabaseCrossReference(query)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isDatabaseCrossReference(TermQuery termQuery) {
        boolean isFieldNameType = termQuery.getTerm().field().equalsIgnoreCase("type");

        String value = termQuery.getTerm().text();
        List<UniProtDatabaseDetail> dbxrefs =
                UniProtCrossReferenceDisplayOrder.INSTANCE.getOrderedDatabases();

        boolean isValueAValidDatabase =
                dbxrefs.stream()
                        .map(UniProtDatabaseDetail::getName)
                        .anyMatch(databaseType -> databaseType.equalsIgnoreCase(value));

        return isFieldNameType && isValueAValidDatabase;
    }

    private static boolean isTaxonomyRelatedQuery(String fieldName) {
        return fieldName.equalsIgnoreCase("organism")
                || fieldName.equalsIgnoreCase("host")
                || fieldName.equalsIgnoreCase("taxonomy");
    }

    private static boolean isLocation(BooleanQuery booleanQuery) {
        for (BooleanClause clause : booleanQuery.clauses()) {
            if (clause.getQuery() instanceof TermQuery) {
                TermQuery query = (TermQuery) clause.getQuery();
                if (isLocationTermQuery(query.getTerm().field())) {
                    return true;
                }
            } else if (clause.getQuery() instanceof PhraseQuery) {
                PhraseQuery phraseQuery = (PhraseQuery) clause.getQuery();
                String phraseField = phraseQuery.getTerms()[0].field();
                if (isLocationTermQuery(phraseField)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isLocationTermQuery(String fieldName) {
        return fieldName.equalsIgnoreCase("location");
    }

    private static boolean isCofactor(BooleanQuery booleanQuery) {
        for (BooleanClause clause : booleanQuery.clauses()) {
            if (clause.getQuery() instanceof TermQuery) {
                TermQuery query = (TermQuery) clause.getQuery();
                if (isCofactorTermQuery(query.getTerm().field())) {
                    return true;
                }
            } else if (clause.getQuery() instanceof PhraseQuery) {
                PhraseQuery phraseQuery = (PhraseQuery) clause.getQuery();
                String phraseField = phraseQuery.getTerms()[0].field();
                if (isCofactorTermQuery(phraseField)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isCofactorTermQuery(String fieldName) {
        return fieldName.equalsIgnoreCase("chebi");
    }

    private static boolean isNote(BooleanQuery booleanQuery) {
        for (BooleanClause clause : booleanQuery.clauses()) {
            if (clause.getQuery() instanceof TermQuery) {
                TermQuery query = (TermQuery) clause.getQuery();
                if (isNoteTermQuery(query.getTerm().field())) {
                    return true;
                }
            } else if (clause.getQuery() instanceof PhraseQuery) {
                PhraseQuery phraseQuery = (PhraseQuery) clause.getQuery();
                String phraseField = phraseQuery.getTerms()[0].field();
                if (isNoteTermQuery(phraseField)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isAnnotation(BooleanQuery booleanQuery) {
        for (BooleanClause clause : booleanQuery.clauses()) {
            if (clause.getQuery() instanceof TermQuery) {
                TermQuery query = (TermQuery) clause.getQuery();
                if (isCommentOrFeatureType(query.getTerm().field(), query.getTerm().text())) {
                    return true;
                }
            } else if (clause.getQuery() instanceof PhraseQuery) {
                PhraseQuery phraseQuery = (PhraseQuery) clause.getQuery();
                String phraseField = phraseQuery.getTerms()[0].field();
                String phraseValue =
                        Arrays.stream(phraseQuery.getTerms())
                                .map(Term::text)
                                .collect(Collectors.joining(" "));
                if (isCommentOrFeatureType(phraseField, phraseValue)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isCommentOrFeatureType(String fieldName, String value) {
        return fieldName.equalsIgnoreCase("type") && (isComment(value) || isFeature(value));
    }

    private static boolean isFeature(String type) {
        return featureMappingType.containsKey(type);
    }

    private static boolean isComment(String type) {
        return commentMappingType.containsKey(type);
    }

    private static boolean isCitation(String fieldName) {
        return fieldName.equalsIgnoreCase("author")
                || fieldName.equalsIgnoreCase("journal")
                || fieldName.equalsIgnoreCase("published")
                || fieldName.equalsIgnoreCase("title")
                || fieldName.equalsIgnoreCase("citation");
    }

    private static boolean isNoteTermQuery(String fieldName) {
        return fieldName.equalsIgnoreCase("note");
    }

    private static String getFeatureOrCommentType(String type) {
        String parsedType = "";
        if (featureMappingType.containsKey(type)) {
            parsedType = featureMappingType.get(type);
        } else if (commentMappingType.containsKey(type)) {
            parsedType = commentMappingType.get(type);
        }
        return parsedType;
    }

    private static String getTypePrefix(String type) {
        String prefix = "cc";
        if (isFeature(type)) {
            prefix = "ft";
        }
        return prefix;
    }

    private static void initCommentMappingType() {
        Arrays.stream(CommentType.values())
                .forEach(
                        commentType -> {
                            commentMappingType.put(
                                    commentType.name().toLowerCase(),
                                    commentType.name().toLowerCase());
                            commentMappingType.put(
                                    commentType.getDisplayName().toLowerCase(),
                                    commentType.name().toLowerCase());
                            commentMappingType.put(
                                    commentType.toXmlDisplayName().toLowerCase(),
                                    commentType.name().toLowerCase());
                        });
        // Adding comments sub-items

        // Biophysicochemical properties
        commentMappingType.put("absorption", "bpcp_absorption");
        commentMappingType.put("kinetic", "bpcp_kinetics");
        commentMappingType.put("ph dependence", "bpcp_ph_dependence");
        commentMappingType.put("redox potential", "bpcp_redox_potential");
        commentMappingType.put("temperature dependence", "bpcp_temp_dependence");

        // Alternative products
        commentMappingType.put("alternative promoter usage", "ap_apu");
        commentMappingType.put("alternative splicing", "ap_as");
        commentMappingType.put("alternative initiation", "ap_ai");
        commentMappingType.put("ribosomal frameshifting", "ap_rf");

        // Sequence caution
        commentMappingType.put("frameshift", "sc_framesh");
        commentMappingType.put("erroneous initiation", "sc_einit");
        commentMappingType.put("erroneous termination", "sc_eterm");
        commentMappingType.put("erroneous gene model prediction", "sc_epred");
        commentMappingType.put("erroneous translation", "sc_etran");
        commentMappingType.put("miscellaneous discrepancy", "sc_misc");

        // System.out.println(commentMappingType);
    }

    private static void initFeatureMappingType() {
        Arrays.stream(UniprotKBFeatureType.values())
                .forEach(
                        featureType -> {
                            featureMappingType.put(
                                    featureType.name().toLowerCase(),
                                    featureType.name().toLowerCase());
                            featureMappingType.put(
                                    featureType.getValue().toLowerCase(),
                                    featureType.name().toLowerCase());
                        });

        // Adding featureCategories
        featureMappingType.put("molecule_processing", "molecule_processing");
        featureMappingType.put("positional", "positional");
        featureMappingType.put("secstruct", "secstruct");
        featureMappingType.put("natural_variations", "variants");
        featureMappingType.put("sites", "sites");

        // System.out.println(featureMappingType);
    }
}
