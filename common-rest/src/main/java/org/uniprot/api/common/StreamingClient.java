package org.uniprot.api.common;

import java.io.IOException;

import org.apache.solr.client.solrj.io.SolrClientCache;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.CloudSolrStream;
import org.apache.solr.client.solrj.io.stream.StreamContext;
import org.apache.solr.common.params.ModifiableSolrParams;

public class StreamingClient {
    public static void main(String[] args) throws IOException {
        String zkHost =
                "wp-np2-b3.ebi.ac.uk:2191,wp-np2-b4.ebi.ac.uk:2191,wp-np2-b5.ebi.ac.uk:2191";
        String collection = "uniprot";

        StreamContext streamContext = new StreamContext();
        SolrClientCache solrClientCache = new SolrClientCache();
        streamContext.setSolrClientCache(solrClientCache);

        ModifiableSolrParams props = new ModifiableSolrParams();
        props.add("q", "accession_id:P12345");
        props.add("qt", "/export");
        props.add("sort", "accession_id asc");
        props.add("fl", "accession_id,organism_id");

        CloudSolrStream cstream = new CloudSolrStream(zkHost, collection, props);
        cstream.setStreamContext(streamContext);
        int count = 0;
        long startTime = System.currentTimeMillis();
        try {
            cstream.open();
            while (true) {
                Tuple tuple = cstream.read();
                if (tuple.EOF) {
                    break;
                }
                count++;
                String fieldA = tuple.getString("accession_id");
                String fieldC = tuple.getString("organism_id");
                System.out.println(fieldA + ", " + fieldC);
            }

        } finally {
            cstream.close();
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Total time taken in seconds : " + (endTime - startTime) / 1000);
        System.out.println("Total record found : " + count);
    }
}
