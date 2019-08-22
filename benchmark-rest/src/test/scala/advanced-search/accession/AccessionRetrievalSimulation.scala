import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import scala.concurrent.duration._
import com.typesafe.config._

/**
  * Simulates simple retrieval of accessions from the REST service. The most basic ID retrieval, which serves
  * as a basis to which other test performances can be compared.
  */
class AccessionRetrievalSimulation extends Simulation {

  val conf = ConfigFactory.load()

  val httpConf = http
    .userAgentHeader("Benchmarker")
    .doNotTrackHeader("1")

  val host = conf.getString("a.s.host")
  val feeder = separatedValues(conf.getString("a.s.accession.retrieval.list"), '#').random

  def getRequest(): ChainBuilder = {
    val httpReqInfo: String = "accession retrieval, format=${accession_format}";
    val requestStr: String = host + "${accession_url}";

    val request =
      feed(feeder)
        .pause(5 seconds, 15 seconds)
        .exec(http(httpReqInfo)
          .get(requestStr)
          .header("Accept", "${accession_format}")
        )

    return request
  }

  val requestSeq = Seq(
    getRequest()
  )

  val instance = scenario("Accession Retrieval Scenario")
    .forever {
      exec(requestSeq)
    }

  setUp(
    instance.inject(atOnceUsers(conf.getInt("a.s.accession.retrieval.users")))
  )
    .protocols(httpConf)
    .maxDuration(conf.getInt("a.s.accession.retrieval.maxDuration") minutes)
}
