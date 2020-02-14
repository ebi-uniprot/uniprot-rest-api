// https://computingforgeeks.com/how-to-install-apache-spark-on-ubuntu-debian/
import java.io.FileInputStream
import java.util.Properties

val service = "uniprotkb"

val (inputFile, outputDir) =
  try {
    val prop = new Properties()
    prop.load(new FileInputStream("/home/edd/working/intellij/website/uniprot-rest-api/benchmark-rest/src/test/scala/advanced-search/benchmark-by-url/config.properties"))
    (prop.getProperty("uniprotkb.sourceFilePath"),
      prop.getProperty("uniprotkb.outputDir"))
  } catch {
    case e: Exception =>
      e.printStackTrace()
      sys.exit(1)
  }

val ff = sc.textFile("file://" + inputFile)

val contentTypes = Seq("application/json", "text/tsv", "text/flatfile", "application/json", "application/vnd.ms-excel", "text/fasta", "text/gff")
val endPoints = Seq("/uniprot/api/" + service + "/search?query=XXXX")

val pattern = "^GN   Name=([A-Za-z0-9_]+).*".r
val count = 20000
val random = new Random

def randomItemFrom(l: Seq[String]): String = {
  return l(random.nextInt(l.length))
}

sc.parallelize(ff
  .filter(line => line.startsWith("GN   Name="))
  .map(line => {
    val pattern(name) = line
    name
  })
  .map(gene => randomItemFrom(endPoints).replaceAll("XXXX", gene) + "#" + randomItemFrom(contentTypes))
  .distinct()
  .take(count))
  //  .coalesce(1)
  .saveAsTextFile("file://" + outputDir)

println("========= DONE =========")
