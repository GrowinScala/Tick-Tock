package api.controllers

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import api.dtos.{FileDTO, TaskDTO}
import api.services.{PeriodType, SchedulingType}
import database.repositories.{FileRepository, FileRepositoryImpl, TaskRepository, TaskRepositoryImpl}
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Application, Mode}
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import api.utils.DateUtils._
import play.api.test.FakeRequest
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import api.validators.Error._
import play.api.libs.json.JsArray

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import com.google.inject.{AbstractModule, Guice}
import database.utils.DatabaseUtils.TEST_DB
import play.api.test.Helpers._
import slick.jdbc.meta.MTable


class TaskFunctionalSuite extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterAll with BeforeAndAfterEach{

  /**
    * new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    * new SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
    * new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
    * new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
    */

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
  Guice.createInjector(appBuilder.applicationModule).injectMembers(this)
  implicit val fileRepo: FileRepository = appBuilder.injector.instanceOf[FileRepository]
  implicit val taskRepo: TaskRepository = appBuilder.injector.instanceOf[TaskRepository]
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val mat: Materializer = ActorMaterializer()

  val LOCALHOST = "localhost:9000"

  val taskUUID1: String = UUID.randomUUID().toString
  val taskUUID2: String = UUID.randomUUID().toString
  val taskUUID3: String = UUID.randomUUID().toString
  val taskUUID4: String = UUID.randomUUID().toString

  val fileUUID1: String = UUID.randomUUID().toString
  val fileUUID2: String = UUID.randomUUID().toString
  val fileUUID3: String = UUID.randomUUID().toString
  val fileUUID4: String = UUID.randomUUID().toString

  override def beforeAll = {
    val result = for {
      _ <- fileRepo.createFilesTable
      _ <- fileRepo.insertInFilesTable(FileDTO(fileUUID1, "test1", getCurrentDateTimestamp))
      _ <- fileRepo.insertInFilesTable(FileDTO(fileUUID2, "test2", getCurrentDateTimestamp))
      _ <- fileRepo.insertInFilesTable(FileDTO(fileUUID3, "test3", getCurrentDateTimestamp))
      res <- fileRepo.insertInFilesTable(FileDTO(fileUUID4, "test4", getCurrentDateTimestamp))
    } yield res
    Await.result(result, Duration.Inf)
    Await.result(taskRepo.createTasksTable, Duration.Inf)
    println(Await.result(TEST_DB.run(MTable.getTables), Duration.Inf))
    println(Await.result(fileRepo.selectAllFiles, Duration.Inf))
  }

  override def beforeEach = {

  }

  override def afterAll = {
    Await.result(taskRepo.dropTasksTable, Duration.Inf)
    Await.result(fileRepo.dropFilesTable, Duration.Inf)
  }

  override def afterEach = {
    Await.result(taskRepo.deleteAllTasks, Duration.Inf)
  }

  "GET /" should {
    "receive a GET request"  in {
      val fakeRequest = FakeRequest(GET, "/")
        .withHeaders(HOST -> LOCALHOST)

      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }
  }

  "POST /task" should {
    "receive a POST request with a JSON body with the correct data and insert it into the database. (no date)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test4",
            "taskType": "RunOnce"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
    }

