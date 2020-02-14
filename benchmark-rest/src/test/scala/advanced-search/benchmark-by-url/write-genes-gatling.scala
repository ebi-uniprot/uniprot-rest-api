// https://computingforgeeks.com/how-to-install-apache-spark-on-ubuntu-debian/
import java.io.FileInputStream
import java.util.Properties
import WriteToGatling

val service = "uniprotkb"

// load configuration properties
val (inputFile, outputDir, count, contentTypesStr, endPointsStr) =
  try {
    val prop = new Properties()
    prop.load(new FileInputStream("/home/edd/working/intellij/website/uniprot-rest-api/benchmark-rest/src/test/scala/advanced-search/benchmark-by-url/config.properties"))
    (prop.getProperty("uniprotkb.sourceFilePath"),
      prop.getProperty("uniprotkb.genes.outputDir"),
      new Integer(prop.getProperty("uniprotkb.genes.count")),
    prop.getProperty("uniprotkb.genes.contentTypes"),
    prop.getProperty("uniprotkb.genes.endPoints"))
  } catch {
    case e: Exception =>
      e.printStackTrace()
      sys.exit(1)
  }

val contentTypes=contentTypesStr.split(",")
val endPoints=endPointsStr.split(",")
val ff = sc.textFile("file://" + inputFile)
val pattern = "^GN   Name=([A-Za-z0-9_]+).*".r
val random = new Random

def randomItemFrom(l: Seq[String]): String = {
  return l(random.nextInt(l.length))
}

def createGatlingLine(v: String): String = {
  randomItemFrom(endPoints).replaceAll("XXXX", v) + "#" + randomItemFrom(contentTypes)
}

sc.parallelize(ff
  .filter(line => line.startsWith("GN   Name="))
  .map(line => {
    val pattern(name) = line
    name
  })
  .map(createGatlingLine)
  .distinct()
  .take(count)).coalesce(1).saveAsTextFile("file://" + outputDir)

println("========= DONE =========")
