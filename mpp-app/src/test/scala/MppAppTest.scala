import utest._
import scala.scalajs.js
import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.ext._

object MppAppTest extends TestSuite {
    // Init App
    MppApp.setupUI()

    def tests: Tests = Tests {
        test("HelloWorld") {
            assert(document.querySelectorAll("p").count(_.textContent == "haha") == 1)
        }
    }
}
