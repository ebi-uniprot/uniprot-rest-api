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
    .userAgentHeader("Benchmarker")
    .doNotTrackHeader("1")

  object DownloadScenario {
    val downloadFeeder = csv(conf.getString("a.s.download.swissprot.query.list")).random

    def getRequestWithFormat(): ChainBuilder = {
      val httpReqInfo: String = "url=${download_sp_url}, format=${download_sp_format}"
      val filterGeneralRequestStr: String = "${download_sp_url}"

      val request =
        feed(downloadFeeder)
          .exec(http(httpReqInfo)
            .get(filterGeneralRequestStr)
            .header("Accept", "${download_sp_format}")
          )

      return request
    }

    val requestSeq = Seq(
      DownloadScenario.getRequestWithFormat()
    )

    val instance = scenario("Download Swiss-Prot Scenario")
      .exec(requestSeq)
  }

  class DownloadSwissProtSimulation extends Simulation {
    setUp(
      DownloadScenario.instance.inject(atOnceUsers(conf.getInt("a.s.download.swissprot.users")))
    )
      .protocols(DownloadSwissProtSimulation.httpConf)
      .maxDuration(conf.getInt("a.s.download.swissprot.maxDuration") minutes)
  }

}
