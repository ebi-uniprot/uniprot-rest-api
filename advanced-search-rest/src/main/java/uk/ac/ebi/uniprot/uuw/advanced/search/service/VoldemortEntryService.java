package uk.ac.ebi.uniprot.uuw.advanced.search.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.voldemort.client.UniProtClient;
@Service
public class VoldemortEntryService {
	private final UniProtClient uniprotClient;
	
	public VoldemortEntryService(UniProtClient uniprotClient) {
		this.uniprotClient = uniprotClient;
	}
	public Optional<UniProtEntry> getEntry(String accession) {
		return this.uniprotClient.getEntry(accession);
	}
	
	public Map<String, UniProtEntry> getEntryMap(Iterable<String> accessions){
		return uniprotClient.getEntryMap(accessions);
	}
	
	public List<UniProtEntry> getEntries(Iterable<String> accessions){
		return uniprotClient.getEntries(accessions);
	}
}
