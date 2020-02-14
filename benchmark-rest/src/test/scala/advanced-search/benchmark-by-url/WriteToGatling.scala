// https://computingforgeeks.com/how-to-install-apache-spark-on-ubuntu-debian/
import java.io.FileInputStream
import java.util.Properties

class WriteToGatling {
  val random = new Random

  def randomItemFrom(l: Seq[String]): String = {
    return l(random.nextInt(l.length))
  }

  def createGatlingLine(v: String): String = {
    randomItemFrom(endPoints).replaceAll("XXXX", v) + "#" + randomItemFrom(contentTypes)
  }
}