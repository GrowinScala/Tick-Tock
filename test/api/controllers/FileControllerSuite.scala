package api.controllers

import database.repositories.{FakeFileRepository, FakeTaskRepository, FileRepository, TaskRepository}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Mode
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.mvc._
import play.api.test.Helpers._

import scala.concurrent.{ExecutionContext, Future}

class FileControllerSuite extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterAll with BeforeAndAfterEach{

  /*lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().in(Mode.Test)
  lazy val injector: Injector = appBuilder.injector()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val fc: FileRepository = new FakeFileRepository
  implicit val tc: TaskRepository = new FakeTaskRepository
  val cc: ControllerComponents = injector.instanceOf[ControllerComponents]


  override def beforeAll(): Unit = {

  }

  override def beforeEach(): Unit = {

  }

  override def afterAll(): Unit = {

  }

  override def afterEach(): Unit = {

  }


  "FileController#schedule (POST /task)" should {
    "should be valid in" in {
      val fileController = new FileController(cc)
      val result: Future[Result] = fileController.upload().apply(FakeRequest())
      val bodyText = contentAsString(result)
      bodyText mustBe "It works!"
    }
  }*/
}
