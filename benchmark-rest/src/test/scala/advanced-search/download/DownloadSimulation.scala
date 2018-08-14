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
object DownloadSimulation {

  val httpConf = http
    .baseURL(System.getProperty("advanced.search.url")) // Here is the root for all relative URLs
    .doNotTrackHeader("1")

  object DownloadScenario {
    val generalSearchFeeder = csv(System.getProperty("advanced.search.general-search.csv")).random
    val organismFeeder = csv(System.getProperty("advanced.search.organism.csv")).random
    val accessionFeeder = csv(System.getProperty("advanced.search.accessions.csv")).random
    val taxonomyFeeder = csv(System.getProperty("advanced.search.taxonomy.csv")).random
    val geneNameFeeder = csv(System.getProperty("advanced.search.gene.csv")).random
    val proteinNameFeeder = csv(System.getProperty("advanced.search.protein.csv")).random

    def getRequestWithFormat(format: String): ChainBuilder = {
      val httpReqInfo: String = "accession=${accession}, format=" + format;
      val filterGeneralRequestStr: String = "/searchCursor?query=accession:${accession}";
      val filterOrganismRequestStr: String = "/searchCursor?query=accession:${accession}";
      val accessionRequestStr: String = "/searchCursor?query=accession:${accession}";
      val filterTaxonomyRequestStr: String = "/searchCursor?query=accession:${accession}";
      val filterGeneRequestStr: String = "/searchCursor?query=accession:${accession}";
      val filterProteinRequestStr: String = "/searchCursor?query=accession:${accession}";

      val request =
        feed(accessionFeeder)
          .pause(5 seconds, 15 seconds)
          .exec(http(httpReqInfo)
            .get(filterGeneralRequestStr)
            .header("Accept", format)
          )

      return request
    }

    val requestSeq = Seq(
      DownloadScenario.getRequestWithFormat("application/json")
    )

    val instance = scenario("ScenarioAccession")
      .forever {
        exec(requestSeq)
      }
  }

  class BasicSimulation extends Simulation {
    setUp(
      DownloadScenario.instance.inject(atOnceUsers(700))
    )
      .protocols(DownloadSimulation.httpConf)
      .assertions(global.responseTime.percentile3.lessThan(500), global.successfulRequests.percent.greaterThan(99))
      .maxDuration(Integer.getInteger("maxDuration", 2) minutes)
  }

}
