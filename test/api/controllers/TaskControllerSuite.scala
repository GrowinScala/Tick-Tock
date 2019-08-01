package api.controllers

import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, Materializer }
import api.dtos.TaskDTO
import api.services.{ PeriodType, SchedulingType }
import api.utils.DateUtils._
import api.utils.{ FakeUUIDGenerator, UUIDGenerator }
import database.repositories.exclusion.ExclusionRepository
import database.repositories.file.FileRepository
import database.repositories.scheduling.SchedulingRepository
import database.repositories.task.TaskRepository
import executionengine.{ ExecutionManager, FakeExecutionManager }
import generators.Generator
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._

import scala.concurrent.{ ExecutionContext, Future }

class TaskControllerSuite extends PlaySpec with Results with GuiceOneAppPerSuite with MockitoSugar {

  private lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
  private lazy val injector: Injector = appBuilder.injector()
  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  private implicit val fileRepo: FileRepository = mock[FileRepository]
  private implicit val taskRepo: TaskRepository = mock[TaskRepository]
  private implicit val exclusionRepo: ExclusionRepository = mock[ExclusionRepository]
  private implicit val schedulingRepo: SchedulingRepository = mock[SchedulingRepository]
  private implicit val UUIDGen: UUIDGenerator = new FakeUUIDGenerator
  private implicit val executionManager: ExecutionManager = new FakeExecutionManager
  private val cc: ControllerComponents = injector.instanceOf[ControllerComponents]
  private implicit val actorSystem: ActorSystem = ActorSystem()
  private implicit val mat: Materializer = ActorMaterializer()

  private val LOCALHOST = "localhost:9000"

  private val gen: Generator = new Generator

  private val id1: String = gen.id
  private val id2: String = gen.id
  private val id3: String = gen.id

  private val fileName1: String = gen.fileName
  private val fileName2: String = gen.fileName
  private val fileName3: String = gen.fileName

  private val task1 = TaskDTO(id1, id2, SchedulingType.RunOnce, Some(stringToDateFormat("01-01-2030 12:00:00", "dd-MM-yyyy HH:mm:ss")))
  private val task2 = TaskDTO(id3, fileName1, SchedulingType.Periodic, Some(stringToDateFormat("01-01-2030 12:00:00", "dd-MM-yyyy HH:mm:ss")), Some(PeriodType.Minutely), Some(2), Some(stringToDateFormat("01-01-2050 12:00:00", "dd-MM-yyyy HH:mm:ss")))
  private val task3 = TaskDTO(fileName2, fileName3, SchedulingType.Periodic, Some(stringToDateFormat("01-01-2030 12:00:00", "dd-MM-yyyy HH:mm:ss")), Some(PeriodType.Hourly), Some(1), None, Some(5), Some(5))
  private val seqTasks = Seq(task1, task2, task3)

  when(fileRepo.existsCorrespondingFileName(fileName1)).thenReturn(Future.successful(true))
  when(fileRepo.selectFileIdFromFileName(fileName1)).thenReturn(Future.successful(id1))

  when(exclusionRepo.insertInExclusionsTable(any)).thenReturn(Future.successful(true))

  when(schedulingRepo.insertInSchedulingsTable(any)).thenReturn(Future.successful(true))

  when(taskRepo.insertInTasksTable(any)).thenReturn(Future.successful(true))
  when(taskRepo.selectAllTasks(any, any)).thenReturn(Future.successful(seqTasks))
  when(taskRepo.selectTask(any)).thenReturn(Future.successful(Some(task1)))
  when(taskRepo.updateTaskById(any, any)).thenReturn(Future.successful(true))
  when(taskRepo.deleteTaskById(any)).thenReturn(Future.successful(1))

  "TaskController#schedule (POST /task)" should {
    "be valid in" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST, CONTENT_TYPE -> "application/json")
        .withBody(Json.parse(s"""
          |{
          |  "startDateAndTime": "2020-07-01 00:00:00",
          |  "fileName": "$fileName1",
          |  "taskType": "RunOnce"
          |}
        """.stripMargin))
      val taskController = new TaskController(cc)
      val result = taskController.schedule.apply(fakeRequest)

      status(result) mustBe OK

