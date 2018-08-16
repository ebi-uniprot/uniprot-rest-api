import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import scala.concurrent.duration._
import com.typesafe.config._

/**
  * Simulates simple retrieval of accessions from the REST service. The most basic ID retrieval, which serves
  * as a basis on which other test performances can be compared.
  */
object AccessionRetrievalSimulation {

  val conf = ConfigFactory.load()

  val httpConf = http
    .baseURL(conf.getString("a.s.url"))
    .doNotTrackHeader("1")

  object AccessionScenario {
    val feeder = tsv(conf.getString("a.s.accession.retrieval.list")).random

    def getRequestWithFormat(format: String): ChainBuilder = {
      val httpReqInfo: String = "accession=${accession}, format=" + format;
      val requestStr: String = "/searchCursor?query=accession:${accession}";

      val request =
        feed(feeder)
          .pause(5 seconds, 15 seconds)
          .exec(http(httpReqInfo)
            .get(requestStr)
            .header("Accept", format)
          )

      return request
    }

    val requestSeq = Seq(
      AccessionScenario.getRequestWithFormat("application/json")
    )

    val instance = scenario("Accession Retrieval Scenario")
      .forever {
        exec(requestSeq)
      }
  }

  class AccessionRetrievalSimulation extends Simulation {
    setUp(
      AccessionScenario.instance.inject(atOnceUsers(conf.getInt("a.s.accession.retrieval.users")))
    )
      .protocols(AccessionRetrievalSimulation.httpConf)
//      .assertions(global.responseTime.percentile3.lte(500), global.successfulRequests.percent.gte(99))
      .maxDuration(conf.getInt("a.s.accession.retrieval.maxDuration") minutes)
  }
}
