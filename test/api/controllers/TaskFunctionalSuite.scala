package api.controllers

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, Materializer }
import api.dtos.{ FileDTO, TaskDTO }
import api.services.{ PeriodType, SchedulingType }
import api.utils.DateUtils._
import api.utils.{ DefaultUUIDGenerator, UUIDGenerator }
import api.validators.Error._
import database.mappings.FileMappings._
import database.mappings.TaskMappings._
import database.mappings.ExclusionMappings._
import database.mappings.SchedulingMappings._
import database.utils.DatabaseUtils._
import database.repositories.FileRepository
import database.repositories.exclusion.{ ExclusionRepository, ExclusionRepositoryImpl }
import database.repositories.file.FileRepositoryImpl
import database.repositories.scheduling.{ SchedulingRepository, SchedulingRepositoryImpl }
import database.repositories.task.{ TaskRepository, TaskRepositoryImpl }
import executionengine.{ ExecutionManager, FakeExecutionManager }
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext }

class TaskFunctionalSuite extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterAll with BeforeAndAfterEach {

  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  private lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
  private val dtbase: Database = appBuilder.injector.instanceOf[Database]
  private implicit val fileRepo: FileRepository = new FileRepositoryImpl(TEST_DB)
  private implicit val taskRepo: TaskRepository = new TaskRepositoryImpl(TEST_DB)
  private implicit val exclusionRepo: ExclusionRepository = new ExclusionRepositoryImpl(TEST_DB)
  private implicit val schedulingRepo: SchedulingRepository = new SchedulingRepositoryImpl(TEST_DB)
  private implicit val uuidGen: UUIDGenerator = appBuilder.injector.instanceOf[UUIDGenerator]
  private implicit val executionManager: ExecutionManager = appBuilder.injector.instanceOf[ExecutionManager]
  private implicit val actorSystem: ActorSystem = ActorSystem()
  private implicit val mat: Materializer = ActorMaterializer()

  private val LOCALHOST = "localhost:9000"

  private val taskUUID1: String = UUID.randomUUID().toString
  private val taskUUID2: String = UUID.randomUUID().toString
  private val taskUUID3: String = UUID.randomUUID().toString
  private val taskUUID4: String = UUID.randomUUID().toString

  private val fileUUID1: String = UUID.randomUUID().toString
  private val fileUUID2: String = UUID.randomUUID().toString
  private val fileUUID3: String = UUID.randomUUID().toString
  private val fileUUID4: String = UUID.randomUUID().toString

  private val id = uuidGen.generateUUID

  override def beforeAll: Unit = {
    val result = for {
      _ <- dtbase.run(createFilesTableAction)
      _ <- fileRepo.insertInFilesTable(FileDTO(fileUUID1, "test1", getCurrentDateTimestamp))
      _ <- fileRepo.insertInFilesTable(FileDTO(fileUUID2, "test2", getCurrentDateTimestamp))
      _ <- fileRepo.insertInFilesTable(FileDTO(fileUUID3, "test3", getCurrentDateTimestamp))
      _ <- fileRepo.insertInFilesTable(FileDTO(fileUUID4, "test4", getCurrentDateTimestamp))
      _ <- dtbase.run(createTasksTableAction)
      _ <- dtbase.run(createExclusionsTableAction)
      res <- dtbase.run(createSchedulingsTableAction)
    } yield res
    Await.result(result, Duration.Inf)
  }

  override def afterAll: Unit = {
    Await.result(dtbase.run(dropSchedulingsTableAction), Duration.Inf)
    Await.result(dtbase.run(dropExclusionsTableAction), Duration.Inf)
    Await.result(dtbase.run(dropTasksTableAction), Duration.Inf)
    Await.result(dtbase.run(dropFilesTableAction), Duration.Inf)
  }

