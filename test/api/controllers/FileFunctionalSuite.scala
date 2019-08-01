package api.controllers

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, Materializer }
import api.dtos.FileDTO
import api.utils.DateUtils.stringToDateFormat
import api.validators.Error._
import com.google.inject.Guice
import database.mappings.FileMappings._
import database.repositories.file.FileRepository
import database.repositories.task.TaskRepository
import executionengine.ExecutionManager
import generators.Generator
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._
import slick.jdbc.H2Profile.api._

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext }

class FileFunctionalSuite extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterAll with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  private lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
  Guice.createInjector(appBuilder.applicationModule).injectMembers(this)
  private implicit val fileRepo: FileRepository = appBuilder.injector.instanceOf[FileRepository]
  private implicit val taskRepo: TaskRepository = appBuilder.injector.instanceOf[TaskRepository]
  private implicit val executionManager: ExecutionManager = appBuilder.injector.instanceOf[ExecutionManager]
  private val dtbase: Database = appBuilder.injector.instanceOf[Database]
  private implicit val actorSystem: ActorSystem = ActorSystem()
  private implicit val mat: Materializer = ActorMaterializer()

  private val LOCALHOST = "localhost:9000"

  private val gen: Generator = new Generator

  private val fileUUID1: String = gen.id
  private val fileUUID2: String = gen.id
  private val fileUUID3: String = gen.id
  private val fileUUID4: String = gen.id

  private val fileName1: String = gen.fileName
  private val fileName2: String = gen.fileName
  private val fileName3: String = gen.fileName
  private val fileName4: String = gen.fileName

  private val file1 = FileDTO(fileUUID1, fileName1, stringToDateFormat("01-01-2018 12:00:00", "dd-MM-yyyy HH:mm:ss"))
  private val file2 = FileDTO(fileUUID2, fileName2, stringToDateFormat("01-02-2018 12:00:00", "dd-MM-yyyy HH:mm:ss"))
  private val file3 = FileDTO(fileUUID3, fileName3, stringToDateFormat("01-03-2018 12:00:00", "dd-MM-yyyy HH:mm:ss"))
  private val file4 = FileDTO(fileUUID4, fileName4, stringToDateFormat("01-04-2018 12:00:00", "dd-MM-yyyy HH:mm:ss"))
  private val seqFiles = Seq(file1, file2, file3, file4)

  override def beforeAll: Unit = {
    Await.result(dtbase.run(createFilesTableAction), Duration.Inf)
    Await.result(fileRepo.insertInFilesTable(file1), Duration.Inf)
    Await.result(fileRepo.insertInFilesTable(file2), Duration.Inf)
    Await.result(fileRepo.insertInFilesTable(file3), Duration.Inf)
    Await.result(fileRepo.insertInFilesTable(file4), Duration.Inf)
  }

  override def afterAll: Unit = {
    Await.result(dtbase.run(dropFilesTableAction), Duration.Inf)
  }

  "FileController#GETfile" should {
    "receive a GET request" in {
      val fakeRequest = FakeRequest(GET, s"/file")
        .withHeaders(HOST -> "localhost:9000")
      val result = route(app, fakeRequest).get

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(seqFiles)
    }
  }

  "FileController#GETfileWithId" should {
    "receive a GET request with a valid id" in {
      val fakeRequest = FakeRequest(GET, s"/file/$fileUUID2")
        .withHeaders(HOST -> "localhost:9000")
      val result = route(app, fakeRequest).get

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(file2)
    }

    "receive a GET request with an invalid id" in {
      val fakeRequest = FakeRequest(GET, s"/file/" + gen.id)
        .withHeaders(HOST -> "localhost:9000")
      val result = route(app, fakeRequest).get
      val bodyText = contentAsJson(result)

      status(result) mustBe BAD_REQUEST
      bodyText mustBe Json.toJsObject(invalidFileName)
    }
  }

  "FileController#DELETEfileWithId" should {
    "receive a DELETE request with a valid id" in {
      val fakeRequest = FakeRequest(DELETE, s"/file/$fileUUID4")
        .withHeaders(HOST -> "localhost:9000")
      val result = route(app, fakeRequest).get

      status(result) mustBe NO_CONTENT
    }

    "receive a DELETE request with an invalid id" in {
      val fakeRequest = FakeRequest(DELETE, s"/file/" + gen.id)
        .withHeaders(HOST -> "localhost:9000")
      val result = route(app, fakeRequest).get

      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.toJsObject(invalidEndpointId)
    }
  }
}