      //Fake UUID always returns "asd1"
      contentAsString(result) mustBe "Task received => http://" + LOCALHOST + "/task/asd1"
    }

    "be invalid in" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST, CONTENT_TYPE -> "application/json")
        .withBody(Json.parse("""
          {
            "unknownKey":"unknownValue"
          }
        """))
      val taskController = new TaskController(cc)
      val result = taskController.schedule.apply(fakeRequest)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "{\"status\":\"Error:\",\"message\":{\"obj.taskType\":[{\"msg\":[\"error.path.missing\"],\"args\":[]}],\"obj.fileName\":[{\"msg\":[\"error.path.missing\"],\"args\":[]}]}}"

    }
  }

  "TaskController#getSchedule (GET /task)" should {
    "be valid in" in {
      val fakeRequest = FakeRequest(GET, s"/task")
        .withHeaders(HOST -> LOCALHOST)
      val taskController = new TaskController(cc)
      val result = taskController.getSchedule(None, None).apply(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(seqTasks)
    }
  }

  //TODO hmmm
  "TaskController#getScheduleById (GET /task/:id)" should {
    "be valid in" in {
      val fakeRequest = FakeRequest(GET, s"/task/$id1")
        .withHeaders(HOST -> LOCALHOST)
      val taskController = new TaskController(cc)
      val result = taskController.getScheduleById(id1).apply(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(task1)
    }
  }

  "TaskController#updateTask (PATCH /task/:id)" should {
    "be valid in" in {
      val fakeRequest = FakeRequest(PATCH, s"/task/$id1")
        .withHeaders(HOST -> LOCALHOST, CONTENT_TYPE -> "application/json")
        .withBody(Json.parse(s"""
          {
            "toDelete": [],
            "startDateAndTime": "2020-07-01 00:00:00",
            "fileName": "$fileName1",
            "taskType": "RunOnce"
          }
        """))
      val taskController = new TaskController(cc)
      val result = taskController.updateTask(id1).apply(fakeRequest)
      val bodyText = contentAsString(result)
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id1
    }

    "be invalid in" in {
      val fakeRequest = FakeRequest(PATCH, s"/task/$id1")
        .withHeaders(HOST -> LOCALHOST, CONTENT_TYPE -> "application/json")
        .withBody(Json.parse("""
          {
            "taskId":"newUUID"
          }
        """))
      val taskController = new TaskController(cc)
      val result = taskController.updateTask(id1).apply(fakeRequest)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "Error replacing scheduled task : \nList((/toDelete,List(JsonValidationError(List(error.path.missing),WrappedArray()))))"
    }
  }

  "TaskController#deleteTask (DELETE /task/:id)" should {
    "be valid in" in {
      val fakeRequest = FakeRequest(DELETE, s"/task/$id1")
      val taskController = new TaskController(cc)
      val result = taskController.deleteTask(id1).apply(fakeRequest)

      status(result) mustBe NO_CONTENT
    }
  }

  "TaskController#replaceTask (PUT /task/:id)" should {
    "be valid in" in {
      val fakeRequest = FakeRequest(PUT, s"/task/$id1")
        .withHeaders(HOST -> LOCALHOST)
        .withBody(Json.parse(s"""
          {
            "startDateAndTime": "2020-07-01 00:00:00",
            "fileName": "$fileName1",
            "taskType": "RunOnce"
          }
        """))
      val taskController = new TaskController(cc)
      val result = taskController.replaceTask(id1).apply(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe "Task received => http://" + LOCALHOST + "/task/" + id1
    }

    "be invalid in" in {
      val fakeRequest = FakeRequest(PUT, s"/task/$id1")
        .withHeaders(HOST -> LOCALHOST)
        .withBody(Json.parse("""
          {
            "unknownKey":"unknownValue"
          }
        """))
      val taskController = new TaskController(cc)
      val result = taskController.replaceTask(id1).apply(fakeRequest)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "{\"status\":\"Error:\",\"message\":{\"obj.taskType\":[{\"msg\":[\"error.path.missing\"],\"args\":[]}],\"obj.fileName\":[{\"msg\":[\"error.path.missing\"],\"args\":[]}]}}"
    }
  }
}
