package edu.neu.csye7200

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.{expr, when}
import org.apache.spark.sql.types.{DoubleType, IntegerType, StringType, StructField, StructType}
import org.apache.spark.ml.feature.{StandardScaler, VectorAssembler}
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.ml.classification.{LogisticRegression, RandomForestClassifier}

import scala.util.Try

object FitModel{

  // *** This can be removed after the pipeline is setting up ***
  def load_csv(filepath: String): Try[DataFrame] = Try{
    val my_schema = StructType(List(
      StructField("song_hotness", DoubleType),
      StructField("artist_familiarity", DoubleType),
      StructField("artist_hotttnesss", DoubleType),
      StructField("artist_id", StringType),
      StructField("artist_latitude", DoubleType),
      StructField("artist_location", StringType),
      StructField("artist_longitude", DoubleType),
      StructField("artist_name", StringType),
      StructField("title", StringType),
      StructField("danceability", DoubleType),
      StructField("duration", DoubleType),
      StructField("end_of_fade_in", DoubleType),
      StructField("energy", DoubleType),
      StructField("key", DoubleType),
      StructField("key_confidence", DoubleType),
      StructField("loudness", DoubleType),
      StructField("mode", DoubleType),
      StructField("mode_confidence", DoubleType),
      StructField("start_of_fade_out", DoubleType),
      StructField("tempo", DoubleType),
      StructField("time_signature", DoubleType),
      StructField("time_signature_confidence", DoubleType),
      StructField("artist_terms", StringType),
      StructField("artist_terms_freq", StringType),
      StructField("artist_terms_weight", StringType),
      StructField("year", IntegerType)))

    val spark: SparkSession = SparkSession
      .builder()
      .appName("test")
      .master("local[*]")
      .getOrCreate()

    spark.sparkContext.setLogLevel("ERROR")
    spark.read
      .option("delimiter", ",")
      .schema(my_schema)
      .csv(filepath)
  }

  def preprocessing(df: DataFrame): Try[DataFrame] = Try{
//    println("Raw data size: " + df.count())

    // drop songs before 1920 and those with nan values
    val df_1 = df.filter(df("year") > 1920)
      .na.drop(List("artist_latitude","artist_longitude"))

    val avg_hotness = df_1.select(expr("AVG(song_hotness)"))
      .collect().head.getDouble(0)
    val min_year = df_1.select(expr("MIN(year)"))
      .collect().head.getInt(0)
//    println("average song hotness: " + avg_hotness)

    // set training label & shift years
    val df_2 = df_1.withColumn("label", when(df_1("song_hotness") >= avg_hotness, 1).otherwise(0))
      .withColumn("year", df("year") - min_year)

//    // view the number of popular/unpopular songs
//    df_2.select( count(when(df_2("label")===1, 1)).alias("num of popular songs"),
//      count(when(df_2("label")===0, 1)).alias("num of unpopular songs")).show()

    // set up feature vector assembler and transform df_2
    val features_df = new VectorAssembler()
      .setInputCols(for (col_type <- df_2.dtypes.filter(x => x._1 != "song_hotness" && x._2 == "DoubleType"))
                    yield col_type._1)
      .setOutputCol("raw_features")
      .transform(df_2)

    // scaled_feature_df
    new StandardScaler()
      .setInputCol("raw_features")
      .setOutputCol("features")  // MUST set here "features", Model will find this col by specific name to train
      .fit(features_df)
      .transform(features_df)
  }

  def fit(df: DataFrame): Try[Array[Any]] = Try {
    val data_split = df.randomSplit(Array(0.8, 0.2), seed = 11L)
    val train_set = data_split(0).cache()
    val test_set = data_split(1)

    val evaluator = new BinaryClassificationEvaluator()
      .setLabelCol("label")
      .setRawPredictionCol("rawPrediction")
      .setMetricName("areaUnderROC")

    println("\nFitting with LogisticRegression:")
    val lr_model = new LogisticRegression().fit(train_set)
    val lr_train_predictions = lr_model.transform(train_set)
    val lr_test_predictions = lr_model.transform(test_set)
    println("\tAccuracy on train set: " + evaluator.evaluate(lr_train_predictions))
    println("\tAccuracy on test set: " + evaluator.evaluate(lr_test_predictions))

    println("\nFitting with LogisticRegression:")
    val rf_model = new RandomForestClassifier().fit(train_set)
    val rf_train_predictions = rf_model.transform(train_set)
    val rf_test_predictions = rf_model.transform(test_set)
    println("\tAccuracy on train set: " + evaluator.evaluate(rf_train_predictions))
    println("\tAccuracy on test set: " + evaluator.evaluate(rf_test_predictions))

    Array(lr_model, rf_model)
  }

}
