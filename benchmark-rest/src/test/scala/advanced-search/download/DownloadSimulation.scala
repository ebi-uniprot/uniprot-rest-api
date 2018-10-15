import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import scala.concurrent.duration._
import com.typesafe.config._

/**
  * Simulates downloading files.
  */
object DownloadSimulation {

  val conf = ConfigFactory.load()

  val httpConf = http
    .baseURL(conf.getString("a.s.url"))
    .doNotTrackHeader("1")

  object DownloadScenario {
    val downloadFeeder = tsv(conf.getString("a.s.download.query.list")).random

    def getRequestWithFormat(): ChainBuilder = {
      val httpReqInfo: String = "url=${download_url}, format=${download_format}"
      val queryRequestStr: String = "${download_url}"

      val request =
        feed(downloadFeeder)
          .pause(5 seconds, 15 seconds)
          .exec(http(httpReqInfo)
            .get(queryRequestStr)
            .header("Accept", "${download_format}")
          )

      return request
    }

    val requestSeq = Seq(
      DownloadScenario.getRequestWithFormat()
    )

    val instance = scenario("Download Scenario")
      .exec(requestSeq)
  }

  class DownloadSimulation extends Simulation {
    setUp(
      DownloadScenario.instance.inject(atOnceUsers(conf.getInt("a.s.download.users")))
    )
      .protocols(DownloadSimulation.httpConf)
//      .assertions(global.responseTime.percentile3.lte(500), global.successfulRequests.percent.gte(99))
      .maxDuration(conf.getInt("a.s.download.maxDuration") minutes)
  }

}
