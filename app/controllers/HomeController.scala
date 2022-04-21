package controllers

import play.api.mvc._
import spark.SparkContainer

import javax.inject._

class HomeController @Inject()(val controllerComponents: ControllerComponents, val sparkContainer: SparkContainer) extends BaseController {

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index(""))
  }
}
