package uk.ac.ebi.uniprot.uuw.advanced.search.query;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.util.BytesRef;
import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseType;
import uk.ac.ebi.kraken.interfaces.uniprot.ProteinExistence;
import uk.ac.ebi.kraken.interfaces.uniprot.comments.CommentType;
import uk.ac.ebi.kraken.interfaces.uniprot.features.FeatureType;
import uk.ac.ebi.uniprot.dataservice.restful.features.domain.FeatureCategory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is responsible to parse
 *    FROM old uniprot lucene query string format
 *    TO new uniprot solr query string format format
 *
 * @author lgonzales
 */
public class RequestQueryParser {

    private static final Map<String,String> commentMappingType = new HashMap<>();

    private static final Map<String,String> featureMappingType = new HashMap<>();

    static {
        initFeatureMappingType();
        initCommentMappingType();
    }

    public static String parse(String queryString){
        StandardQueryParser standardQueryParser = new StandardQueryParser();
        try {
            //System.out.println("INN: "+queryString);
            Query query = standardQueryParser.parse(queryString,"");
            Query parsedQuery = parse(query,queryString.contains("location"),queryString.contains("cofactor"));
            //System.out.println("OUT: "+parsedQuery);
            //System.out.println("");
            return parsedQuery.toString();
        } catch (QueryNodeException e) {
            throw new IllegalArgumentException("Error parsing requested Query ",e);
        }
    }


    private static Query parse(Query inputQuery,boolean hasLocationFilter,boolean hasCofactorFilter){
        Query parsedQuery;
        if(inputQuery instanceof TermQuery){
            parsedQuery = parseTermQuery((TermQuery) inputQuery,hasLocationFilter,hasCofactorFilter);
        }else if(inputQuery instanceof TermRangeQuery){
            parsedQuery = parseTermRangeQuery((TermRangeQuery) inputQuery);
        }else if(inputQuery instanceof BooleanQuery){
            parsedQuery = parseBooleanQuery((BooleanQuery) inputQuery,hasLocationFilter,hasCofactorFilter);
        }else{
            throw new IllegalArgumentException("Unsuported query type: "+inputQuery.getClass());
        }
        return parsedQuery;
    }

    private static Query parseTermQuery(TermQuery termQuery,boolean hasLocationFilter,boolean hasCofactorFilter) {
        Query parsedQuery;
        if(isCommentOrFeatureType(termQuery)) { //for features and comment
            String prefix = getTypePrefix(termQuery.getTerm().text());
            String type = getFeatureOrCommentType(termQuery.getTerm().text());
            Term parsedTerm = new Term(prefix+"_"+type, "*");
            parsedQuery = new TermQuery(parsedTerm);
        }else if(isCitation(termQuery)){
            parsedQuery = parseCitationQuery(termQuery);
        }else if(isCofactorTermQuery(termQuery)) {
            Term parsedTerm = new Term("cc_cofactor_chebi", termQuery.getTerm().bytes());
            parsedQuery = new TermQuery(parsedTerm);
        }else if(isLocationTermQuery(termQuery)) {
            Term parsedTerm = new Term("cc_scl_term_location", termQuery.getTerm().bytes());
            parsedQuery = new TermQuery(parsedTerm);
        }else if(isNoteTermQuery(termQuery)) {
            parsedQuery = parseNoteQuery(termQuery,hasLocationFilter,hasCofactorFilter);
        }else if(isTaxonomyRelatedQuery(termQuery)) {
            parsedQuery = parseTaxonomyRelatedQuery(termQuery);
        }else if(isDatabaseCrossReference(termQuery)) {
            Term parsedTerm = new Term("database", termQuery.getTerm().text());
            parsedQuery = new TermQuery(parsedTerm);
        }else if(termQuery.getTerm().field().equalsIgnoreCase("existence")) {
            Term parsedTerm = new Term("existence",ProteinExistence.typeOf(termQuery.getTerm().text()).name().toLowerCase());
            parsedQuery = new TermQuery(parsedTerm);
        }else{
            Term parsedTerm = new Term(termQuery.getTerm().field(), termQuery.getTerm().bytes());
            parsedQuery = new TermQuery(parsedTerm);
        }
        return parsedQuery;
    }


