package org.uniprot.api.uniprotkb.service;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.NoContentException;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.repository.search.QueryRetrievalException;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.comment.CommentType;
import org.uniprot.core.uniprotkb.comment.Interactant;
import org.uniprot.core.uniprotkb.comment.Interaction;
import org.uniprot.core.uniprotkb.comment.InteractionComment;
import org.uniprot.core.uniprotkb.interaction.InteractionEntry;
import org.uniprot.core.uniprotkb.interaction.InteractionMatrixItem;
import org.uniprot.core.uniprotkb.interaction.impl.InteractionEntryBuilder;
import org.uniprot.core.uniprotkb.interaction.impl.InteractionMatrixItemBuilder;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.config.returnfield.model.ReturnField;

/**
 * Responsible for fetching interaction entry data from the UniProtKB repository layer.
 *
 * <p>Created 06/05/2020
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

    public InteractionEntry getEntryInteractions(String accession) {
        return new InteractionEntryBuilder()
                .interactionsSet(getInteractionsForAccession(accession))
                .build();
    }

    public List<InteractionMatrixItem> getInteractionsForAccession(String accession) {
        List<ReturnField> filters =
                asList(
                        returnFieldConfig.getReturnFieldByName("cc_interaction"),
                        returnFieldConfig.getReturnFieldByName("cc_subunit"));

        UniProtKBEntry entry = getEntry(accession, filters);

        List<String> interactionAccessions =
                entry.getCommentsByType(CommentType.INTERACTION).stream()
                        .flatMap(
                                interComment ->
                                        getUniProtKBAccessionsForInteraction(
                                                (InteractionComment) interComment)
                                                .stream())
                        .distinct()
                        .collect(toList());

        if (Utils.notNullNotEmpty(interactionAccessions)) {
            List<InteractionMatrixItem> interEntries = new ArrayList<>();

            // add interactions for entry
            interEntries.add(entry2Interaction(entry));

            // add interactions for every interaction entry
            try {
                interactionAccessions.stream()
                        .map(interactionAccession -> getEntry(interactionAccession, filters))
                        .map(this::entry2Interaction)
                        .forEach(interEntries::add);
            } catch (ResourceNotFoundException exception) {
                throw new QueryRetrievalException(
                        "Could not fetch entries associated with interactions of "
                                + entry.getPrimaryAccession().getValue()
                                + ".");
            }
            return interEntries;
        } else {
            throw new NoContentException(
                    "No interactions for " + entry.getPrimaryAccession().getValue());
        }
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

    private InteractionMatrixItem entry2Interaction(UniProtKBEntry entry) {
        return new InteractionMatrixItemBuilder()
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

    private List<String> getUniProtKBAccessionsForInteraction(
            InteractionComment interactionComment) {
        return interactionComment.getInteractions().stream()
                .map(this::getUniProtKBAccessionForInteraction)
                .filter(Objects::nonNull)
                .distinct()
                .collect(toList());
    }

    private String getUniProtKBAccessionForInteraction(Interaction interaction) {
        Interactant interactantTwo = interaction.getInteractantTwo();
        if (interactantTwo.hasUniProtKBAccession()) {
            return interactantTwo.getUniProtKBAccession().getValue();
        }

        return null;
    }
}
