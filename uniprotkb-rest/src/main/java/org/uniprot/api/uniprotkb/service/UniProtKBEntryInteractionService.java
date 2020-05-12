package org.uniprot.api.uniprotkb.service;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.comment.CommentType;
import org.uniprot.core.uniprotkb.comment.Interactant;
import org.uniprot.core.uniprotkb.comment.Interaction;
import org.uniprot.core.uniprotkb.comment.InteractionComment;
import org.uniprot.core.uniprotkb.interaction.InteractionEntry;
import org.uniprot.core.uniprotkb.interaction.InteractionMatrix;
import org.uniprot.core.uniprotkb.interaction.impl.InteractionEntryBuilder;
import org.uniprot.core.uniprotkb.interaction.impl.InteractionMatrixBuilder;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.config.returnfield.model.ReturnField;

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
                .proteinExistence(entry.getProteinExistence())
                .organism(entry.getOrganism())
                .diseasesSet(entry.getCommentsByType(CommentType.DISEASE))
                .subcellularLocationsSet(entry.getCommentsByType(CommentType.SUBCELLULAR_LOCATION))
                .interactionsSet(
                        convertInteractions(entry.getCommentsByType(CommentType.INTERACTION)))
                .build();
    }

    private List<Interaction> convertInteractions(List<InteractionComment> interactionComment) {
        return interactionComment.stream()
                .map(InteractionComment::getInteractions)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private List<String> getInteractionAccessions(InteractionComment interactionComment) {
        return interactionComment.getInteractions().stream()
                .map(this::getInteractionAccession)
                .distinct()
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
