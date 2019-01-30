package api.controllers

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import database.repositories.{FakeFileRepository, FakeTaskRepository, FileRepository, TaskRepository}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{ControllerComponents, Results}
import play.api.test.Helpers._
import play.api.test._

import scala.concurrent.ExecutionContext

class FileControllerSuite extends PlaySpec with Results with GuiceOneAppPerSuite with BeforeAndAfterAll with BeforeAndAfterEach{

  lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
  lazy val injector: Injector = appBuilder.injector()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val fileRepo: FileRepository = new FakeFileRepository
  implicit val taskRepo: TaskRepository = new FakeTaskRepository
  val cc: ControllerComponents = injector.instanceOf[ControllerComponents]
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val mat: Materializer = ActorMaterializer()

  "FileController#getAllFiles" should {
    "receive a GET request" in {
      val fakeRequest = FakeRequest(GET, s"/file")
        .withHeaders(HOST -> "localhost:9000")
      val fileController = new FileController(cc)
      val result = fileController.getAllFiles.apply(fakeRequest)
      val bodyText = contentAsString(result)
      bodyText mustBe """[{"fileId":"asd1","fileName":"test1","uploadDate":1514808000000},{"fileId":"asd2","fileName":"test2","uploadDate":1514808000000},{"fileId":"asd3","fileName":"test3","uploadDate":1514808000000}]"""
    }
  }

  "FileController#getFileById" should {
    "receive a GET request." in {
      val fakeRequest = FakeRequest(GET, s"/file/asd1")
        .withHeaders(HOST -> "localhost:9000")
      val fileController = new FileController(cc)
      val result = fileController.getFileById("asd1").apply(fakeRequest)
      val bodyText = contentAsString(result)
      bodyText mustBe """{"fileId":"asd1","fileName":"test1","uploadDate":1514808000000}"""
    }
  }

  "FileController#deleteFile" should {
    "receive a DELETE request." in {
      val fakeRequest = FakeRequest(DELETE, s"/file/asd1")
        .withHeaders(HOST -> "localhost:9000")
      val fileController = new FileController(cc)
      val result = fileController.deleteFile("asd1").apply(fakeRequest)
      val bodyText = contentAsString(result)
      bodyText mustBe "File with id = asd1 has been deleted."
    }
  }
}
