import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import scala.concurrent.duration._
import com.typesafe.config._
import scala.io.Source

/**
 * Simulates users accessing the /idmapping service to generate a range of performance statistics
 * for different sizes of source 'ids'.
 */
class IdMappingSimulation extends Simulation {

  val conf = ConfigFactory.load()

  val httpConf = http
    .userAgentHeader("Benchmarker")
    .doNotTrackHeader("1")

  val host = conf.getString("a.s.host")
  val rawAccessions = Source.fromFile(conf.getString("a.s.idmapping.accessions.csv")).getLines.mkString
  val accessions = rawAccessions.split(",").map(_.trim()).toSeq

  val scenario1Users =   conf.getInt("a.s.idmapping.scenario1.users")
  val scenario1IdCount = conf.getInt("a.s.idmapping.scenario1.idCount")
  val scenario2IdCount = conf.getInt("a.s.idmapping.scenario2.users")
  val scenario2Users =   conf.getInt("a.s.idmapping.scenario2.idCount")
  val scenario3Users =   conf.getInt("a.s.idmapping.scenario3.users")
  val scenario3IdCount = conf.getInt("a.s.idmapping.scenario3.idCount")


  def getRandomisedIds(count: Integer): String = {
    val randomList = scala.util.Random.shuffle(accessions).slice(0, count).mkString(",")
    return randomList
  }

  // --------- IDMAPPING SCENARIO ----------
  def getIdMappingFlowRequest(ids: String): ChainBuilder = {
    val count = s"${ids}".count(_ == ',') + 1
    val httpReqInfo: String = "POST /run [KB->EMBL, "+count+"]"
    val queryRequestStr: String = host + "/uniprot/api/idmapping/run"

    val request =
        pause(5 seconds, 60 seconds)
        .exec(http(httpReqInfo)
          .post(queryRequestStr)
          .formParam("ids", ids)
          .formParam("from", "UniProtKB_AC-ID")
          .formParam("to", "EMBL-GenBank-DDBJ_CDS")
          .check(
            jsonPath("$.jobId").saveAs("jobId")
          )
        )
          .doIf("${jobId.exists()}") {
            tryMax(100) {
              exec(
                http("GET /status/JOB_ID [KB->EMBL, "+count+"]")
                  .get(host + "/uniprot/api/idmapping/status/${jobId}")
                  .disableFollowRedirect
                  .check(
                     status.not(400), status.not(500),
                     jsonPath("$.jobStatus").saveAs("jobStatus")
                  )
              )
                .doIfEquals("${jobStatus}", "FINISHED") {
                    exec( http("GET /results/JOB_ID [KB->EMBL, "+count+"]")
                      .get(host + "/uniprot/api/idmapping/results/${jobId}")
                      .check(status.is(200)))
                }
            }
          }

    return request
  }
  
  val idMappingScenario1 =
    scenario("IdMapping KB->EMBL Scenario 1 (#users="+scenario1Users+", #ids="+scenario1IdCount+")")
      .forever {
        exec(getIdMappingFlowRequest(getRandomisedIds(scenario1IdCount)))
      }

  val idMappingScenario2 =
    scenario("IdMapping KB->EMBL Scenario 2 (#users="+scenario2Users+", #ids="+scenario2IdCount+")")
      .forever {
        exec(getIdMappingFlowRequest(getRandomisedIds(scenario2IdCount)))
      }

  val idMappingScenario3 =
    scenario("IdMapping KB->EMBL Scenario 3 (#users="+scenario3Users+", #ids="+scenario3IdCount+")")
      .forever {
        exec(getIdMappingFlowRequest(getRandomisedIds(scenario3IdCount)))
      }
 setUp(
   idMappingScenario1.inject(atOnceUsers(scenario1Users)),
   idMappingScenario2.inject(atOnceUsers(scenario2Users)),
   idMappingScenario3.inject(atOnceUsers(scenario3Users))
  )
    .protocols(httpConf)
    .assertions(global.responseTime.percentile3.lt(conf.getInt("a.s.idmapping.percentile3.responseTime")), //percentile3 == 95th Percentile
      global.successfulRequests.percent.gt(conf.getInt("a.s.idmapping.successPercentGreaterThan")))
    .maxDuration(conf.getInt("a.s.idmapping.maxDuration") minutes)
}
