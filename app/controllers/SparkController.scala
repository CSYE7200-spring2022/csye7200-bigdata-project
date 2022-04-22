package controllers

import org.apache.spark.ml.{PipelineModel, Transformer}
import org.apache.spark.ml.classification.{LogisticRegression, LogisticRegressionModel, RandomForestClassificationModel, RandomForestClassifier}
import play.api.mvc._
import play.api._
import play.api.libs.json._
import spark.{FitModel, SparkContainer}

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

    Future(List(FitModel.ModelName_LR, FitModel.ModelName_RF)
      .map(FitModel.fit(FitModel.data(sparkContainer.getSession).get, _, evaluate = true)))
      .onComplete{
        case Success(List(lrTry, rfTry)) => (lrTry, rfTry) match {
          case (Success(lr), Success(rf)) => {
            // save models to file, need to restart Play! application to load the models
            lr.asInstanceOf[PipelineModel].write.overwrite().save(sparkContainer.LRModelPath)
            rf.asInstanceOf[PipelineModel].write.overwrite().save(sparkContainer.RFModelPath)
            logger.info("Successfully trained the model and saved to files")
          }
          case _ => logger.error("Error while training models!")
        }
        case Failure(exception) => logger.error("Cannot train the model")
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

  def inference(model: Option[Transformer], jsVal: JsValue): Result = model match {
    case Some(model) => {
      val processedDf = FitModel.columnProcessing(
        FitModel.dfFromJson(jsVal, sparkContainer.getSession),
        isTrainData = false
      )

      processedDf match {
        case Success(df) => {
          val prediction = model.transform(df).select("prediction").head().getDouble(0)

          // TODO - to be deleted
          val ddff = model.transform(df)
          println(ddff.head.schema.map(_.name).zip(ddff.head().toSeq))

          Ok(s"result: ${prediction} (${if (prediction == 0) "not popular" else "popular"})")
        }
        case Failure(e) => BadRequest(s"Error processing dataframe: ${e.toString}")
      }
    }
    case None => BadRequest("Model not initialized, please train the model first")
  }
}
