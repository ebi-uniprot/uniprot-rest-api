// https://computingforgeeks.com/how-to-install-apache-spark-on-ubuntu-debian/
import java.io.FileInputStream
import java.util.Properties
import scala.util.Random

val service = "uniprotkb"

// load configuration properties
val (inputFile, outputDir, count, contentTypesStr, endPointsStr) =
  try {
    val prop = new Properties()
    prop.load(new FileInputStream("/home/eddturner/working/intellij/website/uniprot-rest-api/benchmark-rest/src/test/scala/advanced-search/benchmark-by-url/config.properties"))
    (prop.getProperty("uniprotkb.sourceFilePath"),
      prop.getProperty("uniprotkb.proteins.outputDir"),
      new Integer(prop.getProperty("uniprotkb.proteins.count")),
    prop.getProperty("uniprotkb.proteins.contentTypes"),
    prop.getProperty("uniprotkb.proteins.endPoints"))
  } catch {
    case e: Exception =>
      e.printStackTrace()
      sys.exit(1)
  }

val contentTypes=contentTypesStr.split(",")
val endPoints=endPointsStr.split(",")
//val pattern = "^DE   SubName: Full=([A-Za-z0-9\\-]+)( [A-Za-z0-9\\-]+)?( [A-Za-z0-9-]+)?.*/\\1/".r
val pattern = "^DE   ...Name: Full=([A-Za-z0-9 ]+).*".r
val random = new Random

def randomItemFrom(l: Seq[String]): String = {
  l(random.nextInt(l.length))
}

def createGatlingLine(v: String): String = {
  randomItemFrom(endPoints).replaceAll("XXXX", v) + "#" + randomItemFrom(contentTypes)
}

val ff = sc.textFile("file://" + inputFile)
sc.parallelize(ff
  .filter(line => line.matches("^DE   ...Name: Full=.*"))
  .filter(line => !line.matches(".*\\{.*"))
  .map(line => {
    val pattern(name) = line
    println(">>>>> "+name)
    name
  })
  .map(createGatlingLine)
  .distinct()
  .take(count)).coalesce(1).saveAsTextFile("file://" + outputDir)

println("========= DONE =========")
