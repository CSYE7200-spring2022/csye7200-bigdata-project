package spark

import org.apache.spark.sql.SparkSession
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsValue, Json}
import play.api.test.Injecting

import scala.util.{Failure, Success}

class DataUtilsTest extends AnyFlatSpec with Matchers {

  val SAMPLE_H5_FILEPATH = "/sample.h5"
  val SAMPLE_H5_FILEPATH_NO_HOTNESS = "/sample_no_hotness.h5"
  val SAMPLE_H5_FOLDER_PATH = "/A"

  val json: JsValue = Json.parse{
    val file = scala.io.Source.fromFile(getClass.getResource("/sample.json").getPath)
    val js = file.mkString
    file.close()
    js
  }

  val spark: SparkSession = SparkSession
    .builder()
    .appName("test")
    .master("local[*]")
    .getOrCreate()

  behavior of "loadDataFromH5"

  it should "successfully load sample .h5 file" in {
    val fullPath = getClass.getResource(SAMPLE_H5_FILEPATH).getPath
    val sampleRowOptTry = DataUtils.loadRowFromH5(fullPath)

    sampleRowOptTry should matchPattern {
      case Success(_) =>
    }

    val sampleRowOpt = sampleRowOptTry.get

    sampleRowOpt should matchPattern {
      case Some(_) =>
    }

    val sampleRow = sampleRowOpt.get

    sampleRow.size shouldBe 26
    sampleRow.getDouble(0) shouldBe (0.602 +- 0.01)
    sampleRow.getString(8) shouldBe "I Didn't Mean To"
    sampleRow.getDouble(12) shouldBe 0.0
    sampleRow.getInt(25) shouldBe 0
  }

  it should "skip the row if hotness field is missing" in {
    val fullPath = getClass.getResource(SAMPLE_H5_FILEPATH_NO_HOTNESS).getPath
    val rowOptTry = DataUtils.loadRowFromH5(fullPath)

    rowOptTry should matchPattern {
      case Success(_) =>
    }

    val rowOpt = rowOptTry.get

    rowOpt should matchPattern {
      case None =>
    }
  }

  it should "return failure when given invalid filepath" in {
    val filepath = "/invalid/filepath"
    val rowOptTry = DataUtils.loadRowFromH5(filepath)
    rowOptTry should matchPattern {
      case Failure(_) =>
    }
  }

  behavior of "loadDataFromFolder"

  it should "successfully load data from h5 folders" in {
    val dfTry = DataUtils.loadDataFromFolder(
      getClass.getResource(SAMPLE_H5_FOLDER_PATH).getPath,
      spark
    )

    dfTry should matchPattern {
      case Success(_) =>
    }
    val df = dfTry.get
    df.show()
    df.count() shouldBe 12
    df.columns.length shouldBe 26
  }

  it should "fail when given wrong filepath" in {
    val dfTry = DataUtils.loadDataFromFolder(
      "wrong path",
      spark
    )

    dfTry should matchPattern {
      case Failure(_) =>
    }
  }

  behavior of "Json converter"

  it should "successfully convert JsValue to DataFrame" in {
    val df = DataUtils.dfFromJson(json, spark)
    df.count() shouldBe 1
    df.select("artist_latitude").head().getDouble(0) shouldBe 8.4177
  }
}
