package uk.ac.ebi.uniprot.uniprotkb.configuration;

import uk.ac.ebi.uniprot.rest.search.SearchField;
import uk.ac.ebi.uniprot.rest.search.SearchFieldType;
import uk.ac.ebi.uniprot.rest.validation.validator.FieldValueValidator;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class UniProtField {

    public enum Sort{
        accession("accession_id"),
        mnemonic("mnemonic_sort"),
        name("name_sort"),
        annotation_score("annotation_score"),
        gene("gene_sort"),
        length("length"),
        mass("mass"),
        organism("organism_sort");

        private String solrFieldName;

        Sort(String solrFieldName){
            this.solrFieldName = solrFieldName;
        }

        public String getSolrFieldName() {
            return solrFieldName;
        }

        @Override
        public String toString() {
            return this.solrFieldName;
        }
    }

    public enum Search implements SearchField{
        accession_id(SearchFieldType.TERM, FieldValueValidator::isAccessionValid, 1.1f),            // uniprot entry accession
        accession(SearchFieldType.TERM,FieldValueValidator::isAccessionValid, null),            // uniprot entry accession
        mnemonic(SearchFieldType.TERM),
        mnemonic_default(SearchFieldType.TERM,null,10.0f),  // uniprot entry name
        reviewed(SearchFieldType.TERM,FieldValueValidator::isBooleanValue, null),             // reviewed or not reviewed
        name(SearchFieldType.TERM),              // protein name
        sec_acc(SearchFieldType.TERM),              // secondary accessions, other accessions
        content(SearchFieldType.TERM), //used in the default search
        keyword(SearchFieldType.TERM),
        ec(SearchFieldType.TERM,null,1.1f),                   // EC number
        ec_exact(SearchFieldType.TERM),
        gene(SearchFieldType.TERM,null,2.0f),                 // gene name
        gene_exact(SearchFieldType.TERM),                 // exact gene name
        organism_name(SearchFieldType.TERM,null,2.0f),
        organism_id(SearchFieldType.TERM,FieldValueValidator::isNumberValue, 2.0f),
        host_name(SearchFieldType.TERM),
        host_id(SearchFieldType.TERM,FieldValueValidator::isNumberValue, null),
        taxonomy_name(SearchFieldType.TERM,null, null),
        taxonomy_id(SearchFieldType.TERM,FieldValueValidator::isNumberValue, null),
        popular_organism(SearchFieldType.TERM),
        other_organism(SearchFieldType.TERM),
        organelle(SearchFieldType.TERM),

        modified(SearchFieldType.RANGE),    // last updated
        created(SearchFieldType.RANGE),    // entry first created
        sequence_modified(SearchFieldType.RANGE), //sequence last updated
        lit_pubdate(SearchFieldType.RANGE),

        features(SearchFieldType.TERM),
        ft_sites(SearchFieldType.TERM),
        ftev_sites(SearchFieldType.TERM),
        ftlen_sites(SearchFieldType.RANGE),
        ft_molecule_processing(SearchFieldType.TERM),
        ftev_molecule_processing(SearchFieldType.TERM),
        ftlen_molecule_processing(SearchFieldType.RANGE),
        ft_variants(SearchFieldType.TERM),
        ftev_variants(SearchFieldType.TERM),
        ftlen_variants(SearchFieldType.RANGE),
        ft_positional(SearchFieldType.TERM),
        ftev_positional(SearchFieldType.TERM),
        ftlen_positional(SearchFieldType.RANGE),
        ft_secstruct(SearchFieldType.TERM),
        ftev_secstruct(SearchFieldType.TERM),
        ftlen_secstruct(SearchFieldType.RANGE),

        ft_init_met(SearchFieldType.TERM),
        ftev_init_met(SearchFieldType.TERM),
        ftlen_init_met(SearchFieldType.RANGE),
        ft_signal(SearchFieldType.TERM),
        ftev_signal(SearchFieldType.TERM),
        ftlen_signal(SearchFieldType.RANGE),
        ft_propep(SearchFieldType.TERM),
        ftev_propep(SearchFieldType.TERM),
        ftlen_propep(SearchFieldType.RANGE),
        ft_transit(SearchFieldType.TERM),
        ftev_transit(SearchFieldType.TERM),
        ftlen_transit(SearchFieldType.RANGE),
        ft_chain(SearchFieldType.TERM),
        ftev_chain(SearchFieldType.TERM),
        ftlen_chain(SearchFieldType.RANGE),
        ft_peptide(SearchFieldType.TERM),
        ftev_peptide(SearchFieldType.TERM),
        ftlen_peptide(SearchFieldType.RANGE),
        ft_topo_dom(SearchFieldType.TERM),
        ftev_topo_dom(SearchFieldType.TERM),
        ftlen_topo_dom(SearchFieldType.RANGE),
        ft_transmem(SearchFieldType.TERM),
        ftev_transmem(SearchFieldType.TERM),
        ftlen_transmem(SearchFieldType.RANGE),
        ft_domain(SearchFieldType.TERM),
        ftev_domain(SearchFieldType.TERM),
        ftlen_domain(SearchFieldType.RANGE),
        ft_repeat(SearchFieldType.TERM),
        ftev_repeat(SearchFieldType.TERM),
        ftlen_repeat(SearchFieldType.RANGE),
        ft_ca_bind(SearchFieldType.TERM),
        ftev_ca_bind(SearchFieldType.TERM),
        ftlen_ca_bind(SearchFieldType.RANGE),
        ft_zn_fing(SearchFieldType.TERM),
        ftev_zn_fing(SearchFieldType.TERM),
        ftlen_zn_fing(SearchFieldType.RANGE),
        ft_dna_bind(SearchFieldType.TERM),
        ftev_dna_bind(SearchFieldType.TERM),
        ftlen_dna_bind(SearchFieldType.RANGE),
        ft_np_bind(SearchFieldType.TERM),
        ftev_np_bind(SearchFieldType.TERM),
        ftlen_np_bind(SearchFieldType.RANGE),
        ft_region(SearchFieldType.TERM),
        ftev_region(SearchFieldType.TERM),
        ftlen_region(SearchFieldType.RANGE),
        ft_coiled(SearchFieldType.TERM),
        ftev_coiled(SearchFieldType.TERM),
        ftlen_coiled(SearchFieldType.RANGE),
        ft_motif(SearchFieldType.TERM),
        ftev_motif(SearchFieldType.TERM),
        ftlen_motif(SearchFieldType.RANGE),
        ft_compbias(SearchFieldType.TERM),
        ftev_compbias(SearchFieldType.TERM),
        ftlen_compbias(SearchFieldType.RANGE),
        ft_act_site(SearchFieldType.TERM),
        ftev_act_site(SearchFieldType.TERM),
        ftlen_act_site(SearchFieldType.RANGE),
        ft_metal(SearchFieldType.TERM),
        ftev_metal(SearchFieldType.TERM),
        ftlen_metal(SearchFieldType.RANGE),
        ft_binding(SearchFieldType.TERM),
        ftev_binding(SearchFieldType.TERM),
        ftlen_binding(SearchFieldType.RANGE),
        ft_site(SearchFieldType.TERM),
        ftev_site(SearchFieldType.TERM),
        ftlen_site(SearchFieldType.RANGE),
        ft_non_std(SearchFieldType.TERM),
        ftev_non_std(SearchFieldType.TERM),
        ftlen_non_std(SearchFieldType.RANGE),
        ft_mod_res(SearchFieldType.TERM),
        ftev_mod_res(SearchFieldType.TERM),
        ftlen_mod_res(SearchFieldType.RANGE),
        ft_lipid(SearchFieldType.TERM),
        ftev_lipid(SearchFieldType.TERM),
        ftlen_lipid(SearchFieldType.RANGE),
        ft_carbohyd(SearchFieldType.TERM),
        ftev_carbohyd(SearchFieldType.TERM),
        ftlen_carbohyd(SearchFieldType.RANGE),
        ft_disulfid(SearchFieldType.TERM),
        ftev_disulfid(SearchFieldType.TERM),
        ftlen_disulfid(SearchFieldType.RANGE),
        ft_crosslnk(SearchFieldType.TERM),
        ftev_crosslnk(SearchFieldType.TERM),
        ftlen_crosslnk(SearchFieldType.RANGE),
        ft_var_seq(SearchFieldType.TERM),
        ftev_var_seq(SearchFieldType.TERM),
        ftlen_var_seq(SearchFieldType.RANGE),
        ft_variant(SearchFieldType.TERM),
        ftev_variant(SearchFieldType.TERM),
        ftlen_variant(SearchFieldType.RANGE),
        ft_mutagen(SearchFieldType.TERM),
        ftev_mutagen(SearchFieldType.TERM),
        ftlen_mutagen(SearchFieldType.RANGE),
        ft_unsure(SearchFieldType.TERM),
        ftev_unsure(SearchFieldType.TERM),
        ftlen_unsure(SearchFieldType.RANGE),
        ft_conflict(SearchFieldType.TERM),
        ftev_conflict(SearchFieldType.TERM),
        ftlen_conflict(SearchFieldType.RANGE),
        ft_non_cons(SearchFieldType.TERM),
        ftev_non_cons(SearchFieldType.TERM),
        ftlen_non_cons(SearchFieldType.RANGE),
        ft_non_ter(SearchFieldType.TERM),
        ftev_non_ter(SearchFieldType.TERM),
        ftlen_non_ter(SearchFieldType.RANGE),
        ft_helix(SearchFieldType.TERM),
        ftev_helix(SearchFieldType.TERM),
        ftlen_helix(SearchFieldType.RANGE),
        ft_turn(SearchFieldType.TERM),
        ftev_turn(SearchFieldType.TERM),
        ftlen_turn(SearchFieldType.RANGE),
        ft_strand(SearchFieldType.TERM),
        ftev_strand(SearchFieldType.TERM),
        ftlen_strand(SearchFieldType.RANGE),
        ft_intramem(SearchFieldType.TERM),
        ftev_intramem(SearchFieldType.TERM),
        ftlen_intramem(SearchFieldType.RANGE),



        xref(SearchFieldType.TERM),    //database cross references
        database(SearchFieldType.TERM),
        lit_author(SearchFieldType.TERM),  //reference author
        lit_organisation(SearchFieldType.TERM), //reference organisation
        lit_title(SearchFieldType.TERM), //reference title
        lit_pubmed(SearchFieldType.TERM), //reference pubmed id
        lit_journal(SearchFieldType.TERM),
        fragment(SearchFieldType.TERM),           // indicates whether the protein has non-terminal endings
        existence (SearchFieldType.TERM),
        is_isoform(SearchFieldType.TERM,FieldValueValidator::isBooleanValue, null),
        length(SearchFieldType.RANGE),
        mass(SearchFieldType.RANGE),
        precursor(SearchFieldType.TERM),
        active(SearchFieldType.TERM,FieldValueValidator::isBooleanValue, null),
        d3structure(SearchFieldType.TERM,FieldValueValidator::isBooleanValue, null),

        tissue(SearchFieldType.TERM),  //rc line
        strain(SearchFieldType.TERM), //rc line
        plasmid(SearchFieldType.TERM), //rc line
        transposon(SearchFieldType.TERM), //rc line
        scope(SearchFieldType.TERM),   //rp line


        //subcell location cc
        cc_scl_term(SearchFieldType.TERM),
        cc_scl_note(SearchFieldType.TERM),
        ccev_scl_term(SearchFieldType.TERM),
        ccev_scl_note(SearchFieldType.TERM),

        //AP
        cc_ap(SearchFieldType.TERM),
        cc_ap_apu(SearchFieldType.TERM),
        cc_ap_as(SearchFieldType.TERM),
        cc_ap_ai(SearchFieldType.TERM),
        cc_ap_rf(SearchFieldType.TERM),
        ccev_ap(SearchFieldType.TERM),
        ccev_ap_apu(SearchFieldType.TERM),
        ccev_ap_as(SearchFieldType.TERM),
        ccev_ap_ai(SearchFieldType.TERM),
        ccev_ap_rf(SearchFieldType.TERM),
        //bpcp
        cc_bpcp(SearchFieldType.TERM),
        cc_bpcp_absorption(SearchFieldType.TERM),
        cc_bpcp_kinetics(SearchFieldType.TERM),
        cc_bpcp_ph_dependence(SearchFieldType.TERM),
        cc_bpcp_redox_potential(SearchFieldType.TERM),
        cc_bpcp_temp_dependence(SearchFieldType.TERM),
        ccev_bpcp(SearchFieldType.TERM),
        ccev_bpcp_absorption(SearchFieldType.TERM),
        ccev_bpcp_kinetics(SearchFieldType.TERM),
        ccev_bpcp_ph_dependence(SearchFieldType.TERM),
        ccev_bpcp_redox_potential(SearchFieldType.TERM),
        ccev_bpcp_temp_dependence(SearchFieldType.TERM),
        //cofactor
        cc_cofactor_chebi(SearchFieldType.TERM),
        cc_cofactor_note(SearchFieldType.TERM),
        ccev_cofactor_chebi(SearchFieldType.TERM),
        ccev_cofactor_note(SearchFieldType.TERM),

        //sequence caution
        cc_sc(SearchFieldType.TERM),
        cc_sc_framesh(SearchFieldType.TERM),
        cc_sc_einit(SearchFieldType.TERM),
        cc_sc_eterm(SearchFieldType.TERM),
        cc_sc_epred(SearchFieldType.TERM),
        cc_sc_etran(SearchFieldType.TERM),
        cc_sc_misc(SearchFieldType.TERM),

        ccev_sc(SearchFieldType.TERM),
        ccev_sc_misc(SearchFieldType.TERM),

        cc_function(SearchFieldType.TERM),
        ccev_function(SearchFieldType.TERM),
        cc_catalytic_activity(SearchFieldType.TERM),
        ccev_catalytic_activity(SearchFieldType.TERM),
        cc_cofactor(SearchFieldType.TERM),
        ccev_cofactor(SearchFieldType.TERM),
        cc_enzyme_regulation(SearchFieldType.TERM),
        ccev_enzyme_regulation(SearchFieldType.TERM),
        cc_biophysicochemical_properties(SearchFieldType.TERM),
        ccev_biophysicochemical_properties(SearchFieldType.TERM),
        cc_pathway(SearchFieldType.TERM),
        ccev_pathway(SearchFieldType.TERM),
        cc_subunit(SearchFieldType.TERM),
        ccev_subunit(SearchFieldType.TERM),
        cc_interaction(SearchFieldType.TERM),
        ccev_interaction(SearchFieldType.TERM),
        cc_subcellular_location(SearchFieldType.TERM),
        ccev_subcellular_location(SearchFieldType.TERM),
        cc_alternative_products(SearchFieldType.TERM),
        ccev_alternative_products(SearchFieldType.TERM),
        cc_tissue_specificity(SearchFieldType.TERM),
        ccev_tissue_specificity(SearchFieldType.TERM),
        cc_developmental_stage(SearchFieldType.TERM),
        ccev_developmental_stage(SearchFieldType.TERM),
        cc_induction(SearchFieldType.TERM),
        ccev_induction(SearchFieldType.TERM),
        cc_domain(SearchFieldType.TERM),
        ccev_domain(SearchFieldType.TERM),
        cc_ptm(SearchFieldType.TERM),
        ccev_ptm(SearchFieldType.TERM),
        cc_rna_editing(SearchFieldType.TERM),
        ccev_rna_editing(SearchFieldType.TERM),
        cc_mass_spectrometry(SearchFieldType.TERM),
        ccev_mass_spectrometry(SearchFieldType.TERM),
        cc_polymorphism(SearchFieldType.TERM),
        ccev_polymorphism(SearchFieldType.TERM),
        cc_disease(SearchFieldType.TERM),
        ccev_disease(SearchFieldType.TERM),
        cc_disruption_phenotype(SearchFieldType.TERM),
        ccev_disruption_phenotype(SearchFieldType.TERM),
        cc_allergen(SearchFieldType.TERM),
        ccev_allergen(SearchFieldType.TERM),
        cc_toxic_dose(SearchFieldType.TERM),
        ccev_toxic_dose(SearchFieldType.TERM),
        cc_biotechnology(SearchFieldType.TERM),
        ccev_biotechnology(SearchFieldType.TERM),
        cc_pharmaceutical(SearchFieldType.TERM),
        ccev_pharmaceutical(SearchFieldType.TERM),
        cc_miscellaneous(SearchFieldType.TERM),
        ccev_miscellaneous(SearchFieldType.TERM),
        cc_similarity(SearchFieldType.TERM),
        ccev_similarity(SearchFieldType.TERM),
        cc_caution(SearchFieldType.TERM),
        ccev_caution(SearchFieldType.TERM),
        cc_sequence_caution(SearchFieldType.TERM),
        ccev_sequence_caution(SearchFieldType.TERM),
        cc_webresource(SearchFieldType.TERM),
        ccev_webresource(SearchFieldType.TERM),
        cc_page(SearchFieldType.TERM),
        ccev_page(SearchFieldType.TERM),
        cc_unknown(SearchFieldType.TERM),
        ccev_unknown(SearchFieldType.TERM),

        interactor(SearchFieldType.TERM),
        family(SearchFieldType.TERM),
        proteome(SearchFieldType.TERM,FieldValueValidator::isProteomeIdValue, null),
        proteomecomponent(SearchFieldType.TERM),
        annotation_score(SearchFieldType.TERM),

        go(SearchFieldType.TERM),
        go_ida(SearchFieldType.TERM),
        go_imp(SearchFieldType.TERM),
        go_igi(SearchFieldType.TERM),
        go_ipi(SearchFieldType.TERM),
        go_iep(SearchFieldType.TERM),
        go_tas(SearchFieldType.TERM),
        go_nas(SearchFieldType.TERM),
        go_ic(SearchFieldType.TERM),
        go_iss(SearchFieldType.TERM),
        go_iea(SearchFieldType.TERM),
        go_igc(SearchFieldType.TERM),
        go_rca(SearchFieldType.TERM),
        go_nd(SearchFieldType.TERM),
        go_exp(SearchFieldType.TERM),
        go_iba(SearchFieldType.TERM),
        go_ibd(SearchFieldType.TERM),
        go_ikr(SearchFieldType.TERM),
        go_ird(SearchFieldType.TERM),
        go_iso(SearchFieldType.TERM),
        go_isa(SearchFieldType.TERM),
        go_ism(SearchFieldType.TERM),
        go_hda(SearchFieldType.TERM),
        go_hmp(SearchFieldType.TERM),
        go_hgi(SearchFieldType.TERM),
        go_hep(SearchFieldType.TERM),
        go_htp(SearchFieldType.TERM),
        go_unknown(SearchFieldType.TERM);

        private final Predicate<String> fieldValueValidator;
        private final SearchFieldType searchFieldType;
        private final Float boostValue;

        Search(SearchFieldType searchFieldType){
            this.searchFieldType = searchFieldType;
            this.fieldValueValidator = null;
            this.boostValue = null;
        }

        Search(SearchFieldType searchFieldType,Predicate<String> fieldValueValidator,Float boostValue){
            this.searchFieldType = searchFieldType;
            this.fieldValueValidator  = fieldValueValidator;
            this.boostValue = boostValue;
        }

        public Predicate<String> getFieldValueValidator(){
            return this.fieldValueValidator;
        }

        public SearchFieldType getSearchFieldType(){
            return this.searchFieldType;
        }

        @Override
        public Float getBoostValue(){
            return this.boostValue;
        }

        private boolean hasBoostValue(){
            return this.boostValue != null;
        }

        @Override
        public boolean hasValidValue(String value) {
            return this.fieldValueValidator == null || this.fieldValueValidator.test(value);
        }

        @Override
        public String getName(){
            return this.name();
        }

    }

    public static List<SearchField> getBoostFields() {
        return Arrays.stream(Search.values())
                .filter(Search::hasBoostValue)
                .collect(Collectors.toList());
    }
    public enum Return {
        accession

    }
}
