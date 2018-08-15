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
object DownloadSimulation {

  val httpConf = http
    .baseURL(System.getProperty("advanced.search.url")) // Here is the root for all relative URLs
    .doNotTrackHeader("1")

  object DownloadScenario {
    val downloadFeeder = tsv(System.getProperty("advanced.search.download-query.list")).random

    def getRequestWithFormat(format: String): ChainBuilder = {
      val httpReqInfo: String = "download: ${query}"
      val queryRequestStr: String = "/searchAll?query=${query}"

      val request =
        feed(downloadFeeder)
          .pause(5 seconds, 15 seconds)
          .exec(http(httpReqInfo)
            .get(queryRequestStr)
            .header("Accept", format)
          )

      return request
    }

    val requestSeq = Seq(
      DownloadScenario.getRequestWithFormat("application/json")
    )

    val instance = scenario("Download Scenario")
      .exec(requestSeq)
  }

  class DownloadSimulation extends Simulation {
    setUp(
      DownloadScenario.instance.inject(atOnceUsers(Integer.getInteger("download.users", 10)))
    )
      .protocols(DownloadSimulation.httpConf)
//      .assertions(global.responseTime.percentile3.lte(500), global.successfulRequests.percent.gte(99))
      .maxDuration(Integer.getInteger("maxDuration", 60) minutes)
  }

}
