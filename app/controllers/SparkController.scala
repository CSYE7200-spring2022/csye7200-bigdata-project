package controllers

import org.apache.spark.ml.{PipelineModel, Transformer}
import play.api._
import play.api.libs.Files
import play.api.libs.json._
import play.api.mvc._
import spark.{DataUtils, FitModel, SparkContainer}

import javax.inject._
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

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
        case (Success(lr), Success(rf)) =>
          lr.write.overwrite().save(sparkContainer.LRModelPath)
          rf.write.overwrite().save(sparkContainer.RFModelPath)
          sparkContainer.lrModelOpt = Some(lr)
          sparkContainer.rfModelOpt = Some(rf)
          logger.info("Successfully trained the model and saved to files")

        case _ => logger.error("Error while training models!")
      }
      case Failure(exception) => logger.error(s"Cannot train the model: ${exception.getMessage}")
    }

    Ok(views.html.index("Model training initiated!"))
  }

  def inferenceLR: Action[JsValue] = Action(parse.json) { implicit request: Request[JsValue] => {
    val jsVal = request.body
    inference(sparkContainer.lrModelOpt, jsVal) match {
      case Success(result) => result
      case Failure(e) => BadRequest(s"Error processing request: ${e.getMessage}")
    }
  }}

  def inferenceRF: Action[JsValue] = Action(parse.json) { implicit request: Request[JsValue] => {
    val jsVal = request.body
    inference(sparkContainer.rfModelOpt, jsVal)  match {
      case Success(result) => result
      case Failure(e) => BadRequest(s"Error processing request: ${e.getMessage}")
    }
  }}

  def inferenceBatchLR: Action[MultipartFormData[Files.TemporaryFile]] =
    Action(parse.multipartFormData) { implicit request => {
      request.body.file("csv").map {
        csv => inferenceCsv(csv, sparkContainer.lrModelOpt)
      }.getOrElse {
        BadRequest("Missing file!")
      }
    }}

  def inferenceBatchRF: Action[MultipartFormData[Files.TemporaryFile]] =
    Action(parse.multipartFormData) { implicit request => {
    request.body.file("csv").map {
      csv => inferenceCsv(csv, sparkContainer.rfModelOpt)
    }.getOrElse {
      BadRequest("Missing file!")
    }
  }}

  def inference(model: Option[Transformer], jsVal: JsValue): Try[Result] = model match {
    case Some(model) =>
      for(raw_df <- DataUtils.dfFromJson(jsVal, sparkContainer.getSession))
      yield FitModel.columnProcessing(raw_df, isTrainData = false)
      match {
        case Success(df) =>
          val prediction = model.transform(df).select("prediction").head().getDouble(0)
          Ok(s"result: $prediction (${if (prediction == 0) "not popular" else "popular"})")

        case Failure(e) => BadRequest(s"Error processing dataframe: ${e.getMessage}")
      }
    case None => Try(BadRequest("Model not initialized, please train the model first"))
  }

  def inferenceCsv(csv: MultipartFormData.FilePart[Files.TemporaryFile], modelOpt: Option[PipelineModel]): Result ={
    import java.io.File
    val temp_f = new File("batch.csv")
    if(temp_f.exists())  temp_f.delete()

    csv.ref.moveTo(temp_f)
    inferenceBatch(modelOpt, "batch.csv") match {
      case Success(result) => result
      case Failure(e) => BadRequest(s"Error processing request: ${e.getMessage}")
    }
  }

  def inferenceBatch(model: Option[Transformer], filepath: String): Try[Result] = model match {
    case Some(model) =>
      for(raw_df <- DataUtils.loadCsv(filepath, sparkContainer.getSession))
        yield FitModel.columnProcessing(raw_df, isTrainData = false)
      match {
        case Success(df) =>
          logger.info(s"Raw Batch size = ${df.count().toInt}")
          val predictionDF = model.transform(df).select("prediction")
          val record_num = predictionDF.count().toInt
          val prediction = predictionDF.head(record_num)
            .map(r => r.getDouble(0))
            .map(p => if(p == 0) "not popular" else "popular")

          Ok(s"Batch size: $record_num\nresult: ${prediction.mkString("[\n", "\n ", "\n]")}")
        case Failure(e) => BadRequest(s"Error processing dataframe: ${e.getMessage}")
      }
    case None => Try(BadRequest("Model not initialized, please train the model first"))
  }

}
