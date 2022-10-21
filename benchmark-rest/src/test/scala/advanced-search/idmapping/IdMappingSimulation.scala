import scala.concurrent.duration._
import scala.io.Source
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import scala.concurrent.duration._
import com.typesafe.config._

/**
 * Simulates users accessing the /idmapping service to generate a range of performance statistics
 * for different sizes of source 'ids'.
 */
class IdMappingSimulation extends Simulation {
  val conf = ConfigFactory.load()

  val httpConf = http
    .userAgentHeader("Benchmarker")
    .doNotTrackHeader("1")

  // ========================== URL VARIABLES ==========================
  val host = conf.getString("a.s.host")
  val from = conf.getString("a.s.idmapping.from")
  val to = conf.getString("a.s.idmapping.to")
  val runUrl = host + conf.getString("a.s.idmapping.run.url")
  val statusUrl = host + conf.getString("a.s.idmapping.status.url")
  val resultsUrl = host + conf.getString("a.s.idmapping.results.url")
  val resultsParams = conf.getString("a.s.idmapping.results.params")

  // ========================== SCENARIO VARIABLES: USER COUNT + ID COUNT ==========================
  val scenario1Users = conf.getInt("a.s.idmapping.scenario1.users")
  val scenario1IdCount = conf.getInt("a.s.idmapping.scenario1.idCount")
  val scenario2Users = conf.getInt("a.s.idmapping.scenario2.users")
  val scenario2IdCount = conf.getInt("a.s.idmapping.scenario2.idCount")
  val scenario3Users = conf.getInt("a.s.idmapping.scenario3.users")
  val scenario3IdCount = conf.getInt("a.s.idmapping.scenario3.idCount")

  // ========================== FEEDERS ==========================
  val rawAccessions = Source.fromFile(conf.getString("a.s.idmapping.accessions.csv")).getLines.mkString
  val accessions = rawAccessions.split(",").map(_.trim()).toSeq
  val scenario1RandomIdFeeder =
    Iterator.continually(Map("randomIds" -> getRandomisedIds(scenario1IdCount)))
  val scenario2RandomIdFeeder =
    Iterator.continually(Map("randomIds" -> getRandomisedIds(scenario2IdCount)))
  val scenario3RandomIdFeeder =
    Iterator.continually(Map("randomIds" -> getRandomisedIds(scenario3IdCount)))

  def getRandomisedIds(count: Integer): String = {
    scala.util.Random.shuffle(accessions).slice(0, count).mkString(",")
  }

  // ========================== SCENARIO DEFINITIONS ==========================
  val scenario1FlowRequest: ChainBuilder =
    pause(5 seconds, 60 seconds)
      .feed(scenario1RandomIdFeeder)
  val scenario2FlowRequest: ChainBuilder =
    pause(5 seconds, 60 seconds)
      .feed(scenario2RandomIdFeeder)
  val scenario3FlowRequest: ChainBuilder =
    pause(5 seconds, 60 seconds)
      .feed(scenario3RandomIdFeeder)

  def getIdMappingFlowRequest(feederBuilder: ChainBuilder, users: Integer, idCount: Integer): ChainBuilder = {
    val requestId = s"[$from->$to, users=$users, ids=$idCount]"
    val httpReqInfo: String = s"POST /run $requestId"

    feederBuilder
      .exec(http(httpReqInfo)
        .post(runUrl)
        .formParam("ids", "${randomIds}")
        .formParam("from", from)
        .formParam("to", to)
        .check(
          jsonPath("$.jobId").saveAs("jobId")
        )
      )
      .doIf("${jobId.exists()}") {
        doWhile(session => session("jobStatus").as[String].equals("NEW") || session("jobStatus").as[String].equals("RUNNING")) {
          pause(2 seconds)
            .exec(
              http(s"GET /status/JOB_ID $requestId")
                .get(statusUrl + "/${jobId}")
                .disableFollowRedirect
                .check(
                  status.not(400), status.not(500),
                  jsonPath("$.jobStatus").saveAs("jobStatus")
                )
            )
        }.doIfEquals("${jobStatus}", "FINISHED") {
          exec(http(s"GET /results/stream/JOB_ID $requestId")
            .get(resultsUrl + "/${jobId}?" + resultsParams)
            .check(status.is(200)))
        }
      }
  }

  val idMappingScenario1 =
    scenario("IdMapping Scenario 1 (" + from + "->" + to + ", #users=" + scenario1Users + ", #ids=" + scenario1IdCount + ")")
      .forever {
        exec(
          getIdMappingFlowRequest(scenario1FlowRequest, scenario1Users, scenario1IdCount)
        )
      }
  val idMappingScenario2 =
    scenario("IdMapping Scenario 2 (" + from + "->" + to + ", #users=" + scenario2Users + ", #ids=" + scenario2IdCount + ")")
      .forever {
        getIdMappingFlowRequest(scenario2FlowRequest, scenario2Users, scenario2IdCount)
      }
  val idMappingScenario3 =
    scenario("IdMapping Scenario 3 (" + from + "->" + to + ", #users=" + scenario3Users + ", #ids=" + scenario3IdCount + ")")
      .forever {
        getIdMappingFlowRequest(scenario3FlowRequest, scenario3Users, scenario3IdCount)
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
