package spark

import org.apache.spark.ml.PipelineModel
import org.apache.spark.sql.SparkSession
import play.api.{Configuration, Logger, Logging}

import javax.inject._
import scala.util.Try

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
    .appName("webapp")
    .master("local[*]")
    .getOrCreate()


  // -------- models --------
  val lrModelOpt: Option[PipelineModel] = Try {
    if (checkModelExist(LRModelPath))
      PipelineModel.load(LRModelPath)
    else throw new Exception("Model files does not exist")
  }.toOption

  val rfModelOpt: Option[PipelineModel] = Try {
    if (checkModelExist(RFModelPath))
      PipelineModel.load(RFModelPath)
    else throw new Exception("Model files does not exist")
  }.toOption


  // -------- utils --------
  private def checkModelExist(path: String): Boolean = new java.io.File(path).exists
}
