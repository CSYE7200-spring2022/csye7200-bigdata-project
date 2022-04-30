import org.scalajs.dom
import org.scalajs.dom.{Headers, HttpMethod, RequestInit, document}

import java.io.{File, FileInputStream}
import scala.scalajs.js.Thenable.Implicits.thenable2future
import org.scalajs.dom.ext.Ajax

import scala.scalajs.js.JSON
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

object MppApp {
    def appendPar(targetNode: dom.Node, text: String): Unit = {
        val parNode = document.createElement("p")
        parNode.textContent = text
        targetNode.appendChild(parNode)
    }

    def appendBtn(targetNode: dom.Node): Unit = {
        val btn = document.createElement("button")
        btn.textContent = "AHAHA"
        btn.addEventListener("click", { (e: dom.MouseEvent) => {
            addClickedMessage()
        }
        })
        targetNode.appendChild(btn)
    }

    @JSExportTopLevel("addClickedMessage")
    def addClickedMessage(): Unit = {
        appendPar(document.body, "You clicked the button!")
    }

    def singleMusicInference(targetNode: dom.Node): Unit = {
        val subtitle = document.createElement("h2")
        subtitle.textContent = "Single Song Inference"
    }

    def setupUI(): Unit = {
//        val title = document.createElement("h1")
//        title.textContent = "Music Popularity Prediction"
//        document.body.appendChild(title)
//
//        val div1 = document.createElement("div")
//        singleMusicInference(div1)
//        document.body.appendChild(div1)
//
//
//        appendPar(document.body, "haha")
//        appendBtn(document.body)
    }



    def main(args: Array[String]): Unit = {
        document.addEventListener("DOMContentLoaded", { (e: dom.Event) => setupUI() })

        Async.fromFuture
        Ajax.post(
            url = "127.0.0.1:9000/spark/infer_batch/lr",
            data = JSON.stringify(""),
            headers = Map("Content-Type" -> "application/json"))


    }
}