    private static Query parseTermRangeQuery(TermRangeQuery termRangeQuery) {
        return new TermRangeQuery(termRangeQuery.getField(),termRangeQuery.getLowerTerm(),
                termRangeQuery.getUpperTerm(),termRangeQuery.includesLower(),termRangeQuery.includesUpper());
    }

    private static Query parseBooleanQuery(BooleanQuery booleanQuery,boolean hasLocationFilter,boolean hasCofactorFilter) {
        if(isAnnotation(booleanQuery)) {
            return parseAnnotationQuery(booleanQuery);
        }else if(isCofactor(booleanQuery)) {
            return parseCofactorBooleanQuery(booleanQuery);
        }else if(isLocation(booleanQuery)) {
            return parseLocationBooleanQuery(booleanQuery);
        }else if(isDatabase(booleanQuery)) {
            return parseDatabaseBooleanQuery(booleanQuery);
        }else if(isNote(booleanQuery)) {
            return parseNoteBooleanQuery(booleanQuery,hasLocationFilter, hasCofactorFilter);
        }else{
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            for (BooleanClause clause : booleanQuery.clauses()) {
                Query parsedClauseQuery = parse(clause.getQuery(),hasLocationFilter,hasCofactorFilter);
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
        for (BooleanClause clause : booleanQuery.clauses()) {
            if(clause.getQuery() instanceof TermQuery){
                TermQuery query = (TermQuery) clause.getQuery();
                if(query.getTerm().field().equalsIgnoreCase("type")){
                    type = getFeatureOrCommentType(query.getTerm().text());
                }
                if(query.getTerm().field().equalsIgnoreCase("annotation")){
                    annotation = query.getTerm().text();
                }
                if(query.getTerm().field().equalsIgnoreCase("evidence")){
                    evidence = query.getTerm().text();
                }
            }else if(clause.getQuery() instanceof TermRangeQuery){
                TermRangeQuery query = (TermRangeQuery) clause.getQuery();
                if(query.getField().equalsIgnoreCase("length")){
                    length = query;
                }
            }
        }
        String prefix = getTypePrefix(type);

        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        TermQuery typeQuery = new TermQuery(new Term(prefix+"_"+type,annotation));
        builder.add(typeQuery,BooleanClause.Occur.MUST);
        if(!evidence.isEmpty()){
            TermQuery evidenceQuery = new TermQuery(new Term(prefix+"ev_"+type,evidence));
            builder.add(evidenceQuery,BooleanClause.Occur.MUST);
        }
        if(length != null){
            String fieldName = prefix+"len_"+type;
            TermRangeQuery lengthQuery = new TermRangeQuery(fieldName,length.getLowerTerm(),
                    length.getUpperTerm(),length.includesLower(),length.includesUpper());
            builder.add(lengthQuery,BooleanClause.Occur.MUST);
        }
        return builder.build();
    }


    private static Query parseTaxonomyRelatedQuery(TermQuery termQuery) {
        String term = termQuery.getTerm().field();
        String value = termQuery.getTerm().text();
        if(value.matches("^(\\d+)$")){
            term += "_id";
        }else if(value.matches("^.*(\\[(\\d+)\\])(\\s)*$")){
            term += "_id";
            Matcher m = Pattern.compile("^.*(\\[(\\d+)\\])(\\s)*$").matcher(value);
            if(m.matches()) {
                value = m.group(2);
            }
        }else{
            term += "_name";
        }
        Term parsedTerm = new Term(term, value);
        return new TermQuery(parsedTerm);
    }

    private static Query parseDatabaseBooleanQuery(BooleanQuery booleanQuery) {
        String type = "";
        String id = "";
        for (BooleanClause clause : booleanQuery.clauses()) {
            if(clause.getQuery() instanceof TermQuery){
                TermQuery query = (TermQuery) clause.getQuery();
                if(query.getTerm().field().equalsIgnoreCase("type")){
                    type = query.getTerm().text();
                }else if(query.getTerm().field().equalsIgnoreCase("id")) {
                    id = query.getTerm().text();
                }
            }
        }
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        TermQuery typeQuery = new TermQuery(new Term("xref",type.toLowerCase()+"-"+id.toLowerCase()));
        builder.add(typeQuery,BooleanClause.Occur.MUST);
        return builder.build();
    }

    private static Query parseCitationQuery(TermQuery termQuery) {
        Query parsedQuery;
        String fieldName = termQuery.getTerm().field();
        if(fieldName.equalsIgnoreCase("citation")){
            fieldName = "lit_title";
        }else if(fieldName.equalsIgnoreCase("published")) {
            fieldName = "lit_pubdate";
        }else { // for author, title or journal
            fieldName = "lit_"+fieldName;
        }
        Term parsedTerm = new Term(fieldName, termQuery.getTerm().bytes());
        parsedQuery = new TermQuery(parsedTerm);
        return parsedQuery;
    }

    private static Query parseLocationBooleanQuery(BooleanQuery booleanQuery) {
        String evidence = "";
        String location = "";
        for (BooleanClause clause : booleanQuery.clauses()) {
            if(clause.getQuery() instanceof TermQuery){
                TermQuery query = (TermQuery) clause.getQuery();
                if(query.getTerm().field().equalsIgnoreCase("evidence")){
                    evidence = query.getTerm().text();
                }else if(query.getTerm().field().equalsIgnoreCase("location")) {
                    location = query.getTerm().text();
                }
            }
        }
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        TermQuery typeQuery = new TermQuery(new Term("cc_scl_term_location",location));
        builder.add(typeQuery,BooleanClause.Occur.MUST);
        if(!evidence.isEmpty()) {
            TermQuery evidenceQuery = new TermQuery(new Term("ccev_scl_term_location", evidence));
            builder.add(evidenceQuery, BooleanClause.Occur.MUST);
        }
        return builder.build();
    }

    private static Query parseCofactorBooleanQuery(BooleanQuery booleanQuery) {
        String evidence = "";
        String cofactor = "";
        for (BooleanClause clause : booleanQuery.clauses()) {
            if(clause.getQuery() instanceof TermQuery){
                TermQuery query = (TermQuery) clause.getQuery();
                if(query.getTerm().field().equalsIgnoreCase("evidence")){
                    evidence = query.getTerm().text();
                }else if(query.getTerm().field().equalsIgnoreCase("chebi")) {
                    cofactor = query.getTerm().text();
                }
            }
        }
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        TermQuery typeQuery = new TermQuery(new Term("cc_cofactor_chebi",cofactor));
        builder.add(typeQuery,BooleanClause.Occur.MUST);
        if(!evidence.isEmpty()) {
            TermQuery evidenceQuery = new TermQuery(new Term("ccev_cofactor_chebi", evidence));
            builder.add(evidenceQuery, BooleanClause.Occur.MUST);
        }
        return builder.build();
    }

    private static Query parseNoteBooleanQuery(BooleanQuery booleanQuery, boolean hasLocationFilter, boolean hasCofactorFilter) {
        String evidence = "";
        String note = "";
        for (BooleanClause clause : booleanQuery.clauses()) {
            if(clause.getQuery() instanceof TermQuery){
                TermQuery query = (TermQuery) clause.getQuery();
                if(query.getTerm().field().equalsIgnoreCase("evidence")){
                    evidence = query.getTerm().text();
                }else if(query.getTerm().field().equalsIgnoreCase("note")) {
                    note = query.getTerm().text();
                }
            }
        }
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        Term term = parseNoteTerm(note, hasLocationFilter, hasCofactorFilter);
        builder.add(new TermQuery(term),BooleanClause.Occur.MUST);
        if(!evidence.isEmpty()) {
            Term evidenceTerm = parseNoteEvidenceTerm(evidence, hasLocationFilter, hasCofactorFilter);
            builder.add(new TermQuery(evidenceTerm), BooleanClause.Occur.MUST);
        }
        return builder.build();
    }


    private static Query parseNoteQuery(TermQuery termQuery, boolean hasLocationFilter, boolean hasCofactorFilter) {
        Term term = parseNoteTerm(termQuery.getTerm().text(), hasLocationFilter, hasCofactorFilter);
        return new TermQuery(term);
    }

    private static Term parseNoteTerm(String value, boolean hasLocationFilter, boolean hasCofactorFilter) {
        Term term;
        if(hasCofactorFilter){
            term = new Term("cc_cofactor_note",value);
        } else if(hasLocationFilter){
            term = new Term("cc_scl_note",value);
        } else{
            term = new Term("note",value);
        }
        return term;
    }

    private static Term parseNoteEvidenceTerm(String value, boolean hasLocationFilter, boolean hasCofactorFilter) {
        Term term;
        if(hasCofactorFilter){
            term = new Term("ccev_cofactor_note",value);
        } else if(hasLocationFilter){
            term = new Term("ccev_scl_note",value);
        } else{
            term = new Term("note",value);
        }
        return term;
    }

    private static boolean isDatabase(BooleanQuery booleanQuery) {
        for (BooleanClause clause : booleanQuery.clauses()) {
            if(clause.getQuery() instanceof TermQuery){
                TermQuery query = (TermQuery) clause.getQuery();
                if(isDatabaseCrossReference(query)){
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isDatabaseCrossReference(TermQuery termQuery) {
        boolean isFieldNameType = termQuery.getTerm().field().equalsIgnoreCase("type");

        String value = termQuery.getTerm().text();
        boolean isValueAValidDatabase = Arrays.stream(DatabaseType.values())
                    .anyMatch(databaseType -> databaseType.getName().equalsIgnoreCase(value) ||
                        databaseType.name().equalsIgnoreCase(value));

        return isFieldNameType && isValueAValidDatabase;
    }

    private static boolean isTaxonomyRelatedQuery(TermQuery termQuery) {
        String fieldName = termQuery.getTerm().field();
        return fieldName.equalsIgnoreCase("organism") || fieldName.equalsIgnoreCase("host") ||
                fieldName.equalsIgnoreCase("taxonomy");
    }

    private static boolean isLocation(BooleanQuery booleanQuery) {
        for (BooleanClause clause : booleanQuery.clauses()) {
            if(clause.getQuery() instanceof TermQuery){
                TermQuery query = (TermQuery) clause.getQuery();
                if(isLocationTermQuery(query)){
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isLocationTermQuery(TermQuery termQuery) {
        return termQuery.getTerm().field().equalsIgnoreCase("location");
    }

    private static boolean isCofactor(BooleanQuery booleanQuery) {
        for (BooleanClause clause : booleanQuery.clauses()) {
            if(clause.getQuery() instanceof TermQuery){
                TermQuery query = (TermQuery) clause.getQuery();
                if(isCofactorTermQuery(query)){
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isCofactorTermQuery(TermQuery termQuery) {
        return termQuery.getTerm().field().equalsIgnoreCase("chebi");
    }

    private static boolean isNote(BooleanQuery booleanQuery) {
        for (BooleanClause clause : booleanQuery.clauses()) {
            if(clause.getQuery() instanceof TermQuery){
                TermQuery query = (TermQuery) clause.getQuery();
                if(isNoteTermQuery(query)){
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isAnnotation(BooleanQuery booleanQuery) {
        for (BooleanClause clause : booleanQuery.clauses()) {
            if(clause.getQuery() instanceof TermQuery){
                TermQuery query = (TermQuery) clause.getQuery();
                if(isCommentOrFeatureType(query)){
                    return true;
                }
            }
        }
        return false;
    }
    private static boolean isCommentOrFeatureType(TermQuery termQuery) {
        String value = termQuery.getTerm().text();
        return termQuery.getTerm().field().equalsIgnoreCase("type")
                && (isComment(value) || isFeature(value));
    }

    private static boolean isFeature(String type) {
        return featureMappingType.containsKey(type);
    }

    private static boolean isComment(String type) {
        return commentMappingType.containsKey(type);
    }

    private static boolean isCitation(TermQuery termQuery) {
        String fieldName = termQuery.getTerm().field();
        return fieldName.equalsIgnoreCase("author") || fieldName.equalsIgnoreCase("journal") ||
               fieldName.equalsIgnoreCase("published") || fieldName.equalsIgnoreCase("title") ||
               fieldName.equalsIgnoreCase("citation");
    }

    private static boolean isNoteTermQuery(TermQuery termQuery) {
        return termQuery.getTerm().field().equalsIgnoreCase("note");
    }

    private static String getFeatureOrCommentType(String type) {
        String parsedType = "";
        if(featureMappingType.containsKey(type)){
            parsedType = featureMappingType.get(type);
        }else if(commentMappingType.containsKey(type)){
            parsedType = commentMappingType.get(type);
        }
        return parsedType;
    }

    private static String getTypePrefix(String type) {
        String prefix = "cc";
        if(isFeature(type)){
            prefix = "ft";
        }
        return prefix;
    }

    private static void initCommentMappingType() {
        Arrays.stream(CommentType.values()).forEach(commentType -> {
            commentMappingType.put(commentType.name().toLowerCase(),commentType.name().toLowerCase());
            commentMappingType.put(commentType.toDisplayName().toLowerCase(),commentType.name().toLowerCase());
            commentMappingType.put(commentType.toXmlDisplayName().toLowerCase(),commentType.name().toLowerCase());
        });
        //Adding comments sub-items

        //Biophysicochemical properties
        commentMappingType.put("absorption","bpcp_absorption");
        commentMappingType.put("kinetic","bpcp_kinetics");
        commentMappingType.put("ph dependence","bpcp_ph_dependence");
        commentMappingType.put("redox potential","bpcp_redox_potential");
        commentMappingType.put("temperature dependence","bpcp_temp_dependence");

        //Alternative products
        commentMappingType.put("alternative promoter usage","ap_apu");
        commentMappingType.put("alternative splicing","ap_as");
        commentMappingType.put("alternative initiation","ap_ai");
        commentMappingType.put("ribosomal frameshifting","ap_rf");

        //Sequence caution
        commentMappingType.put("frameshift","sc_framesh");
        commentMappingType.put("erroneous initiation","sc_einit");
        commentMappingType.put("erroneous termination","sc_eterm");
        commentMappingType.put("erroneous gene model prediction","sc_epred");
        commentMappingType.put("erroneous translation","sc_etran");
        commentMappingType.put("miscellaneous discrepancy","sc_misc");

        //System.out.println(commentMappingType);
    }

    private static void initFeatureMappingType() {
        Arrays.stream(FeatureType.values()).forEach(featureType -> {
            featureMappingType.put(featureType.name().toLowerCase(),featureType.name().toLowerCase());
            featureMappingType.put(featureType.getValue().toLowerCase(),featureType.name().toLowerCase());
        });

        //Adding featureCategories
        featureMappingType.put("molecule_processing","molecule_processing");
        featureMappingType.put("positional","positional");
        featureMappingType.put("secstruct","secstruct");
        featureMappingType.put("natural_variations","variants");
        featureMappingType.put("sites","sites");

        //System.out.println(featureMappingType);
    }
}
