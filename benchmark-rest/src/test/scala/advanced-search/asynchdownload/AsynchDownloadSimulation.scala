import scala.concurrent.duration._
import scala.io.Source
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import scala.concurrent.duration._
import com.typesafe.config._

/**
 * Simulates users submits the /asynchdownalod job and verifies the ids and result
 */
class AsynchDownloadSimulation extends Simulation {
  val conf = ConfigFactory.load()

  val httpConf = http
    .userAgentHeader("Benchmarker")
    .doNotTrackHeader("1")

  // ========================== URL VARIABLES ==========================
  val host = conf.getString("a.s.host")
  val nginxHost = conf.getString("a.s.nginx.host")
  val downloadFeeder = separatedValues(conf.getString("a.s.asynch.download.query.list"), '#').random
  val runUrl = host + conf.getString("a.s.asynch.download.run.url")
  val statusUrl = host + conf.getString("a.s.asynch.download.status.url")
  val idsUrl = nginxHost + conf.getString("a.s.asynch.download.ids.url")
  val resultsUrl = nginxHost + conf.getString("a.s.asynch.download.results.url")


  // ========================== SCENARIO VARIABLES: USER COUNT ==========================
  val scenario1Users = conf.getInt("a.s.asynch.download.scenario1.users")

  // ========================== SCENARIO DEFINITIONS ==========================
  val scenario1FlowRequest: ChainBuilder =
    pause(5 seconds, 60 seconds)
      .feed(downloadFeeder)

  def getAsynchDownloadFlowRequest(feederBuilder: ChainBuilder, users: Integer): ChainBuilder = {
    val queryRequestStr: String = runUrl + "?${query}&format=${format}"
    val httpReqInfo: String = "POST /download/run"

    feederBuilder
      .exec(http(httpReqInfo)
        .post(queryRequestStr)
        .check(
          jsonPath("$.jobId").saveAs("jobId")
        )
      )
      .doIf("${jobId.exists()}") {
        doWhile(session => session("jobStatus").as[String].equals("NEW") || session("jobStatus").as[String].equals("RUNNING")) {
          pause(2 seconds)
            .exec(
              http(s"GET /status/JOB_ID")
                .get(statusUrl + "/${jobId}")
                .disableFollowRedirect
                .check(
                  status.not(400), status.not(500),
                  jsonPath("$.jobStatus").saveAs("jobStatus")
                )
            )
        }.doIfEquals("${jobStatus}", "FINISHED") {
          exec(http(s"GET /ids/JOB_ID")
            .get(idsUrl + "/${jobId}?")
            .check(status.is(200)))
        }.doIfEquals("${jobStatus}", "FINISHED") {
          exec(http(s"GET /results/JOB_ID")
            .get(resultsUrl + "/${jobId}?")
            .check(status.is(200)))
        }
      }
  }

  val asynchDonwloadScenario1 =
    scenario("Asynch donwload Scenario 1 (#users=" + scenario1Users + ")")
      .forever {
        exec(
          getAsynchDownloadFlowRequest(scenario1FlowRequest, scenario1Users)
        )
      }

  setUp(
    asynchDonwloadScenario1.inject(atOnceUsers(scenario1Users))
  )
    .protocols(httpConf)
    .assertions(global.successfulRequests.percent.gt(conf.getInt("a.s.asynch.download.successPercentGreaterThan")))
    .maxDuration(conf.getInt("a.s.asynch.download.maxDuration") minutes)
}
