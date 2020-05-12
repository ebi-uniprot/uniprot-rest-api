package org.uniprot.api.uniprotkb.service;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.uniprotkb.model.UniProtKBEntryInteraction;
import org.uniprot.api.uniprotkb.model.UniProtKBEntryInteractions;
import org.uniprot.api.uniprotkb.model.converter.CommentConverter;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import org.uniprot.core.interaction.InteractionEntry;
import org.uniprot.core.interaction.InteractionMatrix;
import org.uniprot.core.interaction.impl.InteractionEntryBuilder;
import org.uniprot.core.interaction.impl.InteractionMatrixBuilder;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.comment.*;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.config.returnfield.model.ReturnField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

/**
 * Created 06/05/2020
 *
 * @author Edd
 */
@Service
public class UniProtKBEntryInteractionService {
    private static final String ACCESSION = "accession_id";
    private final UniprotQueryRepository repository;
    private final UniProtEntryQueryResultsConverter resultsConverter;
    private final ReturnFieldConfig returnFieldConfig;

    public UniProtKBEntryInteractionService(
            UniprotQueryRepository repository, UniProtEntryQueryResultsConverter resultsConverter) {
        this.repository = repository;
        this.resultsConverter = resultsConverter;
        this.returnFieldConfig =
                ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPROTKB);
    }

    private UniProtKBEntry getEntry(String accession, List<ReturnField> fields) {
        SolrRequest solrRequest =
                SolrRequest.builder()
                        .query(ACCESSION + ":" + accession.toUpperCase())
                        .rows(NumberUtils.INTEGER_ONE)
                        .build();

        return repository
                .getEntry(solrRequest)
                .map(
                        doc ->
                                resultsConverter
                                        .convertDoc(doc, fields)
                                        .orElseThrow(
                                                () ->
                                                        new ResourceNotFoundException(
                                                                "{search.not.found}")))
                .orElseThrow(() -> new ResourceNotFoundException("{search.not.found}"));
    }

    public InteractionEntry getEntryInteractions(String accession) {
        return new InteractionEntryBuilder()
                .interactionsSet(getInteractionsForAccession(accession))
                .build();
    }

    public List<InteractionMatrix> getInteractionsForAccession(String accession) {
        List<ReturnField> filters =
                asList(
                        returnFieldConfig.getReturnFieldByName("cc_interaction"),
                        returnFieldConfig.getReturnFieldByName("cc_subunit"));

        UniProtKBEntry entry = getEntry(accession, filters);

        List<String> interactionAccessions =
                entry.getCommentsByType(CommentType.INTERACTION).stream()
                        .flatMap(
                                interComment ->
                                        getInteractionAccessions((InteractionComment) interComment)
                                                .stream())
                        .distinct()
                        .collect(toList());

        if (Utils.notNullNotEmpty(interactionAccessions)) {
            List<InteractionMatrix> interEntries = new ArrayList<>();

            // add interactions for entry
            interEntries.add(entry2Interaction(entry));

            // add interactions for every interaction entry
            interactionAccessions.stream()
                    .map(interactionAccession -> getEntry(interactionAccession, filters))
                    .map(this::entry2Interaction)
                    .forEach(interEntries::add);

            return interEntries;
        } else {
            // throw no content exception 204
            throw new IllegalStateException("wrong");
        }
    }

    private InteractionMatrix entry2Interaction(UniProtKBEntry entry) {
        return new InteractionMatrixBuilder()
                .uniProtKBAccession(entry.getPrimaryAccession())
                .uniProtKBId(entry.getUniProtkbId())
                .organism(entry.getOrganism())
                .diseasesSet(entry.getCommentsByType(CommentType.DISEASE))
                .subcellularLocationsSet(entry.getCommentsByType(CommentType.SUBCELLULAR_LOCATION))
                .interactionsSet(convertInteractions(entry.getCommentsByType(CommentType.INTERACTION)))
                .proteinExistence(entry.getProteinExistence())
//                .interactions(
//                        entry.getCommentsByType(CommentType.INTERACTION).stream()
//                                .map(comment -> (InteractionComment) comment)
//                                .map(this::convertInteractionComment)
//                                .filter(Objects::nonNull)
//                                .flatMap(iaComment -> iaComment.getInteractions().stream())
//                                .map(val -> updateIntActAddAccession(val, accession))
//                                .collect(toList()))
//                .diseases(
//                        entry.getCommentsByType(CommentType.DISEASE).stream()
//                                .map(comment -> (DiseaseComment) comment)
//                                .map(CommentConverter::convertDiseaseComment)
//                                .filter(Objects::nonNull)
//                                .collect(Collectors.toList()))
//                .subcellularLocations(
//                        entry.getCommentsByType(CommentType.SUBCELLULAR_LOCATION).stream()
//                                .map(comment -> (SubcellularLocationComment) comment)
//                                .map(CommentConverter::convertSubcellComment)
//                                .filter(Objects::nonNull)
//                                .collect(Collectors.toList()))
                .build();
    }

    private List<Interaction> convertInteractions(List<InteractionComment> interactionComment) {
        return interactionComment.stream()
                .map(InteractionComment::getInteractions)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private org.uniprot.api.uniprotkb.model.Interaction.IntActComment updateIntActAddAccession(
            org.uniprot.api.uniprotkb.model.Interaction.IntActComment intAct, String accession) {
        if (Utils.notNullNotEmpty(intAct.getAccession1())) {
            return intAct;
        } else {
            return org.uniprot.api.uniprotkb.model.Interaction.IntActComment.builder()
                    .accession1(accession)
                    .chain1(intAct.getChain1())
                    .accession2(intAct.getAccession2())
                    .chain2(intAct.getChain2())
                    .gene(intAct.getGene())
                    .interactor1(intAct.getInteractor1())
                    .interactor2(intAct.getInteractor2())
                    .experiments(intAct.getExperiments())
                    .organismDiffer(intAct.isOrganismDiffer())
                    .build();
        }
    }

    private org.uniprot.api.uniprotkb.model.Interaction convertInteractionComment(
            InteractionComment iac) {
        return org.uniprot.api.uniprotkb.model.Interaction.builder()
                .interactions(
                        iac.getInteractions().stream()
                                .map(this::convertInteraction)
                                .collect(toList()))
                .build();
    }

    private org.uniprot.api.uniprotkb.model.Interaction.IntActComment convertInteraction(
            Interaction interaction) {
        org.uniprot.api.uniprotkb.model.Interaction.IntActComment.IntActCommentBuilder builder =
                org.uniprot.api.uniprotkb.model.Interaction.IntActComment.builder();

        //        String accession1 = interaction.getFirstInteractant().getValue();
        String accession1 = interaction.getInteractantOne().getUniProtKBAccession().getValue();
        //        String chain1=null;
        String chain1 = null;
        //        if(!AccessionResolver.isUniprotAccession(accession1)) {
        //            chain1 =accession1;
        //            accession1= null;
        //        }
        // if(isUniProtKBAccession(accession1)) {
        //   chain1 = accession1;
        //  accession1 = null;
        // }
        //        String accession2=null;
        //        String chain2= null;
        String accession2 = null;
        String chain2 = null;
        //        if((interaction.getSecondInteractantParent() !=null) &&
        //
        // !Strings.isNullOrEmpty(interaction.getSecondInteractantParent().getValue())){
        //            accession2= interaction.getSecondInteractantParent().getValue();
        //            chain2 = interaction.getSecondInteractant().getValue();
        //        }else {
        //            accession2 =interaction.getSecondInteractant().getValue();
        //        }
        if (interaction.hasInteractantTwo()
                && interaction.getInteractantTwo().hasUniProtKBAccession()) {
            accession2 = interaction.getInteractantTwo().getUniProtKBAccession().getValue();
            chain2 = interaction.getInteractantTwo().getChainId();
        }

        return org.uniprot.api.uniprotkb.model.Interaction.IntActComment.builder()
                .accession1(interaction.getInteractantOne().getUniProtKBAccession().getValue())
                .interactor1(interaction.getInteractantOne().getGeneName())
                .build();

        //        return  new IntActComment.IntAct(
        //                accession1,
        //                chain1,
        //                accession2,
        //                chain2,
        //                interaction.getInteractionGeneName().getValue(),
        //                interaction.getFirstInteractor().getValue(),
        //                interaction.getSecondInteractor().getValue(),
        //                interaction.getNumberOfExperiments(),
        //                interaction.getInteractionType().equals(InteractionType.XENO));
    }

    private List<String> getInteractionAccessions(InteractionComment interactionComment) {
        return interactionComment.getInteractions().stream()
                .map(this::getInteractionAccession)
                .collect(toList());
    }

    private String getInteractionAccession(Interaction interaction) {
        Interactant interactantTwo = interaction.getInteractantTwo();
        if (interactantTwo.hasUniProtKBAccession()) {
            return interactantTwo.getUniProtKBAccession().getValue();
        }

        return interactantTwo.getIntActId();
    }
}
