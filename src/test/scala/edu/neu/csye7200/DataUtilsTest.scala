package edu.neu.csye7200

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.{Failure, Success}

class DataUtilsTest extends AnyFlatSpec with Matchers {

  val SAMPLE_H5_FILEPATH = "/sample.h5"
  val SAMPLE_H5_FILEPATH_NO_HOTNESS = "/sample_no_hotness.h5"

  behavior of "loadDataFromH5"

  it should "successfully load sample .h5 file" in {
    val fullPath = getClass.getResource(SAMPLE_H5_FILEPATH).getPath
    val sampleRowOptTry = DataUtils.loadDataFromH5(fullPath)

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
    sampleRow.getAs[List[String]](9).size shouldBe 37
    sampleRow.getDouble(12) shouldBe 0.0
    sampleRow.getInt(25) shouldBe 0
  }

  it should "skip the row if hotness field is missing" in {
    val fullPath = getClass.getResource(SAMPLE_H5_FILEPATH_NO_HOTNESS).getPath
    val rowOptTry = DataUtils.loadDataFromH5(fullPath)

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
    val rowOptTry = DataUtils.loadDataFromH5(filepath)
    rowOptTry should matchPattern {
      case Failure(_) =>
    }
  }
}
