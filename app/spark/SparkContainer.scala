package spark

import org.apache.spark.ml.PipelineModel
import org.apache.spark.sql.SparkSession
import play.api.{Configuration, Logging}

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
  val MasterIp: String = config.get[String]("spark.masterIp")
  val ClusterExecutorMem: String = config.get[String]("spark.executorMem")


  // -------- session --------
  val getSession: SparkSession = {
    if (IsLocal) sparkLocal else sparkCluster
  }

  private def sparkLocal: SparkSession = SparkSession
    .builder()
    .appName("webapp")
    .master("local[*]")
    .getOrCreate()

  private def sparkCluster: SparkSession = SparkSession
    .builder()
    .appName("SparkApp")
    .master(s"spark://$MasterIp:7077")
    .config("spark.submit.deployMode","cluster")
    .config("spark.executor.memory", ClusterExecutorMem)
    .getOrCreate()

  // -------- models --------
  // TODO - use val instead of val
  var lrModelOpt: Option[PipelineModel] = Try {
    if (checkModelExist(LRModelPath))
      PipelineModel.load(LRModelPath)
    else throw new Exception("Model files does not exist")
  }.toOption

  var rfModelOpt: Option[PipelineModel] = Try {
    if (checkModelExist(RFModelPath))
      PipelineModel.load(RFModelPath)
    else throw new Exception("Model files does not exist")
  }.toOption


  // -------- utils --------
  private def checkModelExist(path: String): Boolean = new java.io.File(path).exists
}
