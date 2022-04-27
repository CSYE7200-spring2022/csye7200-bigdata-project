package controllers

import org.apache.spark.ml.Transformer
import play.api._
import play.api.libs.Files
import play.api.libs.json._
import play.api.mvc._
import spark.{DataUtils, FitModel, SparkContainer}

import javax.inject._
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.util.{Failure, Success}

class SparkController @Inject()(
  val controllerComponents: ControllerComponents,
  val sparkContainer: SparkContainer,
  val config: Configuration
) extends BaseController with Logging {

  def trainModels: Action[AnyContent] = Action {

    Future(
      List(FitModel.ModelName_LR, FitModel.ModelName_RF)
        .map(FitModel.fit(FitModel.data(sparkContainer.getSession).get, _, evaluate = true))
    ).onComplete{
      case Success(List(lrTry, rfTry)) => (lrTry, rfTry) match {
        case (Success(lr), Success(rf)) => {
          lr.write.overwrite().save(sparkContainer.LRModelPath)
          rf.write.overwrite().save(sparkContainer.RFModelPath)
          sparkContainer.lrModelOpt = Some(lr)
          sparkContainer.rfModelOpt = Some(rf)
          logger.info("Successfully trained the model and saved to files")
        }
        case _ => logger.error("Error while training models!")
      }
      case Failure(exception) => logger.error(s"Cannot train the model: ${exception.getMessage}")
    }

    Ok(views.html.index("Model training initiated!"))
  }


  def inferenceLR: Action[JsValue] = Action(parse.json) { implicit request: Request[JsValue] => {
    val jsVal = request.body
    inference(sparkContainer.lrModelOpt, jsVal)
  }}


  def inferenceRF: Action[JsValue] = Action(parse.json) { implicit request: Request[JsValue] => {
    val jsVal = request.body
    inference(sparkContainer.rfModelOpt, jsVal)
  }}

  def inferenceBatchLR: Action[MultipartFormData[Files.TemporaryFile]] = Action(parse.multipartFormData) { implicit request: => {
    request.body.file("csv").map { csv =>
      import java.io.File
      val filename = csv.filename
      val contentType = csv.contentType
      csv.ref.moveTo(new File("/tmp/batch.csv"))
      inferenceBatch(sparkContainer.lrModelOpt, "/tmp/batch.csv")
      Ok("File uploaded")
    }.getOrElse {
      BadRequest("Missing file!")
    }
  }}

  def inferenceBatchRF: Action[MultipartFormData[Files.TemporaryFile]] = Action(parse.multipartFormData) { implicit request: => {
    request.body.file("csv").map { csv =>
      import java.io.File
      val filename = csv.filename
      val contentType = csv.contentType
      csv.ref.moveTo(new File("/tmp/batch.csv"))
      inferenceBatch(sparkContainer.rfModelOpt, "/tmp/batch.csv")
      Ok("File uploaded")
    }.getOrElse {
      BadRequest("Missing file!")
    }
  }}


  def inference(model: Option[Transformer], jsVal: JsValue): Result = model match {
    case Some(model) => {
      val processedDf = FitModel.columnProcessing(
        DataUtils.dfFromJson(jsVal, sparkContainer.getSession),
        isTrainData = false
      )

      processedDf match {
        case Success(df) => {
          val prediction = model.transform(df).select("prediction").head().getDouble(0)

          Ok(s"result: ${prediction} (${if (prediction == 0) "not popular" else "popular"})")
        }
        case Failure(e) => BadRequest(s"Error processing dataframe: ${e.getMessage}")
      }
    }
    case None => BadRequest("Model not initialized, please train the model first")
  }

  def inferenceBatch(model: Option[Transformer], filepath: String): Result = model match {
    case Some(model) => {
      val processedDf = FitModel.columnProcessing(
        DataUtils.loadCsv(filepath, sparkContainer.getSession).get,
        isTrainData = false
      )

      processedDf match {
        case Success(df) => {
          val prediction = model.transform(df).select("prediction").head().getDouble(0)

          Ok(s"result: ${prediction} (${if (prediction == 0) "not popular" else "popular"})")
        }
        case Failure(e) => BadRequest(s"Error processing dataframe: ${e.getMessage}")
      }
    }
    case None => BadRequest("Model not initialized, please train the model first")
  }


}
