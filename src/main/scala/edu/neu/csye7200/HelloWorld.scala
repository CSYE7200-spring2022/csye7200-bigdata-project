package edu.neu.csye7200

import org.apache.spark.sql.{Dataset, SparkSession}

object HelloWorld extends App {
  def testOutput:Int = 5

  println("Hello, world!")

  val spark: SparkSession = SparkSession
    .builder()
    .appName("test")
    .master("local[*]")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

  var filepath = getClass.getResource("/username.csv")

  val filename = filepath.getPath

  val df = spark.read
    .option("header", "true")
    .option("delimiter", ";")
    .csv(filename)

  df.show()
}
