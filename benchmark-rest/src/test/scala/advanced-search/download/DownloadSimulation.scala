import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import scala.concurrent.duration._
import com.typesafe.config._

/**
  * Simulates downloading files.
  */
class DownloadSimulation extends Simulation {

  val conf = ConfigFactory.load()

  val httpConf = http
    .userAgentHeader("Benchmarker")
    .doNotTrackHeader("1")

  val downloadFeeder = separatedValues(conf.getString("a.s.download.query.list"), '#').random

  def getRequestWithFormat(): ChainBuilder = {
    val httpReqInfo: String = "url=${download_url}, format=${download_format}, encoding=${download_encoding}"
    val queryRequestStr: String = "${download_url}"

    val request =
      feed(downloadFeeder)
        .pause(5 seconds, 15 seconds)
        .exec(http(httpReqInfo)
          .get(queryRequestStr)
          .header("Accept", "${download_format}")
          .header("Accept-Encoding", "${download_encoding}")
        )

    return request
  }

  val requestSeq = Seq(
    getRequestWithFormat()
  )

  val instance = scenario("Download Scenario")
    .exec(requestSeq)

  setUp(
    instance.inject(atOnceUsers(conf.getInt("a.s.download.users")))
  )
    .protocols(httpConf)
    //      .assertions(global.responseTime.percentile3.lte(500), global.successfulRequests.percent.gte(99))
    .maxDuration(conf.getInt("a.s.download.maxDuration") minutes)

}
