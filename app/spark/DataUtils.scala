package spark

import io.jhdf.HdfFile
import org.apache.spark.sql.{DataFrame, Encoders, Row, SparkSession}
import play.api.Logging
import play.api.libs.json.JsValue
import spark.FitModel.getSchema

import java.io.File
import java.util
import scala.annotation.tailrec
import scala.util.Try

object DataUtils extends Logging {

  // attributes we will use and their corresponding data types
  val metadataPairs = List(
    "artist_familiarity" -> Array[Double](),
    "artist_hotttnesss" -> Array[Double](),
    "artist_id" -> Array[String](),
    "artist_latitude" -> Array[Double](),
    "artist_location" -> Array[String](),
    "artist_longitude" -> Array[Double](),
    "artist_name" -> Array[String](),
    "title" -> Array[String]()
  )
  val metadataArrayPairs = List(
    "/metadata/artist_terms" -> Array[String](),
    "/metadata/artist_terms_freq" -> Array[Double](),
    "/metadata/artist_terms_weight" -> Array[Double]()
  )
  val analysisPairs = List(
    "danceability" -> Array[Double](),
    "duration" -> Array[Double](),
    "end_of_fade_in" -> Array[Double](),
    "energy" -> Array[Double](),
    "key" -> Array[Int](),
    "key_confidence" -> Array[Double](),
    "loudness" -> Array[Double](),
    "mode" -> Array[Int](),
    "mode_confidence" -> Array[Double](),
    "start_of_fade_out" -> Array[Double](),
    "tempo" -> Array[Double](),
    "time_signature" -> Array[Int](),
    "time_signature_confidence" -> Array[Double]()
  )
  val musicbrainzPairs = List(
    "year" -> Array[Int]()
  )


  def loadRowFromH5(h5FilePath: String): Try[Option[Row]] = Try {
    val file = new HdfFile(new File(h5FilePath))

    // song hotness
    val hotness = file
      .getDatasetByPath("/metadata/songs")
      .getData.asInstanceOf[util.LinkedHashMap[String, Any]]
      .get("song_hotttnesss")
      .asInstanceOf[Array[Double]]

    // skip reading rest of the attributes if hotness is missing
    if (hotness.isEmpty || hotness.apply(0).isNaN)
      None
    else Some {

      // meta data group
      val metadata = metadataPairs.map(pair => {
        file.getDatasetByPath("/metadata/songs")
          .getData.asInstanceOf[util.LinkedHashMap[String, Any]]
          .get(pair._1)
          .asInstanceOf[pair._2.type]
          .apply(0)
      }).seq

      // meta data arrays
      val metadataArray = metadataArrayPairs.map(pair => {
        file.getDatasetByPath(pair._1)
          .getData
          .asInstanceOf[pair._2.type]
      }).map(_.toArray)

      // analysis group
      val analysis = analysisPairs.map(pair => {
        file.getDatasetByPath("/analysis/songs")
          .getData.asInstanceOf[util.LinkedHashMap[String, Any]]
          .get(pair._1)
          .asInstanceOf[pair._2.type]
          .apply(0)
      }).seq

      // musicbrainz group
      val musicbrainz = musicbrainzPairs.map(pair => {
        file.getDatasetByPath("/musicbrainz/songs")
          .getData.asInstanceOf[util.LinkedHashMap[String, Any]]
          .get(pair._1)
          .asInstanceOf[pair._2.type]
          .apply(0)
      }).seq

      file.close()
      Row.fromSeq((hotness.apply(0) +: metadata) ++ analysis ++ metadataArray.map(_.mkString("[", ", ", "]")) ++ musicbrainz)
    }
  }

  def loadDataFromFolder(folderPath: String, spark: SparkSession): Try[DataFrame] = Try {
    @tailrec
    def inner(files: List[File], rows: List[Row]): List[Row] = {
      val newRows = rows ++
        files.filter(x => x.isFile && x.getName.endsWith(".h5"))
          .map(x => loadRowFromH5(x.getPath))
          .filter(x => x.isSuccess && x.get.isDefined)
          .map(_.get.get)

      val dirs = files.filter(_.isDirectory).map(_.listFiles())

      if (dirs.isEmpty) newRows else {
        val newFiles = dirs
          .reduce(_ ++ _)
          .toList
        inner(newFiles, newRows)
      }
    }

    val folderFile = new File(folderPath)
    if (folderFile.exists()) {
      val rowList = inner(List(folderFile), Nil)
      spark.createDataFrame(spark.sparkContext.parallelize(rowList), FitModel.getSchema())
    } else {throw new Exception("Invalid file path!")}
  }

  def loadCsv(filepath: String, spark: SparkSession): Try[DataFrame] = Try {
    spark.read
      .option("delimiter", ",")
      .schema(getSchema())
      .csv(filepath)
  }

  def dfFromJson(json: JsValue, spark: SparkSession): Try[DataFrame] = Try {
    spark.read
      .schema(getSchema(isTrainData = false))
      .json(
        spark.createDataset(List(json.toString()))(Encoders.STRING)
      )
  }
}
