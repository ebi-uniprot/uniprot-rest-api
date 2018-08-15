import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import scala.concurrent.duration._

// Variables used in this test have been externalised to system properties, so that we do not commit
// large files to this git repo. Please set the following properties which can be passed to maven as -DvariableName=value
//  advanced.search.url : the base url of the server under test
//  advanced.search.accessions.list : the file containing the accessions used in this test
//  maxDuration : the maximum duration of the stress test
object FilterSimulation {

  val httpConf = http
    .baseURL(System.getProperty("advanced.search.url")) // Here is the root for all relative URLs
    .doNotTrackHeader("1")

  object FilterScenario {
    // TODO: use a single property to specify the directory in which the files must be located 
    val generalSearchFeeder = tsv(System.getProperty("advanced.search.general-search.list")).random
    val organismFeeder = tsv(System.getProperty("advanced.search.organism.list")).random
    val accessionFeeder = tsv(System.getProperty("advanced.search.accessions.list")).random
    val taxonomyFeeder = tsv(System.getProperty("advanced.search.taxonomy.list")).random
    val geneNameFeeder = tsv(System.getProperty("advanced.search.gene.list")).random
    val proteinNameFeeder = tsv(System.getProperty("advanced.search.protein.list")).random
//    val featureFeeder = tsv(System.getProperty("advanced.search.feature.list")).random

    def getRequestWithFormat(format: String): ChainBuilder = {
      val filterGeneralRequestStr: String = "/searchCursor?query=content:${content}"
      val filterOrganismRequestStr: String = "/searchCursor?query=tax_name_lineage:${organism}"
      val accessionRequestStr: String = "/searchCursor?query=accession:${accession}"
      val filterTaxonomyRequestStr: String = "/searchCursor?query=tax_id_lineage:${taxon}"
      val filterGeneRequestStr: String = "/searchCursor?query=gene:${gene}"
      val filterProteinRequestStr: String = "/searchCursor?query=protein_name:${protein}"
//      val filterFeatureRequestStr: String = "/searchCursor?query=ft_molecule_processing:${feature}"

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
//          .exec(http("feature molecule processing field")
//            .get(filterFeatureRequestStr)
//            .header("Accept", format))
//          .pause(2 seconds, 5 seconds)
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

  class FilterSimulation extends Simulation {
    setUp(
      FilterScenario.instance.inject(atOnceUsers(Integer.getInteger("multi.filter.users", 700)))
    )
      .protocols(FilterSimulation.httpConf)
//      .assertions(global.responseTime.percentile3.lte(500), global.successfulRequests.percent.gte(99))
      .maxDuration(Integer.getInteger("multi.filter.maxDuration", 2) minutes)
  }

}
