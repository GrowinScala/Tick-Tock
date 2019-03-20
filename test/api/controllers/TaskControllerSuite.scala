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

class TaskControllerSuite extends PlaySpec with Results with GuiceOneAppPerSuite{

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

  "TaskController#schedule (POST /task)" should {
    "be valid in" in {
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
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/asd1"
    }
  }

  "TaskController#getSchedule (GET /task)" should {
    "be valid in" in {
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
    "be valid in" in {
      val id = "asd1"
      val fakeRequest = FakeRequest(GET, s"/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
      val taskController = new TaskController(cc)
      val result = taskController.getScheduleById(id).apply(fakeRequest)
      val bodyText = contentAsString(result)
      bodyText mustBe "{\"taskId\":\"asd1\",\"fileName\":\"test1\",\"taskType\":\"RunOnce\",\"startDateAndTime\":1893499200000}"
    }
  }

  "TaskController#updateTask (PATCH /task/:id)" should {
    "be valid in" in {
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
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
    }
  }

  "TaskController#deleteTask (DELETE /task/:id)" should {
    "be valid in" in {
      val id = "asd1"
      val fakeRequest = FakeRequest(DELETE, s"/task/" + id)
      val taskController = new TaskController(cc)
      val result = taskController.deleteTask(id).apply(fakeRequest)
      val bodyText = contentAsString(result)
      bodyText mustBe ""
    }
  }

  "TaskController#replaceTask (PUT /task/:id)" should {
    "be valid in" in {
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
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
    }
  }
}