  override def afterEach: Unit = {
    Await.result(schedulingRepo.deleteAllSchedulings, Duration.Inf)
    Await.result(exclusionRepo.deleteAllExclusions, Duration.Inf)
    Await.result(taskRepo.deleteAllTasks, Duration.Inf)

  }

  "GET /" should {
    "receive a GET request" in {
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
      val result = for {
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
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
      val result = for {
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id

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
      val result = for {
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
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
      val result = for {
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id

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
      val result = for {
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
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
      val result = for {
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
    }

    "receive a POST request with a JSON body with correct data and insert it into the database. (with timezone)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "RunOnce",
            "startDateAndTime": "2030-01-01 00:00:00",
            "timezone": "EST"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val task = Await.result(taskRepo.selectTask(id), Duration.Inf)
      task.isDefined mustBe true
      task.get.startDateAndTime.toString mustBe "Some(Tue Jan 01 05:00:00 GMT 2030)"
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
      val result = for {
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
      val result = for {
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
      val result = for {
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
      val result = for {
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
      val result = for {
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
      val result = for {
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
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
      val result = for {
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
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
      val result = for {
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
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
      val result = for {
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
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
      val result = for {
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
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
      val result = for {
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
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
      val result = for {
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
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
      val result = for {
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
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
      val result = for {
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
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
      val result = for {
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
    }

    "receive a POST request with a JSON body with correct periodic task data and insert it into the database. (with timezone)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2019-07-01 00:00:00",
            "periodType": "Minutely",
            "period": 2,
            "endDateAndTime": "01-01-2020 00:00:00",
            "timezone": "PST"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val task = Await.result(taskRepo.selectTask(id), Duration.Inf)
      task.isDefined mustBe true
      task.get.startDateAndTime.get.toString mustBe "Mon Jul 01 08:00:00 BST 2019"
      task.get.endDateAndTime.get.toString mustBe "Wed Jan 01 08:00:00 GMT 2020"
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
      val result = for {
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
      val result = for {
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
      val result = for {
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
      val result = for {
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
      val result = for {
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
      val result = for {
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
      val result = for {
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
      val result = for {
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
      val result = for {
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
      val result = for {
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
      bodyText mustBe "[" + Json.toJsObject(invalidCreateTaskFormat).toString + "]"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with exclusionDate)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "exclusionDate": "2035-01-01 00:00:00"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.exclusionDate.get.toString mustBe "Mon Jan 01 00:00:00 GMT 2035"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with day)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "day": 15
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.day.get.toString mustBe "15"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with dayOfWeek)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "dayOfWeek": 3
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.dayOfWeek.get.toString mustBe "3"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with dayType)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "dayType": "Weekday"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.dayType.get.toString mustBe "Weekday"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with month)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "month": 9
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.month.get.toString mustBe "9"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with year)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "year": 2031
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.year.get.toString mustBe "2031"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with day and dayOfWeek)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "day": 25,
                "dayOfWeek": 2
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.day.get.toString mustBe "25"
      exclusion.get.dayOfWeek.get.toString mustBe "2"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with day and dayType)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "day": 12,
                "dayType": "Weekend"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.day.get.toString mustBe "12"
      exclusion.get.dayType.get.toString mustBe "Weekend"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with day and month)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "day": 2,
                "month": 5
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.day.get.toString mustBe "2"
      exclusion.get.month.get.toString mustBe "5"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with day and year)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "day": 31,
                "year": 2033
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.day.get.toString mustBe "31"
      exclusion.get.year.get.toString mustBe "2033"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with dayOfWeek and dayType)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "dayOfWeek": 6,
                "dayType": "Weekday"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.dayOfWeek.get.toString mustBe "6"
      exclusion.get.dayType.get.toString mustBe "Weekday"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with dayOfWeek and month)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "dayOfWeek": 7,
                "month": 7
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.dayOfWeek.get.toString mustBe "7"
      exclusion.get.month.get.toString mustBe "7"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with dayOfWeek and year)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "dayOfWeek": 2,
                "year": 2034
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.dayOfWeek.get.toString mustBe "2"
      exclusion.get.year.get.toString mustBe "2034"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with dayType and month)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "dayType": "Weekday",
                "month": 10
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.dayType.get.toString mustBe "Weekday"
      exclusion.get.month.get.toString mustBe "10"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with dayType and year)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "dayType": "Weekend",
                "year": 2037
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.dayType.get.toString mustBe "Weekend"
      exclusion.get.year.get.toString mustBe "2037"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with month and year)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "month": 8,
                "year": 2039
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.month.get.toString mustBe "8"
      exclusion.get.year.get.toString mustBe "2039"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with day, dayOfWeek and dayType)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "day": 28,
                "dayOfWeek": 1,
                "dayType": "Weekend"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.day.get.toString mustBe "28"
      exclusion.get.dayOfWeek.get.toString mustBe "1"
      exclusion.get.dayType.get.toString mustBe "Weekend"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with day, dayOfWeek and month)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "day": 28,
                "dayOfWeek": 4,
                "month": 3
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.day.get.toString mustBe "28"
      exclusion.get.dayOfWeek.get.toString mustBe "4"
      exclusion.get.month.get.toString mustBe "3"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with day, dayOfWeek and year)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "day": 28,
                "dayOfWeek": 2,
                "year": 2038
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.day.get.toString mustBe "28"
      exclusion.get.dayOfWeek.get.toString mustBe "2"
      exclusion.get.year.get.toString mustBe "2038"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with dayOfWeek, dayType and month)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "dayOfWeek": 3,
                "dayType": "Weekday",
                "month": 4
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.dayOfWeek.get.toString mustBe "3"
      exclusion.get.dayType.get.toString mustBe "Weekday"
      exclusion.get.month.get.toString mustBe "4"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with dayOfWeek, dayType and year)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "dayOfWeek": 4,
                "dayType": "Weekday",
                "year": 2031
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.dayOfWeek.get.toString mustBe "4"
      exclusion.get.dayType.get.toString mustBe "Weekday"
      exclusion.get.year.get.toString mustBe "2031"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with dayType, month and year)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "dayType": "Weekend",
                "month": 2,
                "year": 2040
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.dayType.get.toString mustBe "Weekend"
      exclusion.get.month.get.toString mustBe "2"
      exclusion.get.year.get.toString mustBe "2040"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with day, dayOfWeek, dayType and month)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "day": 23,
                "dayOfWeek": 5,
                "dayType": "Weekday",
                "month": 8
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.day.get.toString mustBe "23"
      exclusion.get.dayOfWeek.get.toString mustBe "5"
      exclusion.get.dayType.get.toString mustBe "Weekday"
      exclusion.get.month.get.toString mustBe "8"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with day, dayOfWeek, dayType and year)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "day": 16,
                "dayOfWeek": 6,
                "dayType": "Weekday",
                "year": 2032
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.day.get.toString mustBe "16"
      exclusion.get.dayOfWeek.get.toString mustBe "6"
      exclusion.get.dayType.get.toString mustBe "Weekday"
      exclusion.get.year.get.toString mustBe "2032"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with day, dayOfWeek, month and year)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "day": 4,
                "dayOfWeek": 3,
                "month": 11,
                "year": 2039
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.day.get.toString mustBe "4"
      exclusion.get.dayOfWeek.get.toString mustBe "3"
      exclusion.get.month.get.toString mustBe "11"
      exclusion.get.year.get.toString mustBe "2039"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with day, dayType, month and year)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "day": 23,
                "dayType": "Weekday",
                "month": 11,
                "year": 2030
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.day.get.toString mustBe "23"
      exclusion.get.dayType.get.toString mustBe "Weekday"
      exclusion.get.month.get.toString mustBe "11"
      exclusion.get.year.get.toString mustBe "2030"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with dayOfWeek, dayType, month and year)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "dayOfWeek": 5,
                "dayType": "Weekday",
                "month": 3,
                "year": 2032
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.dayOfWeek.get.toString mustBe "5"
      exclusion.get.dayType.get.toString mustBe "Weekday"
      exclusion.get.month.get.toString mustBe "3"
      exclusion.get.year.get.toString mustBe "2032"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with day, dayOfWeek, dayType, month and year)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "day": 7,
                "dayOfWeek": 4,
                "dayType": "Weekday",
                "month": 8,
                "year": 2037
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.day.get.toString mustBe "7"
      exclusion.get.dayOfWeek.get.toString mustBe "4"
      exclusion.get.dayType.get.toString mustBe "Weekday"
      exclusion.get.month.get.toString mustBe "8"
      exclusion.get.year.get.toString mustBe "2037"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with day and criteria)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "day": 15,
                "criteria": "First"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.day.get.toString mustBe "15"
      exclusion.get.criteria.get.toString mustBe "First"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with dayOfWeek and criteria)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "dayOfWeek": 3,
                "criteria": "Second"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.dayOfWeek.get.toString mustBe "3"
      exclusion.get.criteria.get.toString mustBe "Second"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with dayType and criteria)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "dayType": "Weekday",
                "criteria": "Third"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.dayType.get.toString mustBe "Weekday"
      exclusion.get.criteria.get.toString mustBe "Third"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with month and criteria)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "month": 2,
                "criteria": "Fourth"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.month.get.toString mustBe "2"
      exclusion.get.criteria.get.toString mustBe "Fourth"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with year and criteria)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "year": 2031,
                "criteria": "Last"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.year.get.toString mustBe "2031"
      exclusion.get.criteria.get.toString mustBe "Last"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with day, dayOfWeek and criteria)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "day": 22,
                "dayOfWeek": 7,
                "criteria": "First"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.day.get.toString mustBe "22"
      exclusion.get.dayOfWeek.get.toString mustBe "7"
      exclusion.get.criteria.get.toString mustBe "First"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with day, dayType and criteria)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "day": 9,
                "dayType": "Weekend",
                "criteria": "Second"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.day.get.toString mustBe "9"
      exclusion.get.dayType.get.toString mustBe "Weekend"
      exclusion.get.criteria.get.toString mustBe "Second"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with day, month and criteria)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "day": 11,
                "month": 9,
                "criteria": "Third"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.day.get.toString mustBe "11"
      exclusion.get.month.get.toString mustBe "9"
      exclusion.get.criteria.get.toString mustBe "Third"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with day, year and criteria)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "day": 29,
                "year": 2035,
                "criteria": "Fourth"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.day.get.toString mustBe "29"
      exclusion.get.year.get.toString mustBe "2035"
      exclusion.get.criteria.get.toString mustBe "Fourth"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with dayOfWeek, dayType and criteria)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "dayOfWeek": 5,
                "dayType": "Weekday",
                "criteria": "Last"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.dayOfWeek.get.toString mustBe "5"
      exclusion.get.dayType.get.toString mustBe "Weekday"
      exclusion.get.criteria.get.toString mustBe "Last"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with dayOfWeek, month and criteria)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "dayOfWeek": 4,
                "month": 5,
                "criteria": "First"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.dayOfWeek.get.toString mustBe "4"
      exclusion.get.month.get.toString mustBe "5"
      exclusion.get.criteria.get.toString mustBe "First"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with dayOfWeek, year and criteria)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "dayOfWeek": 6,
                "year": 2033,
                "criteria": "First"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.dayOfWeek.get.toString mustBe "6"
      exclusion.get.year.get.toString mustBe "2033"
      exclusion.get.criteria.get.toString mustBe "First"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with dayType, month and criteria)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "dayType": "Weekend",
                "month": 12,
                "criteria": "Second"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.dayType.get.toString mustBe "Weekend"
      exclusion.get.month.get.toString mustBe "12"
      exclusion.get.criteria.get.toString mustBe "Second"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with dayType, year and criteria)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "dayType": "Weekday",
                "year": 2039,
                "criteria": "Third"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.dayType.get.toString mustBe "Weekday"
      exclusion.get.year.get.toString mustBe "2039"
      exclusion.get.criteria.get.toString mustBe "Third"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with month, year and criteria)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "month": 7,
                "year": 2036,
                "criteria": "Fourth"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.month.get.toString mustBe "7"
      exclusion.get.year.get.toString mustBe "2036"
      exclusion.get.criteria.get.toString mustBe "Fourth"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with day, dayOfWeek, dayType and criteria)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "day": 14,
                "dayOfWeek": 1,
                "dayType": "Weekend",
                "criteria": "Last"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.day.get.toString mustBe "14"
      exclusion.get.dayOfWeek.get.toString mustBe "1"
      exclusion.get.dayType.get.toString mustBe "Weekend"
      exclusion.get.criteria.get.toString mustBe "Last"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with day, dayOfWeek, month and criteria)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "day": 27,
                "dayOfWeek": 2,
                "month": 1,
                "criteria": "First"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.day.get.toString mustBe "27"
      exclusion.get.dayOfWeek.get.toString mustBe "2"
      exclusion.get.month.get.toString mustBe "1"
      exclusion.get.criteria.get.toString mustBe "First"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with day, dayOfWeek, year and criteria)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "day": 19,
                "dayOfWeek": 6,
                "year": 2037,
                "criteria": "Second"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.day.get.toString mustBe "19"
      exclusion.get.dayOfWeek.get.toString mustBe "6"
      exclusion.get.year.get.toString mustBe "2037"
      exclusion.get.criteria.get.toString mustBe "Second"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with dayOfWeek, dayType, month and criteria)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "dayOfWeek": 2,
                "dayType": "Weekday",
                "month": 10,
                "criteria": "Third"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.dayOfWeek.get.toString mustBe "2"
      exclusion.get.dayType.get.toString mustBe "Weekday"
      exclusion.get.month.get.toString mustBe "10"
      exclusion.get.criteria.get.toString mustBe "Third"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with dayOfWeek, dayType, year and criteria)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "dayOfWeek": 7,
                "dayType": "Weekend",
                "year": 2034,
                "criteria": "Fourth"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.dayOfWeek.get.toString mustBe "7"
      exclusion.get.dayType.get.toString mustBe "Weekend"
      exclusion.get.year.get.toString mustBe "2034"
      exclusion.get.criteria.get.toString mustBe "Fourth"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with dayType, month, year and criteria)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "dayType": "Weekday",
                "month": 6,
                "year": 2038,
                "criteria": "Last"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.dayType.get.toString mustBe "Weekday"
      exclusion.get.month.get.toString mustBe "6"
      exclusion.get.year.get.toString mustBe "2038"
      exclusion.get.criteria.get.toString mustBe "Last"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with day, dayOfWeek, dayType, month and criteria)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "day": 17,
                "dayOfWeek": 1,
                "dayType": "Weekend",
                "month": 6,
                "criteria": "First"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.day.get.toString mustBe "17"
      exclusion.get.dayOfWeek.get.toString mustBe "1"
      exclusion.get.dayType.get.toString mustBe "Weekend"
      exclusion.get.month.get.toString mustBe "6"
      exclusion.get.criteria.get.toString mustBe "First"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with day, dayOfWeek, dayType, year and criteria)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "day": 26,
                "dayOfWeek": 3,
                "dayType": "Weekday",
                "year": 2036,
                "criteria": "Second"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.day.get.toString mustBe "26"
      exclusion.get.dayOfWeek.get.toString mustBe "3"
      exclusion.get.dayType.get.toString mustBe "Weekday"
      exclusion.get.year.get.toString mustBe "2036"
      exclusion.get.criteria.get.toString mustBe "Second"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with day, dayOfWeek, month, year and criteria)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "day": 3,
                "dayOfWeek": 4,
                "month": 4,
                "year": 2040,
                "criteria": "Third"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.day.get.toString mustBe "3"
      exclusion.get.dayOfWeek.get.toString mustBe "4"
      exclusion.get.month.get.toString mustBe "4"
      exclusion.get.year.get.toString mustBe "2040"
      exclusion.get.criteria.get.toString mustBe "Third"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with day, dayType, month, year and criteria)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "day": 20,
                "dayType": "Weekend",
                "month": 5,
                "year": 2033,
                "criteria": "Fourth"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.day.get.toString mustBe "20"
      exclusion.get.dayType.get.toString mustBe "Weekend"
      exclusion.get.month.get.toString mustBe "5"
      exclusion.get.year.get.toString mustBe "2033"
      exclusion.get.criteria.get.toString mustBe "Fourth"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with dayOfWeek, dayType, month, year and criteria)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "dayOfWeek": 3,
                "dayType": "Weekday",
                "month": 11,
                "year": 2038,
                "criteria": "Last"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.dayOfWeek.get.toString mustBe "3"
      exclusion.get.dayType.get.toString mustBe "Weekday"
      exclusion.get.month.get.toString mustBe "11"
      exclusion.get.year.get.toString mustBe "2038"
      exclusion.get.criteria.get.toString mustBe "Last"
    }

    "receive a POST request with a JSON body with the correct periodic task data with correct exclusions and insert it into the database. (with day, dayOfWeek, dayType, month, year and criteria)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Periodic",
            "startDateAndTime": "2030-01-01 00:00:00",
            "periodType": "Minutely",
            "period": 5,
            "endDateAndTime": "2040-01-01 00:00:00",
            "exclusions": [
              {
                "day": 27,
                "dayOfWeek": 7,
                "dayType": "Weekend",
                "month": 2,
                "year": 2030,
                "criteria": "First"
              }
            ]
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- exclusionRepo.selectAllExclusions
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      val exclusion = Await.result(exclusionRepo.selectExclusion(id), Duration.Inf)
      exclusion.isDefined mustBe true
      exclusion.get.day.get.toString mustBe "27"
      exclusion.get.dayOfWeek.get.toString mustBe "7"
      exclusion.get.dayType.get.toString mustBe "Weekend"
      exclusion.get.month.get.toString mustBe "2"
      exclusion.get.year.get.toString mustBe "2030"
      exclusion.get.criteria.get.toString mustBe "First"
    }

    "receive a POST request with a JSON body with the correct periodic task data with incorrect exclusions. (with no arguments)" in {

    }

    "receive a POST request with a JSON body with the correct periodic task data with incorrect exclusions. (with an unknown parameter)" in {

    }

    "receive a POST request with a JSON body with the correct periodic task data with incorrect exclusions. (with exclusionId)" in {

    }

    "receive a POST request with a JSON body with the correct periodic task data with incorrect exclusions. (with taskId)" in {

    }

    "receive a POST request with a JSON body with the correct periodic task data with incorrect exclusions. (with exclusionDate and day)" in {

    }

    "receive a POST request with a JSON body with the correct periodic task data with incorrect exclusions. (with exclusionDate and dayOfWeek)" in {

    }

    "receive a POST request with a JSON body with the correct periodic task data with incorrect exclusions. (with exclusionDate and dayType)" in {

    }

    "receive a POST request with a JSON body with the correct periodic task data with incorrect exclusions. (with exclusionDate and month)" in {

    }

    "receive a POST request with a JSON body with the correct periodic task data with incorrect exclusions. (with exclusionDate and year)" in {

    }

    "receive a POST request with a JSON body with the correct periodic task data with incorrect exclusions. (with only criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with schedulingDate)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "fileName": "test1",
            "taskType": "Personalized",
            "startDateAndTime": "2019-07-01 00:00:00",
            "schedulings": {
              "schedulingDate": ""
            }
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for {
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      val bodyText = contentAsString(routeOption.get)
      result.map(tuple => tuple._2.size mustBe 0)
    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with day)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with dayOfWeek)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with dayType)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with month)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with year)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with day and dayOfWeek)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with day and dayType)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with day and month)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with day and year)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with dayOfWeek and dayType)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with dayOfWeek and month)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with dayOfWeek and year)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with dayType and month)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with dayType and year)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with month and year)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with day, dayOfWeek, dayType)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with day, dayOfWeek, month)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with day, dayOfWeek, year)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with dayOfWeek, dayType, month)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with dayOfWeek, dayType, year)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with dayType, month, year)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with day, dayOfWeek, dayType and month)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with day, dayOfWeek, dayType and year)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with day, dayOfWeek, month and year)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with day, dayType, month and year)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with dayOfWeek, dayType, month and year)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with day, dayOfWeek, dayType, month and year)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with day and criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with dayOfWeek and criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with dayType and criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with month and criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with year and criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with day, dayOfWeek and criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with day, dayType and criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with day, month and criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with day, year and criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with dayOfWeek, dayType and criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with dayOfWeek, month and criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with dayOfWeek, year and criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with dayType, month and criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with dayType, year and criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with month, year and criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with day, dayOfWeek, dayType and criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with day, dayOfWeek, month and criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with day, dayOfWeek, year and criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with dayOfWeek, dayType, month and criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with dayOfWeek, dayType, year and criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with dayType, month, year and criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with day, dayOfWeek, dayType, month and criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with day, dayOfWeek, dayType, year and criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with day, dayOfWeek, month, year and criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with day, dayType, month, year and criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with dayOfWeek, dayType, month, year and criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with day, dayOfWeek, dayType, month, year and criteria)" in {

    }

    "receive a POST request with a JSON body with the correct personalized task data and insert it into the database. (with multiple schedulings)" in {

    }

    "receive a POST request with a JSON body with incorrect personalized task data. (with no scheduling)" in {

    }

    "receive a POST request with a JSON body with incorrect personalized task data. (with a scheduling with no parameters)" in {

    }

    "receive a POST request with a JSON body with incorrect personalized task data. (with a scheduling with an unknown parameter)" in {

    }

    "receive a POST request with a JSON body with incorrect personalized task data. (with schedulingId)" in {

    }

    "receive a POST request with a JSON body with incorrect personalized task data. (with taskId)" in {

    }

    "receive a POST request with a JSON body with incorrect personalized task data. (with exclusionDate and day)" in {

    }

    "receive a POST request with a JSON body with incorrect personalized task data. (with exclusionDate and dayOfWeek)" in {

    }

    "receive a POST request with a JSON body with incorrect personalized task data. (with exclusionDate and dayType)" in {

    }

    "receive a POST request with a JSON body with incorrect personalized task data. (with exclusionDate and month)" in {

    }

    "receive a POST request with a JSON body with incorrect personalized task data. (with exclusionDate and year)" in {

    }

    "receive a POST request with a JSON body with incorrect personalized task data. (with only criteria)" in {

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
      val result = for {
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
      status(routeOption.get) mustBe BAD_REQUEST
      bodyText mustBe "[" + Json.toJsObject(invalidEndpointId) + "]"
      val task = Await.result(taskRepo.selectTask("newUUID"), Duration.Inf)
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
      val task = for {
        _ <- routeOption.get
        res <- taskRepo.selectTask("11231bd5-6f92-496c-9fe7-75bc180467b0")
      } yield res
      val bodyText = contentAsString(routeOption.get)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      task.map {
        elem =>
          elem.isDefined mustBe true
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
      Await.result(taskRepo.selectTask(id), Duration.Inf).isDefined mustBe true
      val fakeRequest = FakeRequest(PATCH, "/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
        .withBody(Json.parse("""
          {
            "fileName":"test4"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val task = for {
        _ <- routeOption.get
        res <- taskRepo.selectTask(id)
      } yield res
      val bodyText = contentAsString(routeOption.get)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      task.map { elem =>
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
      val task = for {
        _ <- routeOption.get
        res <- taskRepo.selectTask(id)
      } yield res
      val bodyText = contentAsString(routeOption.get)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      task.map { elem =>
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
        res <- taskRepo.selectTask(id)
      } yield res
      val bodyText = contentAsString(routeOption.get)
      status(routeOption.get) mustBe BAD_REQUEST
      bodyText mustBe "[" + Json.toJsObject(invalidUpdateTaskFormat) + "]"
      task.map { elem =>
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
        res <- taskRepo.selectTask(id)
      } yield res
      val bodyText = contentAsString(routeOption.get)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
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
        res <- taskRepo.selectTask(id)
      } yield res
      val bodyText = contentAsString(routeOption.get)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
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
        res <- taskRepo.selectTask(id)
      } yield res
      val bodyText = contentAsString(routeOption.get)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
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
        res <- taskRepo.selectTask(id)
      } yield res
      val bodyText = contentAsString(routeOption.get)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      task.map { elem =>
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
        res <- taskRepo.selectTask(id)
      } yield res
      val bodyText = contentAsString(routeOption.get)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
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
      val task = for {
        _ <- routeOption.get
        res <- taskRepo.selectTask(id)
      } yield res
      val bodyText = contentAsString(routeOption.get)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      task.map { elem =>
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
      val task = for {
        _ <- routeOption.get
        res <- taskRepo.selectTask(id)
      } yield res
      val bodyText = contentAsString(routeOption.get)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
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
        res <- taskRepo.selectTask(id)
      } yield res
      val bodyText = contentAsString(routeOption.get)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      task.map { elem =>
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
        res <- taskRepo.selectTask(id)
      } yield res
      val bodyText = contentAsString(routeOption.get)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      task.map { elem =>
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
      val task = for {
        _ <- routeOption.get
        res <- taskRepo.selectTask(id)
      } yield res
      val bodyText = contentAsString(routeOption.get)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
      task.map { elem =>
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
      val task = for {
        _ <- routeOption.get
        res <- taskRepo.selectTask("11231bd5-6f92-496c-9fe7-75bc180467b0")
      } yield res
      val bodyText = contentAsString(routeOption.get)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
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
      val task = for {
        _ <- routeOption.get
        res <- taskRepo.selectTask("11231bd5-6f92-496c-9fe7-75bc180467b0")
      } yield res
      val bodyText = contentAsString(routeOption.get)
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
      val tasks = for {
        _ <- routeOption.get
        task1 <- taskRepo.selectTask("11231bd5-6f92-496c-9fe7-75bc180467b0")
        task2 <- taskRepo.selectTask("asd4")
      } yield (task1, task2)
      val bodyText = contentAsString(routeOption.get)
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
        res <- taskRepo.selectTask("11231bd5-6f92-496c-9fe7-75bc180467b0")
      } yield res
      val bodyText = contentAsString(routeOption.get)
      status(routeOption.get) mustBe OK
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
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
      val task = for {
        _ <- routeOption.get
        res <- taskRepo.selectTask("11231bd5-6f92-496c-9fe7-75bc180467b0")
      } yield res
      val bodyText = contentAsString(routeOption.get)
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
      val tasks = for {
        _ <- routeOption.get
        res <- taskRepo.selectAllTasks
      } yield res
      val bodyText = contentAsString(routeOption.get)
      status(routeOption.get) mustBe NO_CONTENT
      bodyText mustBe ""
      tasks.map(elem => elem.size mustBe 0)
    }
  }

}
