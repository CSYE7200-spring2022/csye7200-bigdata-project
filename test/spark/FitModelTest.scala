package spark

import org.apache.spark.ml.PipelineModel
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{count, when}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.tagobjects.Slow
import play.api.libs.json.{JsValue, Json}
import spark.FitModel.assembleScalePipeline

import scala.util.Success

class FitModelTest extends AnyFlatSpec with Matchers{

  val SAMPLE_SONGS_FILEPATH = "/sample_songs.csv"

  val spark: SparkSession = SparkSession
    .builder()
    .appName("test")
    .master("local[*]")
    .getOrCreate()


  behavior of "ML Pipeline"

  it should "successfully preprocess dataframe" taggedAs Slow in {

    val filepath = getClass.getResource("/sample_songs.csv").getPath

    val try_processed_df = for(df <- DataUtils.loadCsv(filepath, spark);
                               pdf <- FitModel.columnProcessing(df)) yield pdf

    try_processed_df should matchPattern {
      case Success(_) =>
    }

    val processed_df = try_processed_df.get

    val popular_songs_num = processed_df.select(count(when(processed_df("label")===1, 1))).first().getLong(0)
    popular_songs_num shouldBe 454

    val columns_num = processed_df.columns.length
    columns_num shouldBe 27

    val assemble_scale_pipeline_model = assembleScalePipeline(processed_df).fit(processed_df)
    val transformed_df = assemble_scale_pipeline_model.transform(processed_df)
    val features_num = transformed_df.select("features").take(1)(0)(0).toString.split(",").length
    features_num shouldBe 18
  }

  it should "successfully train and predict" taggedAs Slow in {
    val filepath = getClass.getResource(SAMPLE_SONGS_FILEPATH).getPath

    val try_models = for (
      raw_df <- DataUtils.loadCsv(filepath, spark);
      processed_df <- FitModel.columnProcessing(raw_df)
    ) yield (
      FitModel.fit(processed_df, FitModel.ModelName_LR, evaluate = true),
      FitModel.fit(processed_df, FitModel.ModelName_RF, evaluate = true)
    )

    try_models should matchPattern {
      case Success((Success(_), Success(_))) =>
    }

    try_models match {
      case Success((lrTry, rfTry)) => (lrTry, rfTry) match {
        case (Success(lr: PipelineModel), Success(rf: PipelineModel)) =>
          val df = FitModel.columnProcessing(DataUtils.loadCsv(filepath, spark).get.limit(100)).get
          lr.transform(df).select("prediction")
            .head(3).map(r => r.getDouble(0)) shouldBe Array(1.0, 1.0, 0.0)
          rf.transform(df).select("prediction")
            .head(3).map(r => r.getDouble(0)) shouldBe Array(1.0, 1.0, 0.0)
      }
    }
  }
}
