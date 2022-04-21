package spark

import org.apache.spark.ml.classification.{LogisticRegressionModel, RandomForestClassificationModel}
import org.apache.spark.sql.SparkSession
import play.api.{Configuration, Logger, Logging}

import javax.inject._

/**
 * Spark context container for dependency injection.
 * Initialize a spark context when starting the application, and can be injected later in controllers
 */
@Singleton
class SparkContainer @Inject()(config: Configuration) extends Logging {

  // -------- constants --------
  val LRModelPath: String = config.get[String]("spark.LRModelPath")
  val RFModelPath: String = config.get[String]("spark.RFModelPath")

  val IsLocal: Boolean = config.get[Boolean]("spark.isLocal")


  // -------- session --------
  // TODO - To be configured for cluster
  def getSession: SparkSession = {
    if (IsLocal) sparkLocal else ???
  }

  private val sparkLocal: SparkSession = SparkSession
    .builder()
    .appName("test")
    .master("local[*]")
    .getOrCreate()


  // -------- models --------
  val lrModelOpt: Option[LogisticRegressionModel] =
    if (checkModelExist(LRModelPath))
      Some(LogisticRegressionModel.load(LRModelPath))
    else None

  val rfModelOpt: Option[RandomForestClassificationModel] =
    if (checkModelExist(RFModelPath))
      Some(RandomForestClassificationModel.load(RFModelPath))
    else None


  // -------- utils --------
  private def checkModelExist(path: String): Boolean = new java.io.File(path).exists
}
