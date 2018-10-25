import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import scala.concurrent.duration._
import com.typesafe.config._

/**
  * Simulates common user behaviour, where a concurrent users perform a number of filters on the UniProtKB,
  * and occasionally downloads a result set.
  */
class FiltersWithDownloadSimulation extends Simulation {

  val conf = ConfigFactory.load()

  val httpConf = http
    .userAgentHeader("Benchmarker")
    .doNotTrackHeader("1")

  // --------- DOWNLOAD SCENARIO ----------
  val downloadFeeder = separatedValues(conf.getString("a.s.download.query.list"), '#').random

  def getDownloadRequestWithFormat(): ChainBuilder = {
    val httpReqInfo: String = "url=${download_url}, format=${download_format}, encoding=${download_encoding}"
    val queryRequestStr: String = "${download_url}"

    val request =
      feed(downloadFeeder)
        .pause(15 seconds, 120 seconds)
        .exec(http(httpReqInfo)
          .get(queryRequestStr)
          .header("Accept", "${download_format}")
          .header("Accept-Encoding", "${download_encoding}")
        )

    return request
  }

  val downloadRequestSeq = Seq(
    getDownloadRequestWithFormat()
  )

  val downloadInstance = scenario("Download Filter Results Scenario")
    .forever {
      exec(downloadRequestSeq)
    }

  // --------- FILTERS SCENARIO ----------
  val generalSearchFeeder = separatedValues(conf.getString("a.s.multi.filters.general.search.list"), '#').random
  val organismFeeder = separatedValues(conf.getString("a.s.multi.filters.organism.list"), '#').random
  val accessionFeeder = separatedValues(conf.getString("a.s.multi.filters.accessions.retrieval.list"), '#').random
  val taxonomyFeeder = separatedValues(conf.getString("a.s.multi.filters.taxonomy.list"), '#').random
  val geneNameFeeder = separatedValues(conf.getString("a.s.multi.filters.gene.list"), '#').random
  val proteinNameFeeder = separatedValues(conf.getString("a.s.multi.filters.protein.list"), '#').random
  //    val featureFeeder = tsv(conf.getString("advanced.search.feature.list")).random

  def getFiltersRequestWithFormat(): ChainBuilder = {
    //      val filterFeatureRequestStr: String = "/searchCursor?query=ft_molecule_processing:${feature}"

    val request =
      feed(accessionFeeder)
    feed(organismFeeder)
      .feed(generalSearchFeeder)
      .feed(taxonomyFeeder)
      .feed(geneNameFeeder)
      .feed(proteinNameFeeder)
      .pause(5 seconds, 15 seconds)
      .exec(http("content field")
        .get("${content_url}")
        .header("Accept", "${content_format}"))
      .pause(5 seconds, 15 seconds)
      .exec(http("organism field")
        .get("${organism_url}")
        .header("Accept", "${organism_format}"))
      .pause(5 seconds, 15 seconds)
      .exec(http("accession field")
        .get("${accession_url}")
        .header("Accept", "${accession_format}"))
      .pause(5 seconds, 15 seconds)
      .exec(http("taxon field")
        .get("${taxon_url}")
        .header("Accept", "${taxon_format}"))
      .pause(5 seconds, 15 seconds)
      .exec(http("gene field")
        .get("${gene_url}")
        .header("Accept", "${gene_format}"))
      .pause(5 seconds, 15 seconds)
      //          .exec(http("feature molecule processing field")
      //            .get(filterFeatureRequestStr)
      //            .header("Accept", format))
      .pause(5 seconds, 15 seconds)
      .exec(http("protein field")
        .get("${protein_url}")
        .header("Accept", "${protein_format}"))

    return request
  }

  val filtersRequestSeq = Seq(
    getFiltersRequestWithFormat()
  )

  val filtersInstance = scenario("Multiple Filter Request Scenario")
    .forever {
      exec(filtersRequestSeq)
    }

  setUp(
    filtersInstance.inject(atOnceUsers(conf.getInt("a.s.multi.filters.users"))),
    downloadInstance.inject(atOnceUsers(conf.getInt("a.s.multi.filters.download.users")))
  )
    .protocols(httpConf)
    //      .assertions(global.responseTime.percentile3.lte(500), global.successfulRequests.percent.gte(99))
    .maxDuration(conf.getInt("a.s.multi.filters.maxDuration") minutes)
}
