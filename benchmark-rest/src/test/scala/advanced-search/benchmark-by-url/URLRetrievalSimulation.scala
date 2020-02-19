

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
    val url: String = "${url}"
    val urlRegex = raw"([A-Za-z0-9_\-\/]+)(\?|\/).*".r
    val basicRequest: String = url match {
      case urlRegex(baseUrl, _) => "url=" + baseUrl
      case _ => "Url request"
    }
    val httpReqInfo: String = basicRequest + ", format=${format}";
    val requestStr: String = host + "${url}";

    val request =
      feed(feeder)
        .pause(5 seconds, 15 seconds)
        .exec(http(httpReqInfo)
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
    instance.inject(atOnceUsers(conf.getInt("a.s.url.retrieval.users")))
  )
    .protocols(httpConf)
    .maxDuration(conf.getInt("a.s.url.retrieval.maxDuration") minutes)
}
