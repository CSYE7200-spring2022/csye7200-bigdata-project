package edu.neu.csye7200

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class HelloWorldTest extends AnyFlatSpec with Matchers {

  behavior of "helloWorld test function"
  it should "return value 5" in {
    HelloWorld.testOutput shouldBe 5
  }
}
