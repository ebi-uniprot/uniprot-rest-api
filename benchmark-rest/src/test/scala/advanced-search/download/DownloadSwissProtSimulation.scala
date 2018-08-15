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
object DownloadSwissProtSimulation {

  val httpConf = http
    .baseURL(System.getProperty("advanced.search.url")) // Here is the root for all relative URLs
    .doNotTrackHeader("1")

  object DownloadScenario {

    def getRequestWithFormat(format: String): ChainBuilder = {
      val httpReqInfo: String = "downloading swissprot"
      val filterGeneralRequestStr: String = "/searchAll?query=reviewed:true"

      val request =
        exec(http(httpReqInfo)
          .get(filterGeneralRequestStr)
          .header("Accept", format)
        )

      return request
    }

    val requestSeq = Seq(
      DownloadScenario.getRequestWithFormat("application/json")
    )

    val instance = scenario("Download Swiss-Prot Scenario")
      .exec(requestSeq)
  }

  class DownloadSwissProtSimulation extends Simulation {
    setUp(
      DownloadScenario.instance.inject(atOnceUsers(Integer.getInteger("download.swissprot.users", 1)))
    )
      .protocols(DownloadSimulation.httpConf)
      .maxDuration(Integer.getInteger("download.swissprot.maxDuration", 120) minutes)
  }

}