    "receive a POST request with a JSON body with the correct data and insert it into the database. (yyyy-MM-dd HH:mm:ss date format)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "RunOnce",
            "startDateAndTime": "2019-07-01 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."

    }

    "receive a POST request with a JSON body with the correct data and insert it into the database. (dd-MM-yyyy HH:mm:ss date format)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test2",
            "taskType": "RunOnce",
            "startDateAndTime": "01-07-2019 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
    }

    "receive a POST request with a JSON body with the correct data and insert it into the database. (yyyy/MM/dd HH:mm:ss date format)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test3",
            "taskType": "RunOnce",
            "startDateAndTime": "2019/07/01 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."

    }

    "receive a POST request with a JSON body with the correct data and insert it into the database. (dd/MM/yyyy HH:mm:ss date format)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test4",
            "taskType": "RunOnce",
            "startDateAndTime": "01/07/2019 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
    }

    "receive a POST request with a JSON body with the correct data and insert it into the database. (max delay exceeded)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "RunOnce",
            "startDateAndTime": "2030-01-01 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
    }

    "receive a POST request with a JSON body with incorrect data. (wrong file name)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "Unknown",
            "taskType": "RunOnce",
            "startDateAndTime": "2019-07-01 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
      bodyText mustBe "[" + Json.toJsObject(invalidFileName).toString + "]"
    }

    "receive a POST request with a JSON body with incorrect data. (wrong date format)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test2",
            "taskType": "RunOnce",
            "startDateAndTime": "01:07:2019 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
      bodyText mustBe "[" + Json.toJsObject(invalidStartDateFormat).toString + "]"
    }

    "receive a POST request with a JSON body with incorrect data. (wrong date values)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test3",
            "taskType": "RunOnce",
            "startDateAndTime": "2019-14-01 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
      bodyText mustBe "[" + Json.toJsObject(invalidStartDateFormat).toString + "]"
    }

    "receive a POST request with a JSON body with incorrect data. (wrong time values)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test4",
            "taskType": "RunOnce",
            "startDateAndTime": "2019-07-01 25:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
      bodyText mustBe "[" + Json.toJsObject(invalidStartDateFormat).toString + "]"
    }

    "receive a POST request with a JSON body with incorrect data. (given date already happened)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "RunOnce",
            "startDateAndTime": "2015-01-01 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
      bodyText mustBe "[" + Json.toJsObject(invalidStartDateValue).toString + "]"
    }

    "receive a POST request with a JSON body with correct periodic task data and insert it into the database. (with endDateAndTime) (Minutely) (yyyy-MM-dd HH:mm:ss date format)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test2",
            "taskType": "Periodic",
            "startDateAndTime": "2019-07-01 00:00:00",
            "periodType": "Minutely",
            "period": 2,
            "endDateAndTime": "2020-01-01 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
    }

    "receive a POST request with a JSON body with correct periodic task data and insert it into the database. (with occurrences) (Minutely)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test3",
            "taskType": "Periodic",
            "startDateAndTime": "2019-07-01 00:00:00",
            "periodType": "Minutely",
            "period": 2,
            "occurrences": 5
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
    }

    "receive a POST request with a JSON body with correct periodic task data and insert it into the database. (Hourly)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test4",
            "taskType": "Periodic",
            "startDateAndTime": "2019-07-01 00:00:00",
            "periodType": "Hourly",
            "period": 2,
            "occurrences": 5
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
    }

    "receive a POST request with a JSON body with correct periodic task data and insert it into the database. (Daily)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2019-07-01 00:00:00",
            "periodType": "Daily",
            "period": 2,
            "occurrences": 5
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
    }

    "receive a POST request with a JSON body with correct periodic task data and insert it into the database. (Weekly)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test2",
            "taskType": "Periodic",
            "startDateAndTime": "2019-07-01 00:00:00",
            "periodType": "Weekly",
            "period": 2,
            "occurrences": 5
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
    }

    "receive a POST request with a JSON body with correct periodic task data and insert it into the database. (Monthly)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test3",
            "taskType": "Periodic",
            "startDateAndTime": "2019-07-01 00:00:00",
            "periodType": "Monthly",
            "period": 2,
            "occurrences": 5
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
    }

    "receive a POST request with a JSON body with correct periodic task data and insert it into the database. (Yearly)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test4",
            "taskType": "Periodic",
            "startDateAndTime": "2019-07-01 00:00:00",
            "periodType": "Yearly",
            "period": 2,
            "occurrences": 5
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
    }

    "receive a POST request with a JSON body with correct periodic task data and insert it into the database. (dd-MM-yyyy HH:mm:ss endDate format)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2019-07-01 00:00:00",
            "periodType": "Minutely",
            "period": 2,
            "endDateAndTime": "01-01-2020 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
    }

    "receive a POST request with a JSON body with correct periodic task data and insert it into the database. (yyyy/MM/dd HH:mm:ss endDate format)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test2",
            "taskType": "Periodic",
            "startDateAndTime": "2019-07-01 00:00:00",
            "periodType": "Minutely",
            "period": 2,
            "endDateAndTime": "2020/01/01 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
    }

    "receive a POST request with a JSON body with correct periodic task data and insert it into the database. (dd/MM/yyyy HH:mm:ss endDate format)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test3",
            "taskType": "Periodic",
            "startDateAndTime": "2019-07-01 00:00:00",
            "periodType": "Minutely",
            "period": 2,
            "endDateAndTime": "01/01/2020 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
    }

    "receive a POST request with a JSON body with incorrect periodic task data. (missing Periodic fields)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test3",
            "taskType": "Periodic",
            "startDateAndTime": "2019-07-01 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
      bodyText mustBe "[" + Json.toJsObject(invalidCreateTaskFormat).toString + "]"
    }

    "receive a POST request with a JSON body with incorrect periodic task data. (invalid task type)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test4",
            "taskType": "Unknown",
            "startDateAndTime": "2019-07-01 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
      bodyText mustBe "[" + Json.toJsObject(invalidCreateTaskFormat).toString + "," + Json.toJsObject(invalidTaskType).toString + "]"
    }

    "receive a POST request with a JSON body with incorrect periodic task data. (negative period)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2019-07-01 00:00:00",
            "periodType": "Minutely",
            "period": -1,
            "occurrences": 5
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
      bodyText mustBe "[" + Json.toJsObject(invalidPeriod).toString + "]"
    }

    "receive a POST request with a JSON body with incorrect periodic task data. (wrong endDate format)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test2",
            "taskType": "Periodic",
            "startDateAndTime": "2019-07-01 00:00:00",
            "periodType": "Minutely",
            "period": 2,
            "endDateAndTime": "2020:01:01 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
      bodyText mustBe "[" + Json.toJsObject(invalidEndDateFormat).toString + "]"
    }

    "receive a POST request with a JSON body with incorrect periodic task data. (given endDate already happened)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test3",
            "taskType": "Periodic",
            "startDateAndTime": "2019-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 2,
            "endDateAndTime": "2019-01-15 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
      bodyText mustBe "[" + Json.toJsObject(invalidStartDateValue).toString + "," + Json.toJsObject(invalidEndDateValue).toString + "]"
    }

    "receive a POST request with a JSON body with incorrect periodic task data. (given endDate happens before startDate)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test4",
            "taskType": "Periodic",
            "startDateAndTime": "2019-07-01 00:00:00",
            "periodType": "Minutely",
            "period": 2,
            "endDateAndTime": "2019-06-15 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
      bodyText mustBe "[" + Json.toJsObject(invalidEndDateValue).toString + "]"
    }

    "receive a POST request with a JSON body with incorrect periodic task data. (negative occurrences)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2019-07-01 00:00:00",
            "periodType": "Minutely",
            "period": 2,
            "occurrences": -1
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
      bodyText mustBe "[" + Json.toJsObject(invalidOccurrences).toString + "]"
    }

    "receive a POST request with a JSON body with incorrect periodic task data. (missing periodType field)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test2",
            "taskType": "Periodic",
            "startDateAndTime": "2019-07-01 00:00:00",
            "period": 2,
            "occurrences": 5
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
      bodyText mustBe "[" + Json.toJsObject(invalidCreateTaskFormat).toString + "]"
    }

    "receive a POST request with a JSON body with incorrect periodic task data. (missing period field)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test3",
            "taskType": "Periodic",
            "startDateAndTime": "2019-07-01 00:00:00",
            "periodType": "Minutely",
            "occurrences": 5
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
      bodyText mustBe "[" + Json.toJsObject(invalidCreateTaskFormat).toString + "]"
    }

    "receive a POST request with a JSON body with incorrect periodic task data. (missing endDate/occurrences)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test4",
            "taskType": "Periodic",
            "startDateAndTime": "2019-07-01 00:00:00",
            "periodType": "Minutely",
            "period": 2
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
      bodyText mustBe "[" + Json.toJsObject(invalidCreateTaskFormat).toString + "]"
    }
  }

  "GET /task" should {
    "receive a GET request with no tasks inserted" in {
      val fakeRequest = FakeRequest(GET, "/task")
        .withHeaders(HOST -> LOCALHOST)
      val routeOption = route(app, fakeRequest)
      Await.result(routeOption.get, Duration.Inf)
      val bodyText = contentAsString(routeOption.get)
      status(routeOption.get) mustBe OK
      bodyText mustBe "[]"
    }

    "receive a GET request of all tasks after inserting 3 tasks" in {
      val dto1 = TaskDTO("asd1", "test1", SchedulingType.RunOnce)
      val dto2 = TaskDTO("asd2", "test2", SchedulingType.Periodic, Some(stringToDateFormat("2020-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Daily), Some(2), Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      val dto3 = TaskDTO("asd3", "test3", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Monthly), Some(1), None, Some(12), Some(12))
      val result = for{
        _ <- taskRepo.insertInTasksTable(dto1)
        _ <- taskRepo.insertInTasksTable(dto2)
        res <- taskRepo.insertInTasksTable(dto3)
      } yield res
      Await.result(result, Duration.Inf)
      val fakeRequest = FakeRequest(GET, "/task")
        .withHeaders(HOST -> LOCALHOST)
      val routeOption = route(app, fakeRequest)
      Await.result(routeOption.get, Duration.Inf)
      val bodyText = contentAsString(routeOption.get)
      status(routeOption.get) mustBe OK
      bodyText mustBe "[" + Json.toJsObject(dto1) + "," + Json.toJsObject(dto2) + "," + Json.toJsObject(dto3) + "]"
    }
  }

  "GET /task/:id" should {

    "receive a GET request with an existing id for a specific task." in {
      val dto1 = TaskDTO("asd1", "test1", SchedulingType.RunOnce)
      val dto2 = TaskDTO("asd2", "test2", SchedulingType.Periodic, Some(stringToDateFormat("2020-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Daily), Some(2), Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      val dto3 = TaskDTO("asd3", "test3", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Monthly), Some(1), None, Some(12), Some(12))
      val result = for {
        _ <- taskRepo.insertInTasksTable(dto1)
        _ <- taskRepo.insertInTasksTable(dto2)
        res <- taskRepo.insertInTasksTable(dto3)
      } yield res
      Await.result(result, Duration.Inf)
      val id = "asd2" // id corresponding to dto2
      val fakeRequest = FakeRequest(GET, "/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
      val routeOption = route(app, fakeRequest)
      Await.result(routeOption.get, Duration.Inf)
      val bodyText = contentAsString(routeOption.get)
      status(routeOption.get) mustBe OK
      bodyText mustBe Json.toJsObject(dto2).toString
    }

    "receive a GET request with a non-existing id." in {
      val dto1 = TaskDTO("asd1", "test1", SchedulingType.RunOnce)
      val dto2 = TaskDTO("asd2", "test2", SchedulingType.Periodic, Some(stringToDateFormat("2020-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Daily), Some(2), Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      val dto3 = TaskDTO("asd3", "test3", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Monthly), Some(1), None, Some(12), Some(12))
      val result = for {
        _ <- taskRepo.insertInTasksTable(dto1)
        _ <- taskRepo.insertInTasksTable(dto2)
        res <- taskRepo.insertInTasksTable(dto3)
      } yield res
      Await.result(result, Duration.Inf)
      val id = "asd4" // id that doesn't correspond to any dto
      val fakeRequest = FakeRequest(GET, "/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
      val routeOption = route(app, fakeRequest)
      Await.result(routeOption.get, Duration.Inf)
      val bodyText = contentAsString(routeOption.get)
      status(routeOption.get) mustBe BAD_REQUEST
      bodyText mustBe Json.toJsObject(invalidEndpointId).toString
    }
  }

  "PATCH /task/:id" should {
    "receive a PATCH request with a non-existing id." in {
      val dto1 = TaskDTO("asd1", "test1", SchedulingType.RunOnce)
      val dto2 = TaskDTO("asd2", "test2", SchedulingType.Periodic, Some(stringToDateFormat("2020-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Daily), Some(2), Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      val dto3 = TaskDTO("asd3", "test3", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Monthly), Some(1), None, Some(12), Some(12))
      val result = for {
        _ <- taskRepo.insertInTasksTable(dto1)
        _ <- taskRepo.insertInTasksTable(dto2)
        res <- taskRepo.insertInTasksTable(dto3)
      } yield res
      Await.result(result, Duration.Inf)
      val id = "asd4" // id that doesn't correspond to any dto
      val fakeRequest = FakeRequest(PATCH, "/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
        .withBody(Json.parse("""
          {
            "taskId":"newUUID"
          }
        """))
      val routeOption = route(app, fakeRequest)
      Await.result(routeOption.get, Duration.Inf)
      val bodyText = contentAsString(routeOption.get)
      println(bodyText)
      status(routeOption.get) mustBe BAD_REQUEST
      bodyText mustBe "[" + Json.toJsObject(invalidEndpointId) + "]"
      val task = Await.result(taskRepo.selectTaskByTaskId("newUUID"), Duration.Inf)
      task.isDefined mustBe false
    }

    "receive a PATCH request changing the taskId value of a task." in {
      val dto1 = TaskDTO("asd1", "test1", SchedulingType.RunOnce)
      val dto2 = TaskDTO("asd2", "test2", SchedulingType.Periodic, Some(stringToDateFormat("2020-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Daily), Some(2), Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      val dto3 = TaskDTO("asd3", "test3", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Monthly), Some(1), None, Some(12), Some(12))
      val result = for {
        _ <- taskRepo.insertInTasksTable(dto1)
        _ <- taskRepo.insertInTasksTable(dto2)
        res <- taskRepo.insertInTasksTable(dto3)
      } yield res
      Await.result(result, Duration.Inf)
      val id = "asd1"
      val fakeRequest = FakeRequest(PATCH, "/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
        .withBody(Json.parse("""
          {
            "taskId":"11231bd5-6f92-496c-9fe7-75bc180467b0"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val task = for{
        _ <- routeOption.get
        res <- taskRepo.selectTaskByTaskId("11231bd5-6f92-496c-9fe7-75bc180467b0")
      } yield res
      val bodyText = contentAsString(routeOption.get)
      println(bodyText)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
      task.map{
        elem => elem.isDefined mustBe true
        val resultDto = TaskDTO("11231bd5-6f92-496c-9fe7-75bc180467b0", "test1", SchedulingType.RunOnce)
        elem.get mustBe Json.toJsObject(resultDto).toString
      }

    }

    "receive a PATCH request changing the fileName of a task." in {
      val dto1 = TaskDTO("asd1", "test1", SchedulingType.RunOnce)
      val dto2 = TaskDTO("asd2", "test2", SchedulingType.Periodic, Some(stringToDateFormat("2020-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Daily), Some(2), Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      val dto3 = TaskDTO("asd3", "test3", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Monthly), Some(1), None, Some(12), Some(12))
      val result = for {
        _ <- taskRepo.insertInTasksTable(dto1)
        _ <- taskRepo.insertInTasksTable(dto2)
        res <- taskRepo.insertInTasksTable(dto3)
      } yield res
      Await.result(result, Duration.Inf)
      val id = "asd1"
      Await.result(taskRepo.selectTaskByTaskId(id), Duration.Inf).isDefined mustBe true
      val fakeRequest = FakeRequest(PATCH, "/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
        .withBody(Json.parse("""
          {
            "fileName":"test4"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val task = for{
        _ <- routeOption.get
        res <- taskRepo.selectTaskByTaskId(id)
      } yield res
      val bodyText = contentAsString(routeOption.get)
      println(bodyText)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
      task.map{ elem =>
        elem.isDefined mustBe true
        val resultDto = TaskDTO("asd1", "test4", SchedulingType.RunOnce)
        elem.get mustBe Json.toJsObject(resultDto).toString
      }
    }

    "receive a PATCH request changing the taskType of a periodic task to RunOnce." in {
      val dto1 = TaskDTO("asd1", "test1", SchedulingType.RunOnce)
      val dto2 = TaskDTO("asd2", "test2", SchedulingType.Periodic, Some(stringToDateFormat("2020-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Daily), Some(2), Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      val dto3 = TaskDTO("asd3", "test3", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Monthly), Some(1), None, Some(12), Some(12))
      val result = for {
        _ <- taskRepo.insertInTasksTable(dto1)
        _ <- taskRepo.insertInTasksTable(dto2)
        res <- taskRepo.insertInTasksTable(dto3)
      } yield res
      Await.result(result, Duration.Inf)
      val id = "asd2"
      val fakeRequest = FakeRequest(PATCH, "/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
        .withBody(Json.parse("""
          {
            "taskType":"RunOnce"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val task = for{
        _ <- routeOption.get
        res <- taskRepo.selectTaskByTaskId(id)
      } yield res
      val bodyText = contentAsString(routeOption.get)
      println(bodyText)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
      task.map{ elem =>
        elem.isDefined mustBe true
        val resultDto = TaskDTO(id, "test2", SchedulingType.RunOnce, Some(stringToDateFormat("2020-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Daily), Some(2), Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
        elem.get mustBe resultDto
      }

    }

    "receive a PATCH request changing the taskType of a single run task to Periodic and fail. (doesn't have the other needed parameters)." in {
      val dto1 = TaskDTO("asd1", "test1", SchedulingType.RunOnce)
      val dto2 = TaskDTO("asd2", "test2", SchedulingType.Periodic, Some(stringToDateFormat("2020-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Daily), Some(2), Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      val dto3 = TaskDTO("asd3", "test3", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Monthly), Some(1), None, Some(12), Some(12))
      val result = for {
        _ <- taskRepo.insertInTasksTable(dto1)
        _ <- taskRepo.insertInTasksTable(dto2)
        res <- taskRepo.insertInTasksTable(dto3)
      } yield res
      Await.result(result, Duration.Inf)
      val id = "asd1"
      val fakeRequest = FakeRequest(PATCH, "/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
        .withBody(Json.parse("""
          {
            "taskType":"Periodic"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val task = for {
        _ <- routeOption.get
        res <- taskRepo.selectTaskByTaskId(id)
      } yield res
      val bodyText = contentAsString(routeOption.get)
      println(bodyText)
      status(routeOption.get) mustBe BAD_REQUEST
      bodyText mustBe "[" + Json.toJsObject(invalidUpdateTaskFormat) + "]"
      task.map{ elem =>
        elem.isDefined mustBe true
        elem.get mustBe Json.toJsObject(dto1).toString
      }

    }

    "receive a PATCH request changing the taskType of a single run task to Periodic and succeed. (by adding all other needed Periodic parameters)" in {
      val dto1 = TaskDTO("asd1", "test1", SchedulingType.RunOnce)
      val dto2 = TaskDTO("asd2", "test2", SchedulingType.Periodic, Some(stringToDateFormat("2020-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Daily), Some(2), Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      val dto3 = TaskDTO("asd3", "test3", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Monthly), Some(1), None, Some(12), Some(12))
      val result = for {
        _ <- taskRepo.insertInTasksTable(dto1)
        _ <- taskRepo.insertInTasksTable(dto2)
        res <- taskRepo.insertInTasksTable(dto3)
      } yield res
      Await.result(result, Duration.Inf)
      val id = "asd1"
      val fakeRequest = FakeRequest(PATCH, "/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
        .withBody(Json.parse("""
          {
            "taskType":"Periodic",
            "startDateAndTime": "2019-07-01 00:00:00",
            "periodType": "Hourly",
            "period": 1,
            "endDateAndTime": "2020-01-01 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val task = for {
        _ <- routeOption.get
        res <- taskRepo.selectTaskByTaskId(id)
      } yield res
      val bodyText = contentAsString(routeOption.get)
      println(bodyText)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
      task.map { elem =>
        elem.isDefined mustBe true
        val resultDto = TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(stringToDateFormat("2019-07-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Hourly), Some(1), Some(stringToDateFormat("2020-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")))
        elem.get mustBe resultDto
      }

    }

    "receive a PATCH request changing the startDate of a task." in {
      val dto1 = TaskDTO("asd1", "test1", SchedulingType.RunOnce)
      val dto2 = TaskDTO("asd2", "test2", SchedulingType.Periodic, Some(stringToDateFormat("2020-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Daily), Some(2), Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      val dto3 = TaskDTO("asd3", "test3", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Monthly), Some(1), None, Some(12), Some(12))
      val result = for {
        _ <- taskRepo.insertInTasksTable(dto1)
        _ <- taskRepo.insertInTasksTable(dto2)
        res <- taskRepo.insertInTasksTable(dto3)
      } yield res
      Await.result(result, Duration.Inf)
      val id = "asd2"
      val fakeRequest = FakeRequest(PATCH, "/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
        .withBody(Json.parse("""
          {
            "startDateAndTime":"2019-07-01 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val task = for {
        _ <- routeOption.get
        res <- taskRepo.selectTaskByTaskId(id)
      } yield res
      val bodyText = contentAsString(routeOption.get)
      println(bodyText)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
      task.map { elem =>
        elem.isDefined mustBe true
        val resultDto = dto2.copy(startDateAndTime = Some(stringToDateFormat("2019-07-01 00:00:00", "yyyy-MM-dd HH:mm:ss")))
        elem.get mustBe resultDto
      }
    }

    "receive a PATCH request changing the periodType of a task." in {
      val dto1 = TaskDTO("asd1", "test1", SchedulingType.RunOnce)
      val dto2 = TaskDTO("asd2", "test2", SchedulingType.Periodic, Some(stringToDateFormat("2020-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Daily), Some(2), Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      val dto3 = TaskDTO("asd3", "test3", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Monthly), Some(1), None, Some(12), Some(12))
      val result = for {
        _ <- taskRepo.insertInTasksTable(dto1)
        _ <- taskRepo.insertInTasksTable(dto2)
        res <- taskRepo.insertInTasksTable(dto3)
      } yield res
      Await.result(result, Duration.Inf)
      val id = "asd2"
      val fakeRequest = FakeRequest(PATCH, "/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
        .withBody(Json.parse("""
          {
            "periodType":"Hourly"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val task = for {
        _ <- routeOption.get
        res <- taskRepo.selectTaskByTaskId(id)
      } yield res
      val bodyText = contentAsString(routeOption.get)
      println(bodyText)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
      task.map { elem =>
        elem.isDefined mustBe true
        val resultDto = dto2.copy(periodType = Some(PeriodType.Hourly))
        elem.get mustBe resultDto
      }
    }

    "receive a PATCH request changing the period of a task." in {
      val dto1 = TaskDTO("asd1", "test1", SchedulingType.RunOnce)
      val dto2 = TaskDTO("asd2", "test2", SchedulingType.Periodic, Some(stringToDateFormat("2020-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Daily), Some(2), Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      val dto3 = TaskDTO("asd3", "test3", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Monthly), Some(1), None, Some(12), Some(12))
      val result = for {
        _ <- taskRepo.insertInTasksTable(dto1)
        _ <- taskRepo.insertInTasksTable(dto2)
        res <- taskRepo.insertInTasksTable(dto3)
      } yield res
      Await.result(result, Duration.Inf)
      val id = "asd2"
      val fakeRequest = FakeRequest(PATCH, "/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
        .withBody(Json.parse("""
          {
            "period": 5
          }
        """))
      val routeOption = route(app, fakeRequest)
      val task = for {
        _ <- routeOption.get
        res <- taskRepo.selectTaskByTaskId(id)
      } yield res
      val bodyText = contentAsString(routeOption.get)
      println(bodyText)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
      task.map{ elem =>
        elem.isDefined mustBe true
        val resultDto = dto2.copy(period = Some(5))
        elem.get mustBe resultDto
      }

    }

    "receive a PATCH request changing the endDate of a task." in {
      val dto1 = TaskDTO("asd1", "test1", SchedulingType.RunOnce)
      val dto2 = TaskDTO("asd2", "test2", SchedulingType.Periodic, Some(stringToDateFormat("2020-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Daily), Some(2), Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      val dto3 = TaskDTO("asd3", "test3", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Monthly), Some(1), None, Some(12), Some(12))
      val result = for {
        _ <- taskRepo.insertInTasksTable(dto1)
        _ <- taskRepo.insertInTasksTable(dto2)
        res <- taskRepo.insertInTasksTable(dto3)
      } yield res
      Await.result(result, Duration.Inf)
      val id = "asd2"
      val fakeRequest = FakeRequest(PATCH, "/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
        .withBody(Json.parse("""
          {
            "endDateAndTime":"2050-01-01 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val task = for {
        _ <- routeOption.get
        res <- taskRepo.selectTaskByTaskId(id)
      } yield res
      val bodyText = contentAsString(routeOption.get)
      println(bodyText)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
      task.map { elem =>
        elem.isDefined mustBe true
        val resultDto = dto2.copy(endDateAndTime = Some(stringToDateFormat("2050-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")))
        elem.get mustBe resultDto
      }

    }

    "receive a PATCH request changing the endDate of a task that already has an occurrences field. (replaces occurrences with the endDate)" in {
      val dto1 = TaskDTO("asd1", "test1", SchedulingType.RunOnce)
      val dto2 = TaskDTO("asd2", "test2", SchedulingType.Periodic, Some(stringToDateFormat("2020-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Daily), Some(2), Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      val dto3 = TaskDTO("asd3", "test3", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Monthly), Some(1), None, Some(12), Some(12))
      val result = for {
        _ <- taskRepo.insertInTasksTable(dto1)
        _ <- taskRepo.insertInTasksTable(dto2)
        res <- taskRepo.insertInTasksTable(dto3)
      } yield res
      Await.result(result, Duration.Inf)
      val id = "asd3"
      val fakeRequest = FakeRequest(PATCH, "/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
        .withBody(Json.parse("""
          {
            "endDateAndTime":"2050-01-01 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val task = for{
        _ <- routeOption.get
        res <- taskRepo.selectTaskByTaskId(id)
      } yield res
      val bodyText = contentAsString(routeOption.get)
      println(bodyText)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
      task.map{ elem =>
        elem.isDefined mustBe true
        val resultDTO = dto3.copy(endDateAndTime = Some(stringToDateFormat("2050-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), totalOccurrences = None, currentOccurrences = None)
        elem.get mustBe Json.toJsObject(resultDTO).toString
      }
    }


    "receive a PATCH request changing the occurrences of a task." in {
      val dto1 = TaskDTO("asd1", "test1", SchedulingType.RunOnce)
      val dto2 = TaskDTO("asd2", "test2", SchedulingType.Periodic, Some(stringToDateFormat("2020-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Daily), Some(2), Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      val dto3 = TaskDTO("asd3", "test3", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Monthly), Some(1), None, Some(12), Some(12))
      val result = for {
        _ <- taskRepo.insertInTasksTable(dto1)
        _ <- taskRepo.insertInTasksTable(dto2)
        res <- taskRepo.insertInTasksTable(dto3)
      } yield res
      Await.result(result, Duration.Inf)
      val id = "asd3"
      val fakeRequest = FakeRequest(PATCH, "/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
        .withBody(Json.parse("""
          {
            "occurrences": 5
          }
        """))
      val routeOption = route(app, fakeRequest)
      val task = for{
        _ <- routeOption.get
        res <- taskRepo.selectTaskByTaskId(id)
      } yield res
      val bodyText = contentAsString(routeOption.get)
      println(bodyText)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
      task.map { elem =>
        elem.isDefined mustBe true
        val resultDto = dto3.copy(totalOccurrences = Some(5), currentOccurrences = Some(5))
        elem.get mustBe Json.toJsObject(resultDto).toString
      }

    }

    "receive a PATCH request changing the occurrences of a task that already has an endDate field. (replaces endDate with the occurrences)" in {
      val dto1 = TaskDTO("asd1", "test1", SchedulingType.RunOnce)
      val dto2 = TaskDTO("asd2", "test2", SchedulingType.Periodic, Some(stringToDateFormat("2020-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Daily), Some(2), Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      val dto3 = TaskDTO("asd3", "test3", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Monthly), Some(1), None, Some(12), Some(12))
      val result = for {
        _ <- taskRepo.insertInTasksTable(dto1)
        _ <- taskRepo.insertInTasksTable(dto2)
        res <- taskRepo.insertInTasksTable(dto3)
      } yield res
      Await.result(result, Duration.Inf)
      val id = "asd2"
      val fakeRequest = FakeRequest(PATCH, "/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
        .withBody(Json.parse("""
          {
            "occurrences": 5
          }
        """))
      val routeOption = route(app, fakeRequest)
      val task = for {
        _ <- routeOption.get
        res <- taskRepo.selectTaskByTaskId(id)
      } yield res
      val bodyText = contentAsString(routeOption.get)
      println(bodyText)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
      task.map{ elem =>
        elem.isDefined mustBe true
        val resultDTO = dto2.copy(endDateAndTime = None, totalOccurrences = Some(5), currentOccurrences = Some(5))
        elem.get mustBe Json.toJsObject(dto2).toString
      }
    }

    "receive a PATCH request changing multiple values of a RunOnce task." in {
      val dto1 = TaskDTO("asd1", "test1", SchedulingType.RunOnce)
      val dto2 = TaskDTO("asd2", "test2", SchedulingType.Periodic, Some(stringToDateFormat("2020-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Daily), Some(2), Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      val dto3 = TaskDTO("asd3", "test3", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Monthly), Some(1), None, Some(12), Some(12))
      val result = for {
        _ <- taskRepo.insertInTasksTable(dto1)
        _ <- taskRepo.insertInTasksTable(dto2)
        res <- taskRepo.insertInTasksTable(dto3)
      } yield res
      Await.result(result, Duration.Inf)
      val id = "asd1"
      val fakeRequest = FakeRequest(PATCH, "/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
        .withBody(Json.parse("""
          {
            "taskId":"11231bd5-6f92-496c-9fe7-75bc180467b0",
            "fileName":"test4",
            "taskType":"RunOnce",
            "startDateAndTime":"2050-01-01 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val task = for {
        _ <- routeOption.get
        res <- taskRepo.selectTaskByTaskId(id)
      } yield res
      val bodyText = contentAsString(routeOption.get)
      println(bodyText)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
      task.map{ elem =>
        elem.isDefined mustBe true
        val resultDto = TaskDTO("11231bd5-6f92-496c-9fe7-75bc180467b0", "test4", SchedulingType.RunOnce, Some(stringToDateFormat("2050-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")))
        elem.get mustBe Json.toJsObject(resultDto).toString
      }
    }

    "receive a PATCH request changing multiple values of a Periodic task." in {
      val dto1 = TaskDTO("asd1", "test1", SchedulingType.RunOnce)
      val dto2 = TaskDTO("asd2", "test2", SchedulingType.Periodic, Some(stringToDateFormat("2020-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Daily), Some(2), Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      val dto3 = TaskDTO("asd3", "test3", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Monthly), Some(1), None, Some(12), Some(12))
      val result = for {
        _ <- taskRepo.insertInTasksTable(dto1)
        _ <- taskRepo.insertInTasksTable(dto2)
        res <- taskRepo.insertInTasksTable(dto3)
      } yield res
      Await.result(result, Duration.Inf)
      val id = "asd3"
      val fakeRequest = FakeRequest(PATCH, "/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
        .withBody(Json.parse("""
          {
            "taskId":"11231bd5-6f92-496c-9fe7-75bc180467b0",
            "fileName":"test4",
            "taskType":"Periodic",
            "startDateAndTime":"2050-01-01 00:00:00",
            "periodType":"Yearly",
            "period":2,
            "occurrences":6
          }
        """))
      val routeOption = route(app, fakeRequest)
      val task = for{
        _ <- routeOption.get
        res <- taskRepo.selectTaskByTaskId(id)
      } yield res
      val bodyText = contentAsString(routeOption.get)
      println(bodyText)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
      task.map{ elem =>
        elem.isDefined mustBe true
        val resultDto = TaskDTO("11231bd5-6f92-496c-9fe7-75bc180467b0", "test4", SchedulingType.Periodic, Some(stringToDateFormat("2050-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Yearly), Some(2), None, Some(6), Some(6))
        elem.get mustBe Json.toJsObject(resultDto).toString
      }

    }
  }

  "PUT /task/:id" should {

    "receive a PUT request with a non-existing id and a given correct task" in {
      val dto1 = TaskDTO("asd1", "test1", SchedulingType.RunOnce)
      val dto2 = TaskDTO("asd2", "test2", SchedulingType.Periodic, Some(stringToDateFormat("2020-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Daily), Some(2), Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      val dto3 = TaskDTO("asd3", "test3", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Monthly), Some(1), None, Some(12), Some(12))
      val result = for {
        _ <- taskRepo.insertInTasksTable(dto1)
        _ <- taskRepo.insertInTasksTable(dto2)
        res <- taskRepo.insertInTasksTable(dto3)
      } yield res
      Await.result(result, Duration.Inf)
      val id = "11231bd5-6f92-496c-9fe7-75bc180467b0"
      val fakeRequest = FakeRequest(PUT, "/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
        .withBody(Json.parse("""
          {
            "fileName": "test4",
            "taskType": "Periodic",
            "startDateAndTime": "2019-07-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2020-01-01 00:00:00"
           }
        """))
      val routeOption = route(app, fakeRequest)
      val task = for{
        _ <- routeOption.get
        res <- taskRepo.selectTaskByTaskId("11231bd5-6f92-496c-9fe7-75bc180467b0")
      } yield res
      val bodyText = contentAsString(routeOption.get)
      println(bodyText)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
      task.map(elem => elem.isDefined mustBe true)
    }

    "receive a PUT request with a non-existing id and a given incorrect task. (missing endDate)" in {
      val dto1 = TaskDTO("asd1", "test1", SchedulingType.RunOnce)
      val dto2 = TaskDTO("asd2", "test2", SchedulingType.Periodic, Some(stringToDateFormat("2020-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Daily), Some(2), Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      val dto3 = TaskDTO("asd3", "test3", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Monthly), Some(1), None, Some(12), Some(12))
      val result = for {
        _ <- taskRepo.insertInTasksTable(dto1)
        _ <- taskRepo.insertInTasksTable(dto2)
        res <- taskRepo.insertInTasksTable(dto3)
      } yield res
      Await.result(result, Duration.Inf)
      val id = "11231bd5-6f92-496c-9fe7-75bc180467b0"
      val fakeRequest = FakeRequest(PUT, "/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
        .withBody(Json.parse("""
          {
            "fileName": "test4",
            "taskType": "Periodic",
            "startDateAndTime": "2019-07-01 00:00:00",
            "periodType": "Minutely",
            "period": 5
           }
        """))
      val routeOption = route(app, fakeRequest)
      val task = for{
        _ <- routeOption.get
        res <- taskRepo.selectTaskByTaskId("11231bd5-6f92-496c-9fe7-75bc180467b0")
      } yield res
      val bodyText = contentAsString(routeOption.get)
      println(bodyText)
      status(routeOption.get) mustBe BAD_REQUEST
      bodyText mustBe "[" + Json.toJsObject(invalidCreateTaskFormat) + "]"
      task.map(elem => elem.isDefined mustBe false)
    }

    "receive a PUT request with a non-existing id and a given incorrect task. (has taskId)" in {
      val dto1 = TaskDTO("asd1", "test1", SchedulingType.RunOnce)
      val dto2 = TaskDTO("asd2", "test2", SchedulingType.Periodic, Some(stringToDateFormat("2020-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Daily), Some(2), Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      val dto3 = TaskDTO("asd3", "test3", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Monthly), Some(1), None, Some(12), Some(12))
      val result = for {
        _ <- taskRepo.insertInTasksTable(dto1)
        _ <- taskRepo.insertInTasksTable(dto2)
        res <- taskRepo.insertInTasksTable(dto3)
      } yield res
      Await.result(result, Duration.Inf)
      val id = "asd4"
      val fakeRequest = FakeRequest(PUT, "/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
        .withBody(Json.parse("""
          {
            "taskId": "11231bd5-6f92-496c-9fe7-75bc180467b0",
            "fileName": "test4",
            "taskType": "Periodic",
            "startDateAndTime": "2019-07-01 00:00:00",
            "periodType": "Minutely",
            "period": 5
           }
        """))
      val routeOption = route(app, fakeRequest)
      val tasks = for{
        _ <- routeOption.get
        task1 <- taskRepo.selectTaskByTaskId("11231bd5-6f92-496c-9fe7-75bc180467b0")
        task2 <- taskRepo.selectTaskByTaskId("asd4")
      } yield (task1, task2)
      val bodyText = contentAsString(routeOption.get)
      println(bodyText)
      status(routeOption.get) mustBe BAD_REQUEST
      bodyText mustBe "[" + Json.toJsObject(invalidCreateTaskFormat) + "]"
      tasks.map { elem =>
        elem._1.isDefined mustBe false
        elem._2.isDefined mustBe false
      }
    }

    "receive a PUT request replacing a task with the given id with a given correct task." in {
      val dto1 = TaskDTO("asd1", "test1", SchedulingType.RunOnce)
      val dto2 = TaskDTO("asd2", "test2", SchedulingType.Periodic, Some(stringToDateFormat("2020-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Daily), Some(2), Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      val dto3 = TaskDTO("asd3", "test3", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Monthly), Some(1), None, Some(12), Some(12))
      val result = for {
        _ <- taskRepo.insertInTasksTable(dto1)
        _ <- taskRepo.insertInTasksTable(dto2)
        res <- taskRepo.insertInTasksTable(dto3)
      } yield res
      Await.result(result, Duration.Inf)
      val id = "asd1"
      val fakeRequest = FakeRequest(PUT, "/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
        .withBody(Json.parse("""
          {
            "taskId": "11231bd5-6f92-496c-9fe7-75bc180467b0",
            "fileName": "test4",
            "taskType": "Periodic",
            "startDateAndTime": "2019-07-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2020-01-01 00:00:00"
           }
        """))
      val routeOption = route(app, fakeRequest)
      val task = for {
        _ <- routeOption.get
        res <- taskRepo.selectTaskByTaskId("11231bd5-6f92-496c-9fe7-75bc180467b0")
      } yield res
      val bodyText = contentAsString(routeOption.get)
      println(bodyText)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received."
      task.map { elem =>
        elem.isDefined mustBe true
        val resultDto = TaskDTO("11231bd5-6f92-496c-9fe7-75bc180467b0", "test4", SchedulingType.Periodic, Some(stringToDateFormat("2019-07-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2020-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")))
        elem.get mustBe Json.toJsObject(resultDto).toString
      }
    }

    "receive a PUT request replacing a task with the given id with a given incorrect task." in {
      val dto1 = TaskDTO("asd1", "test1", SchedulingType.RunOnce)
      val dto2 = TaskDTO("asd2", "test2", SchedulingType.Periodic, Some(stringToDateFormat("2020-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Daily), Some(2), Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      val dto3 = TaskDTO("asd3", "test3", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Monthly), Some(1), None, Some(12), Some(12))
      val result = for {
        _ <- taskRepo.insertInTasksTable(dto1)
        _ <- taskRepo.insertInTasksTable(dto2)
        res <- taskRepo.insertInTasksTable(dto3)
      } yield res
      Await.result(result, Duration.Inf)
      val id = "asd1"
      val fakeRequest = FakeRequest(PUT, "/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
        .withBody(Json.parse("""
          {
            "taskId": "11231bd5-6f92-496c-9fe7-75bc180467b0",
            "fileName": "test4",
            "taskType": "Periodic",
            "startDateAndTime": "2019-07-01 00:00:00",
            "periodType": "Minutely",
            "period": 5
           }
        """))
      val routeOption = route(app, fakeRequest)
      val task = for{
        _ <- routeOption.get
        res <- taskRepo.selectTaskByTaskId("11231bd5-6f92-496c-9fe7-75bc180467b0")
      } yield res
      val bodyText = contentAsString(routeOption.get)
      println(bodyText)
      status(routeOption.get) mustBe BAD_REQUEST
      bodyText mustBe "[" + Json.toJsObject(invalidCreateTaskFormat) + "]"
      task.map { elem =>
        elem.isDefined mustBe true
        elem.get mustBe Json.toJsObject(dto1).toString
      }
    }
  }

  "DELETE /task/:id" should {

    "receive a PUT request with a non-existing id" in {
      val dto1 = TaskDTO("asd1", "test1", SchedulingType.RunOnce)
      Await.result(taskRepo.insertInTasksTable(dto1), Duration.Inf)
      val initialResult = Await.result(taskRepo.selectAllTasks, Duration.Inf)
      initialResult.size mustBe 1
      val id = "asd2"
      val fakeRequest = FakeRequest(DELETE, "/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
      val routeOption = route(app, fakeRequest)
      val tasks = for {
        _ <- routeOption.get
        res <- taskRepo.selectAllTasks
      } yield res
      val bodyText = contentAsString(routeOption.get)
      println(bodyText)
      status(routeOption.get) mustBe BAD_REQUEST
      bodyText mustBe Json.toJsObject(invalidEndpointId).toString
      tasks.map(elem => elem.size mustBe 1)
    }

    "receive a DELETE request to delete a task with the given id." in {
      val dto1 = TaskDTO("asd1", "test1", SchedulingType.RunOnce)
      Await.result(taskRepo.insertInTasksTable(dto1), Duration.Inf)
      val initialResult = Await.result(taskRepo.selectAllTasks, Duration.Inf)
      initialResult.size mustBe 1
      val id = "asd1"
      val fakeRequest = FakeRequest(DELETE, "/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
      val routeOption = route(app, fakeRequest)
      val tasks = for{
        _ <- routeOption.get
        res <- taskRepo.selectAllTasks
      } yield res
      val bodyText = contentAsString(routeOption.get)
      println(bodyText)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task with id = " + id + " was deleted"
      tasks.map(elem => elem.size mustBe 0)
    }
  }

  /*"TaskController#GETtask" should {
    "receive a GET request" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
      val result = route(app, fakeRequest)
      val bodyText: String = contentAsString(result.get)
      println(bodyText)
      status(result.get) mustBe OK
    }
  }*/

}
