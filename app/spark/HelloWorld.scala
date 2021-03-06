package spark

import org.apache.spark.sql.SparkSession

object HelloWorld extends App {
  def testOutput: Int = 5

  println("Hello, world!")

  val spark: SparkSession = SparkSession
    .builder()
    .appName("test")
    .master("local[*]")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

  val filepath = getClass.getResource("/username.csv").getPath

  val df = spark.read
    .option("header", "true")
    .option("delimiter", ";")
    .csv(filepath)

  df.show()
}
