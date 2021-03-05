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

  val idMapping10Instance =
    scenario("IdMapping KB->EMBL (#ids=10) Scenario")
      .forever {
        exec(getIdMappingFlowRequest(getRandomisedIds(10)))
      }

  val idMapping100Instance =
    scenario("IdMapping KB->EMBL (#ids=100) Scenario")
      .forever {
        exec(getIdMappingFlowRequest(getRandomisedIds(100)))
      }

  val idMapping1KInstance =
    scenario("IdMapping KB->EMBL (#ids=1K) Scenario")
      .forever {
        exec(getIdMappingFlowRequest(getRandomisedIds(1000)))
      }

  val idMapping5KInstance =
    scenario("IdMapping KB->EMBL (#ids=5K) Scenario")
      .forever {
        exec(getIdMappingFlowRequest(getRandomisedIds(5000)))
      }

  val idMapping10KInstance =
    scenario("IdMapping KB->EMBL (#ids=10K) Scenario")
      .forever {
        exec(getIdMappingFlowRequest(getRandomisedIds(10000)))
      }

  val idMapping20KInstance =
    scenario("IdMapping KB->EMBL (#ids=20K) Scenario")
      .forever {
        exec(getIdMappingFlowRequest(getRandomisedIds(20000)))
      }

  val idMapping50KInstance =
    scenario("IdMapping KB->EMBL (#ids=50K) Scenario")
      .forever {
        exec(getIdMappingFlowRequest(getRandomisedIds(50000)))
      }

  setUp(
    idMapping50Instance.inject(atOnceUsers(conf.getInt("a.s.idmapping.users")))
  )
    .protocols(httpConf)
    .assertions(global.responseTime.percentile3.lt(conf.getInt("a.s.multi.filters.percentile3.responseTime")), //percentile3 == 95th Percentile
      global.successfulRequests.percent.gt(conf.getInt("a.s.multi.filters.successPercentGreaterThan")))
    .maxDuration(conf.getInt("a.s.multi.filters.maxDuration") minutes)
}
