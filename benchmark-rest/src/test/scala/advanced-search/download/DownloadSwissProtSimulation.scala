import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._

import scala.concurrent.duration._
import com.typesafe.config._

/**
  * Simulates downloading all of Swiss-Prot.
  */
class DownloadSwissProtSimulation extends Simulation {

  val conf = ConfigFactory.load()

  val httpConf = http
    .userAgentHeader("Benchmarker")
    .doNotTrackHeader("1")

  val host = conf.getString("a.s.host")
  val downloadFeeder = separatedValues(conf.getString("a.s.download.swissprot.query.list"), '#').random

  def getRequestWithFormat(): ChainBuilder = {
    val httpReqInfo: String = "download swissprot, format=${download_sp_format}"
    val filterGeneralRequestStr: String = host + "${download_sp_url}"

    val request =
      feed(downloadFeeder)
        .exec(http(httpReqInfo)
          .get(filterGeneralRequestStr)
          .header("Accept", "${download_sp_format}")
        )

    return request
  }

  val requestSeq = Seq(
    getRequestWithFormat()
  )

  val instance = scenario("Download Swiss-Prot Scenario")
    .exec(requestSeq)

  setUp(
    instance.inject(atOnceUsers(conf.getInt("a.s.download.swissprot.users")))
  )
    .protocols(httpConf)
    .maxDuration(conf.getInt("a.s.download.swissprot.maxDuration") minutes)
}
