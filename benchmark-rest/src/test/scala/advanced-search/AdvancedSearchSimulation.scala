import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import scala.concurrent.duration._

// Variables used in this test have been externalised to system properties, so that we do not commit
// large files to this git repo. Please set the following properties which can be passed to maven as -DvariableName=value
//  advanced.search.url : the base url of the server under test
//  advanced.search.accessions.csv : the file containing the accessions used in this test
object AdvancedSearchSimulation {

  val httpConf = http
    .baseURL(System.getProperty("advanced.search.url")) // Here is the root for all relative URLs
    .doNotTrackHeader("1")

  object AccessionScenario {
    val feeder = csv(System.getProperty("advanced.search.accessions.csv")).random

    def getRequestWithFormat(format: String): ChainBuilder = {
      val httpReqInfo: String = "accession=${accession}, format=" + format;
      val requestStr: String = "/searchCursor?query=accession:${accession}";

      val request =
        repeat(10, "i") {
        feed(feeder)
          .exec(http(httpReqInfo)
            .get(requestStr)
            .header("Accept", format)
          )
      }

      return request
    }

    val requestSeq = Seq(
      AccessionScenario.getRequestWithFormat("application/json")
    )

    val instance = scenario("ScenarioAccession").exec(requestSeq);
  }

  class BasicSimulation extends Simulation {
    setUp(
      // rampUsers(nbUsers) over(duration) => injects a given number of users with a linear ramp over a given duration.
      AccessionScenario.instance.inject(atOnceUsers(700))
    )
      .throttle(
        reachRps(700) in (10 seconds),
        holdFor(1 minutes)
      )
      .protocols(AdvancedSearchSimulation.httpConf)
  }

}