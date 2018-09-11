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
object FiltersWithDownloadSimulation {

  val conf = ConfigFactory.load()

  val httpConf = http
    .baseURL(conf.getString("a.s.url"))
    .doNotTrackHeader("1")

  object DownloadFilterResultsScenario {
    val downloadFeeder = tsv(conf.getString("a.s.download.query.list")).random

    def getRequestWithFormat(format: String): ChainBuilder = {
      val httpReqInfo: String = "download filter results: ${query}"
      val queryRequestStr: String = "/download?query=${query}"

      val request =
        feed(downloadFeeder)
          .pause(15 seconds, 120 seconds)
          .exec(http(httpReqInfo)
            .get(queryRequestStr)
            .header("Accept", format)
          )

      return request
    }

    val requestSeq = Seq(
      DownloadFilterResultsScenario.getRequestWithFormat("text/flatfile")
    )

    val instance = scenario("Download Filter Results Scenario")
      .forever {
        exec(requestSeq)
      }
  }

  object FilterScenario {
    val generalSearchFeeder = tsv(conf.getString("a.s.multi.filters.general.search.list")).random
    val organismFeeder = tsv(conf.getString("a.s.multi.filters.organism.list")).random
    val accessionFeeder = tsv(conf.getString("a.s.multi.filters.accessions.retrieval.list")).random
    val taxonomyFeeder = tsv(conf.getString("a.s.multi.filters.taxonomy.list")).random
    val geneNameFeeder = tsv(conf.getString("a.s.multi.filters.gene.list")).random
    val proteinNameFeeder = tsv(conf.getString("a.s.multi.filters.protein.list")).random
//    val featureFeeder = tsv(conf.getString("advanced.search.feature.list")).random

    def getRequestWithFormat(format: String): ChainBuilder = {
      val filterGeneralRequestStr: String = "/searchCursor?query=content:${content}"
      val filterOrganismRequestStr: String = "/searchCursor?query=tax_name_lineage:${organism}"
      val accessionRequestStr: String = "/searchCursor?query=accession:${accession}"
      val filterTaxonomyRequestStr: String = "/searchCursor?query=tax_id_lineage:${taxon}"
      val filterGeneRequestStr: String = "/searchCursor?query=gene:${gene}"
      val filterProteinRequestStr: String = "/searchCursor?query=protein_name:${protein}"
//      val filterFeatureRequestStr: String = "/searchCursor?query=ft_molecule_processing:${feature}"

      val request =
        feed(accessionFeeder)
          .feed(organismFeeder)
          .feed(generalSearchFeeder)
          .feed(taxonomyFeeder)
          .feed(geneNameFeeder)
          .feed(proteinNameFeeder)
          .pause(5 seconds, 15 seconds)
          .exec(http("content field")
            .get(filterGeneralRequestStr)
            .header("Accept", format))
          .pause(2 seconds, 5 seconds)
          .exec(http("tax_name_lineage field")
            .get(filterOrganismRequestStr)
            .header("Accept", format))
          .pause(2 seconds, 10 seconds)
          .exec(http("accession field")
            .get(accessionRequestStr)
            .header("Accept", format))
          .pause(2 seconds, 5 seconds)
          .exec(http("tax_id_lineage field")
            .get(filterTaxonomyRequestStr)
            .header("Accept", format))
          .pause(2 seconds, 5 seconds)
          .exec(http("gene field")
            .get(filterGeneRequestStr)
            .header("Accept", format))
          .pause(2 seconds, 5 seconds)
//          .exec(http("feature molecule processing field")
//            .get(filterFeatureRequestStr)
//            .header("Accept", format))
//          .pause(2 seconds, 5 seconds)
          .exec(http("protein_name field")
            .get(filterProteinRequestStr)
            .header("Accept", format))

      return request
    }

    val requestSeq = Seq(
      FilterScenario.getRequestWithFormat("application/json")
    )

    val instance = scenario("Multiple Filter Request Scenario")
      .forever {
        exec(requestSeq)
      }
  }

  class FiltersWithDownloadSimulation extends Simulation {
    setUp(
      FilterScenario.instance.inject(atOnceUsers(conf.getInt("a.s.multi.filters.users"))),
      DownloadFilterResultsScenario.instance.inject(atOnceUsers(conf.getInt("a.s.multi.filters.download.users")))
    )
      .protocols(FiltersWithDownloadSimulation.httpConf)
//      .assertions(global.responseTime.percentile3.lte(500), global.successfulRequests.percent.gte(99))
      .maxDuration(conf.getInt("a.s.multi.filters.maxDuration") minutes)
  }
}
