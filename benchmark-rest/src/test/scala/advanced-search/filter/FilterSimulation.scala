import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import scala.concurrent.duration._

// Variables used in this test have been externalised to system properties, so that we do not commit
// large files to this git repo. Please set the following properties which can be passed to maven as -DvariableName=value
//  advanced.search.url : the base url of the server under test
//  advanced.search.accessions.csv : the file containing the accessions used in this test
//  maxDuration : the maximum duration of the stress test
object FilterSimulation {

  val httpConf = http
    .baseURL(System.getProperty("advanced.search.url")) // Here is the root for all relative URLs
    .doNotTrackHeader("1")

  object FilterScenario {
    val generalSearchFeeder = csv(System.getProperty("advanced.search.general-search.csv")).random
    val organismFeeder = csv(System.getProperty("advanced.search.organism.csv")).random
    val accessionFeeder = csv(System.getProperty("advanced.search.accessions.csv")).random
    val taxonomyFeeder = csv(System.getProperty("advanced.search.taxonomy.csv")).random
    val geneNameFeeder = csv(System.getProperty("advanced.search.gene.csv")).random
    val proteinNameFeeder = csv(System.getProperty("advanced.search.protein.csv")).random

    def getRequestWithFormat(format: String): ChainBuilder = {
      val filterGeneralRequestStr: String = "/searchCursor?query=content:${content}";
      val filterOrganismRequestStr: String = "/searchCursor?query=tax_name_lineage:${organism}";
      val accessionRequestStr: String = "/searchCursor?query=accession:${accession}";
      val filterTaxonomyRequestStr: String = "/searchCursor?query=tax_id_lineage:${taxon}";
      val filterGeneRequestStr: String = "/searchCursor?query=gene:${gene}";
      val filterProteinRequestStr: String = "/searchCursor?query=protein_name:${protein}";

      val request =
        feed(accessionFeeder)
          .feed(organismFeeder)
          .feed(generalSearchFeeder)
          .feed(taxonomyFeeder)
          .feed(geneNameFeeder)
          .feed(proteinNameFeeder)
          .pause(5 seconds, 15 seconds)
          .exec(http("content field")
            .get(filterGeneralRequestStr)
            .header("Accept", format))
          .pause(2 seconds, 5 seconds)
          .exec(http("tax_name_lineage field")
            .get(filterOrganismRequestStr)
            .header("Accept", format))
          .pause(2 seconds, 10 seconds)
          .exec(http("accession field")
            .get(accessionRequestStr)
            .header("Accept", format))
          .pause(2 seconds, 5 seconds)
          .exec(http("tax_id_lineage field")
            .get(filterTaxonomyRequestStr)
            .header("Accept", format))
          .pause(2 seconds, 5 seconds)
          .exec(http("gene field")
            .get(filterGeneRequestStr)
            .header("Accept", format))
          .pause(2 seconds, 5 seconds)
          .exec(http("protein_name field")
            .get(filterProteinRequestStr)
            .header("Accept", format))

      return request
    }

    val requestSeq = Seq(
      FilterScenario.getRequestWithFormat("application/json")
    )

    val instance = scenario("Multiple Filter Request Scenario")
      .forever {
        exec(requestSeq)
      }
  }

  class BasicSimulation extends Simulation {
    setUp(
      FilterScenario.instance.inject(atOnceUsers(700))
    )
      .protocols(FilterSimulation.httpConf)
      .assertions(global.responseTime.percentile3.lessThan(500), global.successfulRequests.percent.greaterThan(99))
      .maxDuration(Integer.getInteger("maxDuration", 2) minutes)
  }

}
