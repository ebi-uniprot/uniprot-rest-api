import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import scala.concurrent.duration._
import com.typesafe.config._

/**
  * Simulates downloading all of Swiss-Prot.
  */
object DownloadSwissProtSimulation {

  val conf = ConfigFactory.load()

  val httpConf = http
    .baseURL(conf.getString("a.s.url"))
    .doNotTrackHeader("1")

  object DownloadScenario {

    def getRequestWithFormat(format: String): ChainBuilder = {
      val httpReqInfo: String = "downloading swissprot"
      val filterGeneralRequestStr: String = "/download?query=reviewed:true"

      val request =
        exec(http(httpReqInfo)
          .get(filterGeneralRequestStr)
          .header("Accept", format)
        )

      return request
    }

    val requestSeq = Seq(
      DownloadScenario.getRequestWithFormat("text/flatfile")
    )

    val instance = scenario("Download Swiss-Prot Scenario")
      .exec(requestSeq)
  }

  class DownloadSwissProtSimulation extends Simulation {
    setUp(
      DownloadScenario.instance.inject(atOnceUsers(conf.getInt("a.s.download.swissprot.users")))
    )
      .protocols(DownloadSimulation.httpConf)
      .maxDuration(conf.getInt("a.s.download.swissprot.maxDuration") minutes)
  }

}
