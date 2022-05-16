import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import scala.concurrent.duration._
import com.typesafe.config._

/**
 * Simulates simple retrieval of URLs from the REST service. Since these URLs can be anywhere, this simulation
 * can be used to test anything.
 */
class URLRetrievalSimulation extends Simulation {

  val conf = ConfigFactory.load()

  val httpConf = http
    .userAgentHeader("Benchmarker")
    .doNotTrackHeader("1")

  val host = conf.getString("a.s.host")
  val feeder = separatedValues(conf.getString("a.s.url.retrieval.list"), '#').random

  def getRequest(): ChainBuilder = {
    val url2: String = "${url}"
    val urlRegex = raw"([A-Za-z0-9_\-\/]+)[\?\/].*".r
    val basicRequest: String = url2 match {
      case urlRegex(baseUrl) => "url=" + baseUrl
      case _ => url2
    }
    val format = "format=${format}"
    val httpReqInfo: String = basicRequest + ", " + format;
    val requestStr: String = host + "${url}";

    val request =
      feed(feeder)
        //.pause(5 seconds, 15 seconds)
        .exec(http(format)
          .get(requestStr)
          .header("Accept", "${format}")
        )

    return request
  }

  val requestSeq = Seq(
    getRequest()
  )

  val instance = scenario("URL Retrieval Scenario")
    .forever {
      exec(requestSeq)
    }

  setUp(
    instance.inject(
      atOnceUsers(conf.getInt("a.s.url.retrieval.users")).throttle(
        // ------------- CYCLE 1 -----------------
        // constant requests/sec
        reachRps(conf.getInt("a.s.url.retrieval.constantRPS")) in (10 seconds),
        holdFor(conf.getInt("a.s.url.retrieval.constantRPSDuration") minutes),
        // peak requests/sec
        jumpToRps(conf.getInt("a.s.url.retrieval.maxRPS")),
        holdFor(conf.getInt("a.s.url.retrieval.maxRPSDuration") minutes),
        // ------------- CYCLE 2 -----------------
        // constant requests/sec
        reachRps(conf.getInt("a.s.url.retrieval.constantRPS")) in (10 seconds),
        holdFor(conf.getInt("a.s.url.retrieval.constantRPSDuration") minutes),
        // peak requests/sec
        jumpToRps(conf.getInt("a.s.url.retrieval.maxRPS")),
        holdFor(conf.getInt("a.s.url.retrieval.maxRPSDuration") minutes),
        // ------------- CYCLE 3 -----------------
        // constant requests/sec
        reachRps(conf.getInt("a.s.url.retrieval.constantRPS")) in (10 seconds),
        holdFor(conf.getInt("a.s.url.retrieval.constantRPSDuration") minutes),
        // peak requests/sec
        jumpToRps(conf.getInt("a.s.url.retrieval.maxRPS")),
        holdFor(conf.getInt("a.s.url.retrieval.maxRPSDuration") minutes),
        // ------------- CYCLE 4 -----------------
        // constant requests/sec
        reachRps(conf.getInt("a.s.url.retrieval.constantRPS")) in (10 seconds),
        holdFor(conf.getInt("a.s.url.retrieval.constantRPSDuration") minutes),
        // peak requests/sec
        jumpToRps(conf.getInt("a.s.url.retrieval.maxRPS")),
        holdFor(conf.getInt("a.s.url.retrieval.maxRPSDuration") minutes),
        // ------------- RETURN TO CONSTANT REQUESTS -----------------
        // constant requests/sec
        reachRps(conf.getInt("a.s.url.retrieval.constantRPS")) in (10 seconds),
        holdFor(conf.getInt("a.s.url.retrieval.constantRPSDuration") minutes)
      )
  )
    .protocols(httpConf)
    .maxDuration(conf.getInt("a.s.url.retrieval.maxDuration") minutes)
    .assertions(global.successfulRequests.percent.gt(conf.getInt("a.s.url.retrieval.successPercentGreaterThan")),
      global.responseTime.percentile3.lt(conf.getInt("a.s.url.retrieval.percentile3.responseTime")))
}
