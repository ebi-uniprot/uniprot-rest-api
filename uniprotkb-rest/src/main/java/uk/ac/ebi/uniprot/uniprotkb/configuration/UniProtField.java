package uk.ac.ebi.uniprot.uniprotkb.configuration;

import uk.ac.ebi.uniprot.rest.SearchFieldType;
import uk.ac.ebi.uniprot.rest.validation.validator.FieldValueValidator;

import java.util.function.Predicate;

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

    public enum Search {
        accession_id(SearchFieldType.TERM, FieldValueValidator::isAccessionValid),            // uniprot entry accession
        accession(SearchFieldType.TERM,FieldValueValidator::isAccessionValid),            // uniprot entry accession
        mnemonic(SearchFieldType.TERM,null),                   // uniprot entry name
        reviewed(SearchFieldType.TERM,FieldValueValidator::isBooleanValue),             // reviewed or not reviewed
        name(SearchFieldType.TERM,null),              // protein name
        sec_acc(SearchFieldType.TERM,null),              // secondary accessions, other accessions
        keyword(SearchFieldType.TERM,null),
        ec(SearchFieldType.TERM,null),                   // EC number
        ec_exact(SearchFieldType.TERM,null),
        gene(SearchFieldType.TERM,null),                 // gene name
        gene_exact(SearchFieldType.TERM,null),                 // exact gene name
        organism_name(SearchFieldType.TERM,null),
        organism_id(SearchFieldType.TERM,FieldValueValidator::isNumberValue),
        host_name(SearchFieldType.TERM,null),
        host_id(SearchFieldType.TERM,FieldValueValidator::isNumberValue),
        taxonomy_name(SearchFieldType.TERM,null),
        taxonomy_id(SearchFieldType.TERM,FieldValueValidator::isNumberValue),
        popular_organism(SearchFieldType.TERM,null),
        other_organism(SearchFieldType.TERM,null),
        organelle(SearchFieldType.TERM,null),

        modified(SearchFieldType.RANGE,null),    // last updated
        created(SearchFieldType.RANGE,null),    // entry first created
        sequence_modified(SearchFieldType.RANGE,null), //sequence last updated
        lit_pubdate(SearchFieldType.RANGE,null),

        features(SearchFieldType.TERM,null),
        ft_sites(SearchFieldType.TERM,null),
        ftev_sites(SearchFieldType.TERM,null),
        ftlen_sites(SearchFieldType.RANGE,null),
        ft_molecule_processing(SearchFieldType.TERM,null),
        ftev_molecule_processing(SearchFieldType.TERM,null),
        ftlen_molecule_processing(SearchFieldType.RANGE,null),
        ft_variants(SearchFieldType.TERM,null),
        ftev_variants(SearchFieldType.TERM,null),
        ftlen_variants(SearchFieldType.RANGE,null),
        ft_positional(SearchFieldType.TERM,null),
        ftev_positional(SearchFieldType.TERM,null),
        ftlen_positional(SearchFieldType.RANGE,null),
        ft_secstruct(SearchFieldType.TERM,null),
        ftev_secstruct(SearchFieldType.TERM,null),
        ftlen_secstruct(SearchFieldType.RANGE,null),

        ft_init_met(SearchFieldType.TERM,null),
        ftev_init_met(SearchFieldType.TERM,null),
        ftlen_init_met(SearchFieldType.RANGE,null),
        ft_signal(SearchFieldType.TERM,null),
        ftev_signal(SearchFieldType.TERM,null),
        ftlen_signal(SearchFieldType.RANGE,null),
        ft_propep(SearchFieldType.TERM,null),
        ftev_propep(SearchFieldType.TERM,null),
        ftlen_propep(SearchFieldType.RANGE,null),
        ft_transit(SearchFieldType.TERM,null),
        ftev_transit(SearchFieldType.TERM,null),
        ftlen_transit(SearchFieldType.RANGE,null),
        ft_chain(SearchFieldType.TERM,null),
        ftev_chain(SearchFieldType.TERM,null),
        ftlen_chain(SearchFieldType.RANGE,null),
        ft_peptide(SearchFieldType.TERM,null),
        ftev_peptide(SearchFieldType.TERM,null),
        ftlen_peptide(SearchFieldType.RANGE,null),
        ft_topo_dom(SearchFieldType.TERM,null),
        ftev_topo_dom(SearchFieldType.TERM,null),
        ftlen_topo_dom(SearchFieldType.RANGE,null),
        ft_transmem(SearchFieldType.TERM,null),
        ftev_transmem(SearchFieldType.TERM,null),
        ftlen_transmem(SearchFieldType.RANGE,null),
        ft_domain(SearchFieldType.TERM,null),
        ftev_domain(SearchFieldType.TERM,null),
        ftlen_domain(SearchFieldType.RANGE,null),
        ft_repeat(SearchFieldType.TERM,null),
        ftev_repeat(SearchFieldType.TERM,null),
        ftlen_repeat(SearchFieldType.RANGE,null),
        ft_ca_bind(SearchFieldType.TERM,null),
        ftev_ca_bind(SearchFieldType.TERM,null),
        ftlen_ca_bind(SearchFieldType.RANGE,null),
        ft_zn_fing(SearchFieldType.TERM,null),
        ftev_zn_fing(SearchFieldType.TERM,null),
        ftlen_zn_fing(SearchFieldType.RANGE,null),
        ft_dna_bind(SearchFieldType.TERM,null),
        ftev_dna_bind(SearchFieldType.TERM,null),
        ftlen_dna_bind(SearchFieldType.RANGE,null),
        ft_np_bind(SearchFieldType.TERM,null),
        ftev_np_bind(SearchFieldType.TERM,null),
        ftlen_np_bind(SearchFieldType.RANGE,null),
        ft_region(SearchFieldType.TERM,null),
        ftev_region(SearchFieldType.TERM,null),
        ftlen_region(SearchFieldType.RANGE,null),
        ft_coiled(SearchFieldType.TERM,null),
        ftev_coiled(SearchFieldType.TERM,null),
        ftlen_coiled(SearchFieldType.RANGE,null),
        ft_motif(SearchFieldType.TERM,null),
        ftev_motif(SearchFieldType.TERM,null),
        ftlen_motif(SearchFieldType.RANGE,null),
        ft_compbias(SearchFieldType.TERM,null),
        ftev_compbias(SearchFieldType.TERM,null),
        ftlen_compbias(SearchFieldType.RANGE,null),
        ft_act_site(SearchFieldType.TERM,null),
        ftev_act_site(SearchFieldType.TERM,null),
        ftlen_act_site(SearchFieldType.RANGE,null),
        ft_metal(SearchFieldType.TERM,null),
        ftev_metal(SearchFieldType.TERM,null),
        ftlen_metal(SearchFieldType.RANGE,null),
        ft_binding(SearchFieldType.TERM,null),
        ftev_binding(SearchFieldType.TERM,null),
        ftlen_binding(SearchFieldType.RANGE,null),
        ft_site(SearchFieldType.TERM,null),
        ftev_site(SearchFieldType.TERM,null),
        ftlen_site(SearchFieldType.RANGE,null),
        ft_non_std(SearchFieldType.TERM,null),
        ftev_non_std(SearchFieldType.TERM,null),
        ftlen_non_std(SearchFieldType.RANGE,null),
        ft_mod_res(SearchFieldType.TERM,null),
        ftev_mod_res(SearchFieldType.TERM,null),
        ftlen_mod_res(SearchFieldType.RANGE,null),
        ft_lipid(SearchFieldType.TERM,null),
        ftev_lipid(SearchFieldType.TERM,null),
        ftlen_lipid(SearchFieldType.RANGE,null),
        ft_carbohyd(SearchFieldType.TERM,null),
        ftev_carbohyd(SearchFieldType.TERM,null),
        ftlen_carbohyd(SearchFieldType.RANGE,null),
        ft_disulfid(SearchFieldType.TERM,null),
        ftev_disulfid(SearchFieldType.TERM,null),
        ftlen_disulfid(SearchFieldType.RANGE,null),
        ft_crosslnk(SearchFieldType.TERM,null),
        ftev_crosslnk(SearchFieldType.TERM,null),
        ftlen_crosslnk(SearchFieldType.RANGE,null),
        ft_var_seq(SearchFieldType.TERM,null),
        ftev_var_seq(SearchFieldType.TERM,null),
        ftlen_var_seq(SearchFieldType.RANGE,null),
        ft_variant(SearchFieldType.TERM,null),
        ftev_variant(SearchFieldType.TERM,null),
        ftlen_variant(SearchFieldType.RANGE,null),
        ft_mutagen(SearchFieldType.TERM,null),
        ftev_mutagen(SearchFieldType.TERM,null),
        ftlen_mutagen(SearchFieldType.RANGE,null),
        ft_unsure(SearchFieldType.TERM,null),
        ftev_unsure(SearchFieldType.TERM,null),
        ftlen_unsure(SearchFieldType.RANGE,null),
        ft_conflict(SearchFieldType.TERM,null),
        ftev_conflict(SearchFieldType.TERM,null),
        ftlen_conflict(SearchFieldType.RANGE,null),
        ft_non_cons(SearchFieldType.TERM,null),
        ftev_non_cons(SearchFieldType.TERM,null),
        ftlen_non_cons(SearchFieldType.RANGE,null),
        ft_non_ter(SearchFieldType.TERM,null),
        ftev_non_ter(SearchFieldType.TERM,null),
        ftlen_non_ter(SearchFieldType.RANGE,null),
        ft_helix(SearchFieldType.TERM,null),
        ftev_helix(SearchFieldType.TERM,null),
        ftlen_helix(SearchFieldType.RANGE,null),
        ft_turn(SearchFieldType.TERM,null),
        ftev_turn(SearchFieldType.TERM,null),
        ftlen_turn(SearchFieldType.RANGE,null),
        ft_strand(SearchFieldType.TERM,null),
        ftev_strand(SearchFieldType.TERM,null),
        ftlen_strand(SearchFieldType.RANGE,null),
        ft_intramem(SearchFieldType.TERM,null),
        ftev_intramem(SearchFieldType.TERM,null),
        ftlen_intramem(SearchFieldType.RANGE,null),



        xref(SearchFieldType.TERM,null),    //database cross references
        database(SearchFieldType.TERM,null),
        lit_author(SearchFieldType.TERM,null),  //reference author
        lit_organisation(SearchFieldType.TERM,null), //reference organisation
        lit_title(SearchFieldType.TERM,null), //reference title
        lit_pubmed(SearchFieldType.TERM,null), //reference pubmed id
        lit_journal(SearchFieldType.TERM,null),
        fragment(SearchFieldType.TERM,null),           // indicates whether the protein has non-terminal endings
        existence (SearchFieldType.TERM,null),
        is_isoform(SearchFieldType.TERM,FieldValueValidator::isBooleanValue),
        length(SearchFieldType.RANGE,null),
        mass(SearchFieldType.RANGE,null),
        precursor(SearchFieldType.TERM,null),
        active(SearchFieldType.TERM,FieldValueValidator::isBooleanValue),
        d3structure(SearchFieldType.TERM,FieldValueValidator::isBooleanValue),

        tissue(SearchFieldType.TERM,null),  //rc line
        strain(SearchFieldType.TERM,null), //rc line
        plasmid(SearchFieldType.TERM,null), //rc line
        transposon(SearchFieldType.TERM,null), //rc line
        scope(SearchFieldType.TERM,null),   //rp line


        //subcell location cc
        cc_scl_term(SearchFieldType.TERM,null),
        cc_scl_note(SearchFieldType.TERM,null),
        ccev_scl_term(SearchFieldType.TERM,null),
        ccev_scl_note(SearchFieldType.TERM,null),

        //AP
        cc_ap(SearchFieldType.TERM,null),
        cc_ap_apu(SearchFieldType.TERM,null),
        cc_ap_as(SearchFieldType.TERM,null),
        cc_ap_ai(SearchFieldType.TERM,null),
        cc_ap_rf(SearchFieldType.TERM,null),
        ccev_ap(SearchFieldType.TERM,null),
        ccev_ap_apu(SearchFieldType.TERM,null),
        ccev_ap_as(SearchFieldType.TERM,null),
        ccev_ap_ai(SearchFieldType.TERM,null),
        ccev_ap_rf(SearchFieldType.TERM,null),
        //bpcp
        cc_bpcp(SearchFieldType.TERM,null),
        cc_bpcp_absorption(SearchFieldType.TERM,null),
        cc_bpcp_kinetics(SearchFieldType.TERM,null),
        cc_bpcp_ph_dependence(SearchFieldType.TERM,null),
        cc_bpcp_redox_potential(SearchFieldType.TERM,null),
        cc_bpcp_temp_dependence(SearchFieldType.TERM,null),
        ccev_bpcp(SearchFieldType.TERM,null),
        ccev_bpcp_absorption(SearchFieldType.TERM,null),
        ccev_bpcp_kinetics(SearchFieldType.TERM,null),
        ccev_bpcp_ph_dependence(SearchFieldType.TERM,null),
        ccev_bpcp_redox_potential(SearchFieldType.TERM,null),
        ccev_bpcp_temp_dependence(SearchFieldType.TERM,null),
        //cofactor
        cc_cofactor_chebi(SearchFieldType.TERM,null),
        cc_cofactor_note(SearchFieldType.TERM,null),
        ccev_cofactor_chebi(SearchFieldType.TERM,null),
        ccev_cofactor_note(SearchFieldType.TERM,null),

        //sequence caution
        cc_sc(SearchFieldType.TERM,null),
        cc_sc_framesh(SearchFieldType.TERM,null),
        cc_sc_einit(SearchFieldType.TERM,null),
        cc_sc_eterm(SearchFieldType.TERM,null),
        cc_sc_epred(SearchFieldType.TERM,null),
        cc_sc_etran(SearchFieldType.TERM,null),
        cc_sc_misc(SearchFieldType.TERM,null),

        ccev_sc(SearchFieldType.TERM,null),
        ccev_sc_misc(SearchFieldType.TERM,null),

        cc_function(SearchFieldType.TERM,null),
        ccev_function(SearchFieldType.TERM,null),
        cc_catalytic_activity(SearchFieldType.TERM,null),
        ccev_catalytic_activity(SearchFieldType.TERM,null),
        cc_cofactor(SearchFieldType.TERM,null),
        ccev_cofactor(SearchFieldType.TERM,null),
        cc_enzyme_regulation(SearchFieldType.TERM,null),
        ccev_enzyme_regulation(SearchFieldType.TERM,null),
        cc_biophysicochemical_properties(SearchFieldType.TERM,null),
        ccev_biophysicochemical_properties(SearchFieldType.TERM,null),
        cc_pathway(SearchFieldType.TERM,null),
        ccev_pathway(SearchFieldType.TERM,null),
        cc_subunit(SearchFieldType.TERM,null),
        ccev_subunit(SearchFieldType.TERM,null),
        cc_interaction(SearchFieldType.TERM,null),
        ccev_interaction(SearchFieldType.TERM,null),
        cc_subcellular_location(SearchFieldType.TERM,null),
        ccev_subcellular_location(SearchFieldType.TERM,null),
        cc_alternative_products(SearchFieldType.TERM,null),
        ccev_alternative_products(SearchFieldType.TERM,null),
        cc_tissue_specificity(SearchFieldType.TERM,null),
        ccev_tissue_specificity(SearchFieldType.TERM,null),
        cc_developmental_stage(SearchFieldType.TERM,null),
        ccev_developmental_stage(SearchFieldType.TERM,null),
        cc_induction(SearchFieldType.TERM,null),
        ccev_induction(SearchFieldType.TERM,null),
        cc_domain(SearchFieldType.TERM,null),
        ccev_domain(SearchFieldType.TERM,null),
        cc_ptm(SearchFieldType.TERM,null),
        ccev_ptm(SearchFieldType.TERM,null),
        cc_rna_editing(SearchFieldType.TERM,null),
        ccev_rna_editing(SearchFieldType.TERM,null),
        cc_mass_spectrometry(SearchFieldType.TERM,null),
        ccev_mass_spectrometry(SearchFieldType.TERM,null),
        cc_polymorphism(SearchFieldType.TERM,null),
        ccev_polymorphism(SearchFieldType.TERM,null),
        cc_disease(SearchFieldType.TERM,null),
        ccev_disease(SearchFieldType.TERM,null),
        cc_disruption_phenotype(SearchFieldType.TERM,null),
        ccev_disruption_phenotype(SearchFieldType.TERM,null),
        cc_allergen(SearchFieldType.TERM,null),
        ccev_allergen(SearchFieldType.TERM,null),
        cc_toxic_dose(SearchFieldType.TERM,null),
        ccev_toxic_dose(SearchFieldType.TERM,null),
        cc_biotechnology(SearchFieldType.TERM,null),
        ccev_biotechnology(SearchFieldType.TERM,null),
        cc_pharmaceutical(SearchFieldType.TERM,null),
        ccev_pharmaceutical(SearchFieldType.TERM,null),
        cc_miscellaneous(SearchFieldType.TERM,null),
        ccev_miscellaneous(SearchFieldType.TERM,null),
        cc_similarity(SearchFieldType.TERM,null),
        ccev_similarity(SearchFieldType.TERM,null),
        cc_caution(SearchFieldType.TERM,null),
        ccev_caution(SearchFieldType.TERM,null),
        cc_sequence_caution(SearchFieldType.TERM,null),
        ccev_sequence_caution(SearchFieldType.TERM,null),
        cc_webresource(SearchFieldType.TERM,null),
        ccev_webresource(SearchFieldType.TERM,null),
        cc_page(SearchFieldType.TERM,null),
        ccev_page(SearchFieldType.TERM,null),
        cc_unknown(SearchFieldType.TERM,null),
        ccev_unknown(SearchFieldType.TERM,null),

        interactor(SearchFieldType.TERM,null),
        family(SearchFieldType.TERM,null),
        proteome(SearchFieldType.TERM,FieldValueValidator::isProteomeIdValue),
        proteomecomponent(SearchFieldType.TERM,null),
        annotation_score(SearchFieldType.TERM,null),

        go(SearchFieldType.TERM,null),
        go_ida(SearchFieldType.TERM,null),
        go_imp(SearchFieldType.TERM,null),
        go_igi(SearchFieldType.TERM,null),
        go_ipi(SearchFieldType.TERM,null),
        go_iep(SearchFieldType.TERM,null),
        go_tas(SearchFieldType.TERM,null),
        go_nas(SearchFieldType.TERM,null),
        go_ic(SearchFieldType.TERM,null),
        go_iss(SearchFieldType.TERM,null),
        go_iea(SearchFieldType.TERM,null),
        go_igc(SearchFieldType.TERM,null),
        go_rca(SearchFieldType.TERM,null),
        go_nd(SearchFieldType.TERM,null),
        go_exp(SearchFieldType.TERM,null),
        go_iba(SearchFieldType.TERM,null),
        go_ibd(SearchFieldType.TERM,null),
        go_ikr(SearchFieldType.TERM,null),
        go_ird(SearchFieldType.TERM,null),
        go_iso(SearchFieldType.TERM,null),
        go_isa(SearchFieldType.TERM,null),
        go_ism(SearchFieldType.TERM,null),
        go_hda(SearchFieldType.TERM,null),
        go_hmp(SearchFieldType.TERM,null),
        go_hgi(SearchFieldType.TERM,null),
        go_hep(SearchFieldType.TERM,null),
        go_htp(SearchFieldType.TERM,null),
        go_unknown(SearchFieldType.TERM,null);

        private final Predicate<String> fieldValueValidator;
        private final SearchFieldType searchFieldType;

        Search(SearchFieldType searchFieldType,Predicate<String> fieldValueValidator){
            this.searchFieldType = searchFieldType;
            this.fieldValueValidator  = fieldValueValidator;
        }

        public Predicate<String> getFieldValueValidator(){
            return this.fieldValueValidator;
        }

        public SearchFieldType getSearchFieldType(){
            return this.searchFieldType;
        }

    }

    public enum Return {
        accession

    }
}
