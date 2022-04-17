package edu.neu.csye7200

import org.apache.spark.sql.functions.{ count, when }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Success

class FitModelTest extends AnyFlatSpec with Matchers{

  val SAMPLE_SONGS_FILEPATH = "/sample_songs.csv"

  behavior of "ML Pipeline"

  it should "successfully preprocess dataframe" in {

    val filepath = getClass.getResource("/sample_songs.csv").getPath

    val try_processed_df = for(df <- FitModel.load_csv(filepath);
                               pdf <- FitModel.preprocessing(df)) yield pdf

    try_processed_df should matchPattern {
      case Success(_) =>
    }

    val processed_df = try_processed_df.get

    val popular_songs_num = processed_df.select(count(when(processed_df("label")===1, 1))).first().getLong(0)
    popular_songs_num shouldBe 454

    val features_num = processed_df.select("features").take(1)(0)(0).toString.split(",").length
    features_num shouldBe 17
  }

  it should "successfully train and predict" in {
    val filepath = getClass.getResource(SAMPLE_SONGS_FILEPATH).getPath
    val try_models = for(raw_df <- FitModel.load_csv(filepath);
        processed_df <- FitModel.preprocessing(raw_df))
    yield FitModel.fit(processed_df)

    try_models should matchPattern {
      case Success(_) =>
    }
  }

}
