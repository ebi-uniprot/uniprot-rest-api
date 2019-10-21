package org.uniprot.api.rest.service;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RDFService {
  private static final int BATCH_SIZE = 3;
  private static final String QUERY_STR = "query";
  private static final String FORMAT_STR = "format";
  private static final String RDF_STR = "rdf";
  private static final String ID_COLON_STR = "id:";
  private static final String OR_DELIMITER_STR = " or ";

  private static final String URI = "https://www.uniprot.org/uniprot/?";
  public Model getEntriesByAccessions(List<String> accessions) throws IOException {
    // split the list of accessions in batch of size BATCH_SIZE
    List<List<String>> lolAccession = IntStream
            .range(0, accessions.size())
            .filter(i -> i % BATCH_SIZE == 0)
            .mapToObj(nb -> accessions.subList(nb, Math.min(nb + BATCH_SIZE, accessions.size())))
            .collect(Collectors.toList());

    Model result = null;
    for(List<String> lAccession : lolAccession){ // get the RDF for each batch and then union it
      // create query like id:P12345 or id:P54321 or ....
      String idQuery = lAccession
              .stream()
              .map(acc -> new StringBuilder(ID_COLON_STR).append(acc))
              .collect(Collectors.joining(OR_DELIMITER_STR));

      UriComponents uriBuilder =
              UriComponentsBuilder.fromHttpUrl(URI)
                      .queryParam(QUERY_STR, idQuery)
                      .queryParam(FORMAT_STR, RDF_STR)
                      .build();

      RestTemplate restTemplate = new RestTemplate(); // FIXME move this creation to @Configuration class
      String rdfStr = restTemplate.getForObject(uriBuilder.toString(), String.class);
      Model model = ModelFactory.createDefaultModel();
      model.read(new ByteArrayInputStream(rdfStr.getBytes()), null);
      if(result == null){
        result = model;
      } else {
        result = result.union(model);
      }
    }

    result.write(new FileWriter("chunk.rdf"), "N-TRIPLE");

    return result;
  }

  public static void main(String[] args) throws IOException {
    new RDFService().getEntriesByAccessions(Arrays.asList("P05067", "P00750", "P12345"));
  }
}
