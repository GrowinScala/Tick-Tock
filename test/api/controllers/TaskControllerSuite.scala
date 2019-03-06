package api.controllers

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import api.dtos.TaskDTO
import api.services.SchedulingType
import api.utils.DateUtils.stringToDateFormat
import api.utils.{FakeUUIDGenerator, UUIDGenerator}
import com.google.inject.Guice
import database.repositories.{FakeFileRepository, FakeTaskRepository, FileRepository, TaskRepository}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Mode
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._

import scala.concurrent.{ExecutionContext, Future}

class TaskControllerSuite extends PlaySpec with Results with GuiceOneAppPerSuite with BeforeAndAfterAll with BeforeAndAfterEach{

  lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
  lazy val injector: Injector = appBuilder.injector()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val fileRepo: FileRepository = new FakeFileRepository
  implicit val taskRepo: TaskRepository = new FakeTaskRepository
  implicit val UUIDGen: UUIDGenerator = new FakeUUIDGenerator
  val cc: ControllerComponents = injector.instanceOf[ControllerComponents]
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val mat: Materializer = ActorMaterializer()

  val LOCALHOST = "localhost:9000"

  override def beforeAll(): Unit = {

  }

  override def beforeEach(): Unit = {

  }

  override def afterAll(): Unit = {

  }

  override def afterEach(): Unit = {

  }

  "TaskController#schedule (POST /task)" should {
    "should be valid in" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST, CONTENT_TYPE -> "application/json")
        .withBody(Json.parse("""
          {
            "startDateAndTime": "2020-07-01 00:00:00",
            "fileName": "test1",
            "taskType": "RunOnce"
          }
        """))

      val taskController = new TaskController(cc)
      val result = taskController.schedule.apply(fakeRequest)
      val bodyText = contentAsString(result)
      bodyText mustBe "Task received."
    }
  }

  "TaskController#getSchedule (GET /task)" should {
    "should be valid in" in {
      val fakeRequest = FakeRequest(GET, s"/task")
        .withHeaders(HOST -> LOCALHOST)
      val taskController = new TaskController(cc)
      val result = taskController.getSchedule.apply(fakeRequest)
      val bodyText = contentAsString(result)
      bodyText mustBe "[{\"taskId\":\"asd1\",\"fileName\":\"test1\",\"taskType\":\"RunOnce\",\"startDateAndTime\":1893499200000}," +
        "{\"taskId\":\"asd2\",\"fileName\":\"test2\",\"taskType\":\"Periodic\",\"startDateAndTime\":1893499200000,\"periodType\":\"Minutely\",\"period\":2,\"endDateAndTime\":2524651200000}," +
        "{\"taskId\":\"asd3\",\"fileName\":\"test3\",\"taskType\":\"Periodic\",\"startDateAndTime\":1893499200000,\"periodType\":\"Hourly\",\"period\":1,\"totalOccurrences\":5,\"currentOccurrences\":5}]"
    }
  }

  "TaskController#getScheduleById (GET /task/:id)" should {
    "should be valid in" in {
      val id = "asd1"
      val fakeRequest = FakeRequest(GET, s"/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
      val taskController = new TaskController(cc)
      val result = taskController.getScheduleById(id).apply(fakeRequest)
      val bodyText = contentAsString(result)
      bodyText mustBe "{\"taskId\":\"asd1\",\"fileName\":\"test1\",\"taskType\":\"RunOnce\",\"startDateAndTime\":1893499200000}"
    }
  }

  "TaskController#updateTask (PATCH /task/:id" should {
    "should be valid in" in {
      val id = "asd1"
      val fakeRequest = FakeRequest(PATCH, s"/task/" + id)
        .withHeaders(HOST -> LOCALHOST, CONTENT_TYPE -> "application/json")
        .withBody(Json.parse("""
          {
            "startDateAndTime": "2020-07-01 00:00:00",
            "fileName": "test1",
            "taskType": "RunOnce"
          }
        """))
      val taskController = new TaskController(cc)
      val result = taskController.updateTask(id).apply(fakeRequest)
      val bodyText = contentAsString(result)
      bodyText mustBe "Task received."
    }
  }

  "TaskController#replaceTask (PUT /task/:id)" should {
    "should be valid in" in {
      val id = "asd1"
      val fakeRequest = FakeRequest(PUT, s"/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
        .withBody(Json.parse("""
          {
            "startDateAndTime": "2020-07-01 00:00:00",
            "fileName": "test1",
            "taskType": "RunOnce"
          }
        """))
      val taskController = new TaskController(cc)
      val result = taskController.replaceTask(id).apply(fakeRequest)
      val bodyText = contentAsString(result)
      bodyText mustBe "Task received."
    }
  }


  /*"FileController#getAllFiles" should {
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
      bodyText mustBe "File with id = asd1 as been deleted."
    }
  }*/

  "TaskController#getSchedule (GET /task)" should {
    val fakeRequest = FakeRequest(GET, s"/task")
      .withHeaders(HOST -> "localhost:9000")
    val fileController = new FileController(cc)
    val result = fileController.getAllFiles.apply(fakeRequest)
    val bodyText = contentAsString(result)
  }

  "TaskController#getScheduleById (GET /task/:id)" should {
    val fakeRequest = FakeRequest(GET, s"/task/asd1")
      .withHeaders(HOST -> "localhost:9000")
    val fileController = new FileController(cc)
    val result = fileController.getAllFiles.apply(fakeRequest)
    val bodyText = contentAsString(result)
  }

  "TaskController#replaceTask (PUT /task/:id)" should {
    val fakeRequest = FakeRequest(GET, s"/task")
      .withHeaders(HOST -> "localhost:9000")
    val fileController = new FileController(cc)
    val result = fileController.getAllFiles.apply(fakeRequest)
    val bodyText = contentAsString(result)
  }
}
