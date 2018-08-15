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
object AccessionRetrievalSimulation {

  val httpConf = http
    .baseURL(System.getProperty("advanced.search.url")) // Here is the root for all relative URLs
    .doNotTrackHeader("1")

  object AccessionScenario {
    val feeder = tsv(System.getProperty("advanced.search.accessions.list")).random

    def getRequestWithFormat(format: String): ChainBuilder = {
      val httpReqInfo: String = "accession=${accession}, format=" + format;
      val requestStr: String = "/searchCursor?query=accession:${accession}";

      val request =
        feed(feeder)
          .pause(5 seconds, 15 seconds)
          .exec(http(httpReqInfo)
            .get(requestStr)
            .header("Accept", format)
          )

      return request
    }

    val requestSeq = Seq(
      AccessionScenario.getRequestWithFormat("application/json")
    )

    val instance = scenario("Accession Retrieval Scenario")
      .forever {
        exec(requestSeq)
      }
  }

  class AccessionRetrievalSimulation extends Simulation {
    setUp(
      AccessionScenario.instance.inject(atOnceUsers(Integer.getInteger("accession.retrieval.users", 700)))
    )
      .protocols(AccessionRetrievalSimulation.httpConf)
//      .assertions(global.responseTime.percentile3.lte(500), global.successfulRequests.percent.gte(99))
      .maxDuration(Integer.getInteger("accession.retrieval.maxDuration", 2) minutes)
  }
}
