package uk.ac.ebi.uniprot.api.proteome.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.stereotype.Service;

import uk.ac.ebi.uniprot.api.common.exception.ResourceNotFoundException;
import uk.ac.ebi.uniprot.api.common.exception.ServiceException;
import uk.ac.ebi.uniprot.api.proteome.repository.ProteomeQueryRepository;
import uk.ac.ebi.uniprot.domain.proteome.CanonicalProtein;
import uk.ac.ebi.uniprot.domain.proteome.ProteomeEntry;
import uk.ac.ebi.uniprot.search.document.proteome.ProteomeDocument;
import uk.ac.ebi.uniprot.search.field.ProteomeField;

/**
 *
 * @author jluo
 * @date: 30 Apr 2019
 *
 */
@Service
public class GeneCentricService {
	private ProteomeQueryRepository repository;
	private final ProteomeEntryConverter proteomeConverter;
	 @Autowired
	public GeneCentricService(ProteomeQueryRepository repository) {
		this.repository = repository;
		this.proteomeConverter = new ProteomeEntryConverter(false);
	}
	
	public List<CanonicalProtein> getByUpId(String upid) {
		SimpleQuery simpleQuery = new SimpleQuery(
				Criteria.where(ProteomeField.Search.upid.name()).is(upid.toUpperCase()));
		try {
		Optional<ProteomeDocument> optionalDoc = repository.getEntry(simpleQuery);
		if(optionalDoc.isPresent()) {
			ProteomeEntry entry = proteomeConverter.apply(optionalDoc.get());
			if(entry ==null) {
				 String message = "Could not convert Proteome entry from document for: [" + upid + "]";
				 throw new ServiceException(message);
			}
			else
				return entry.getCanonicalProteins();
		}else {
			throw new ResourceNotFoundException("{search.not.found}");
		}
		
		}catch (ResourceNotFoundException e) {
			throw e;
		} catch (Exception e) {
            String message = "Could not get upid for: [" + upid + "]";
            throw new ServiceException(message, e);
        }
	}

	public CanonicalProtein getByAccession(String accession) {
		SimpleQuery simpleQuery = new SimpleQuery(Criteria.where(ProteomeField.Search.accession.name()).is(accession));
		try {
			Optional<ProteomeDocument> optionalDoc = repository.getEntry(simpleQuery);
			if (optionalDoc.isPresent()) {

				ProteomeEntry entry = proteomeConverter.apply(optionalDoc.get());
				Optional<CanonicalProtein> opProtein = fromProteomeEntry(accession, entry);
				if (opProtein.isPresent()) {
					return opProtein.get();

				} else {
					throw new ResourceNotFoundException("{search.not.found}");
				}

			} else {
				throw new ResourceNotFoundException("{search.not.found}");
			}

		} catch (ResourceNotFoundException e) {
			throw e;
		} catch (Exception e) {
			String message = "Could not fetch proteome";
			throw new ServiceException(message, e);
		}
	}

	private Optional<CanonicalProtein> fromProteomeEntry(String accession, ProteomeEntry entry) {
		if (entry == null) {
			throw new ResourceNotFoundException("{search.not.found}");
		}
		return entry.getCanonicalProteins().stream().filter(cp -> hasProtein(cp, accession)).findFirst();
	}

	private boolean hasProtein(CanonicalProtein cp, String accession) {
		if (cp.getCanonicalProtein().getAccession().getValue().equalsIgnoreCase(accession))
			return true;
		return cp.getRelatedProteins().stream()
				.anyMatch(val -> val.getAccession().getValue().equalsIgnoreCase(accession));
	}
}
