package database.mappings

import java.util.UUID

import api.utils.DateUtils._
import database.mappings.TaskMappings._
import org.scalatest.{ AsyncWordSpec, BeforeAndAfterAll, BeforeAndAfterEach, MustMatchers }
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext }

class TaskMappingsSuite extends AsyncWordSpec with BeforeAndAfterAll with BeforeAndAfterEach with MustMatchers {

  private lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
  private val dtbase: Database = appBuilder.injector.instanceOf[Database]
  implicit val ec: ExecutionContext = ExecutionContext.global

  private val taskUUID1 = UUID.randomUUID().toString
  private val taskUUID2 = UUID.randomUUID().toString
  private val taskUUID3 = UUID.randomUUID().toString
  private val taskUUID4 = UUID.randomUUID().toString

  private val fileUUID1 = UUID.randomUUID().toString
  private val fileUUID2 = UUID.randomUUID().toString
  private val fileUUID3 = UUID.randomUUID().toString

  private val timeFormat = "yyyy-MM-dd HH:mm:ss"

  override def beforeAll: Unit = {
    Await.result(dtbase.run(createTasksTableAction), Duration.Inf)
    Await.result(dtbase.run(insertTask(TaskRow(taskUUID1, fileUUID1, 0))), Duration.Inf) //runOnce task
    Await.result(dtbase.run(insertTask(TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), None, Some(10), Some(10)))), Duration.Inf) //periodic task
    Await.result(dtbase.run(insertTask(TaskRow(taskUUID3, fileUUID3, 7, None, Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), Some(stringToDateFormat("2040-01-01 00:00:00", timeFormat)), None, None, Some("PST")))), Duration.Inf) //personalized task
  }

  override def afterAll: Unit = {
    Await.result(dtbase.run(dropTasksTableAction), Duration.Inf)
  }

  "TaskMappings#selectTaskByTaskId" should {
    "return the correct TaskRow when given an existing taskId." in {

      for {
        result1 <- dtbase.run(getTaskByTaskId(taskUUID1).result)
        result2 <- dtbase.run(getTaskByTaskId(taskUUID2).result)
      } yield {
        result1.size mustBe 1
        result1.head mustBe TaskRow(taskUUID1, fileUUID1, 0, None, None, None, None, None, None)
        result2.size mustBe 1
        result2.head mustBe TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), None, Some(10), Some(10), None)
      }
    }
  }

  "TaskMappings#selectTaskByFileId" should {
    "return the correct TaskRow when given an existing fileId." in {

      for {
        result1 <- dtbase.run(getTaskByFileId(fileUUID1).result)
        result2 <- dtbase.run(getTaskByFileId(fileUUID2).result)
      } yield {
        result1.size mustBe 1
        result1.head mustBe TaskRow(taskUUID1, fileUUID1, 0, None, None, None, None, None, None)
        result2.size mustBe 1
        result2.head mustBe TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), None, Some(10), Some(10), None)
      }
    }
  }

  "TaskMappings#selectTaskByPeriod" should {
    "return the correct TaskRow when given an existing period." in {

      for {
        result1 <- dtbase.run(getTaskByPeriod(0).result)
        result2 <- dtbase.run(getTaskByPeriod(4).result)
      } yield {
        result1.size mustBe 1
        result1.head mustBe TaskRow(taskUUID1, fileUUID1, 0, None, None, None, None, None, None)
        result2.size mustBe 1
        result2.head mustBe TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), None, Some(10), Some(10), None)
      }
    }
  }

  "TaskMappings#selectTaskByValue" should {
    "return the correct TaskRow when given an existing value." in {

      for {
        result1 <- dtbase.run(getTaskByValue(3).result)
      } yield {
        result1.size mustBe 1
        result1.head mustBe TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), None, Some(10), Some(10), None)
      }
    }
  }

  "TaskMappings#selectTaskByStartDateAndTime" should {
    "return the correct TaskRow when given an existing startDateAndTime." in {

      for {
        result1 <- dtbase.run(getTaskByStartDateAndTime(stringToDateFormat("2030-01-01 00:00:00", timeFormat)).result)
      } yield {
        result1.size mustBe 2
        result1.contains(TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), None, Some(10), Some(10), None)) mustBe true
      }
    }
  }

  "TaskMappings#selectTaskByEndDateAndTime" should {
    "return the correct TaskRow when given an existing endDateAndTime." in {

      for {
        result1 <- dtbase.run(getTaskByEndDateAndTime(stringToDateFormat("2040-01-01 00:00:00", timeFormat)).result)
      } yield {
        result1.size mustBe 1
        result1.head mustBe TaskRow(taskUUID3, fileUUID3, 7, None, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(stringToDateFormat("2040-01-01 00:00:00", timeFormat)), None, None, Some("PST"))
      }
    }
  }

  "TaskMappings#selectTaskByTotalOccurrences" should {
    "return the correct TaskRow when given an existing totalOccurrences." in {

      for {
        result1 <- dtbase.run(getTaskByTotalOccurrences(10).result)
      } yield {
        result1.size mustBe 1
        result1.head mustBe TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), None, Some(10), Some(10), None)
      }
    }
  }

  "TaskMappings#selectTaskByCurrentOccurrences" should {
    "return the correct TaskRow when given an existing currentOccurrences." in {

      for {
        result1 <- dtbase.run(getTaskByCurrentOccurrences(10).result)
      } yield {
        result1.size mustBe 1
        result1.head mustBe TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), None, Some(10), Some(10), None)
      }
    }
  }

  "TaskMappings#selectTaskByTimezone" should {
    "return the correct TaskRow when given an existing timezone." in {

      for {
        result1 <- dtbase.run(getTaskByTimezone("PST").result)
      } yield {
        result1.size mustBe 1
        result1.head mustBe TaskRow(taskUUID3, fileUUID3, 7, None, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(stringToDateFormat("2040-01-01 00:00:00", timeFormat)), None, None, Some("PST"))
      }
    }
  }
  "TaskMappings#insertTask" should {
    "insert the given TaskRow." in {

      for {
        result1 <- dtbase.run(insertTask(TaskRow(taskUUID4, fileUUID1, 0)))
        result2 <- dtbase.run(getTaskByTaskId(taskUUID4).result)
        _ <- dtbase.run(deleteTaskByTaskId(taskUUID4))
      } yield {
        result1 mustBe 1
        result2.head mustBe TaskRow(taskUUID4, fileUUID1, 0, None, None, None, None, None, None)
      }
    }
  }

  "TaskMappings#updateTaskByFileId" should {
    "update a TaskRow by giving the corresponding fileId." in {

      for {
        result1 <- dtbase.run(updateTaskByFileId(taskUUID1, fileUUID2))
        result2 <- dtbase.run(getTaskByTaskId(taskUUID1).result)
        result3 <- dtbase.run(updateTaskByFileId(taskUUID1, fileUUID1))
        result4 <- dtbase.run(getTaskByTaskId(taskUUID1).result)
      } yield {
        result1 mustBe 1
        result2.head mustBe TaskRow(taskUUID1, fileUUID2, 0, None, None, None, None, None, None)
        result3 mustBe 1
        result4.head mustBe TaskRow(taskUUID1, fileUUID1, 0, None, None, None, None, None, None)
      }
    }
  }

  "TaskMappings#updateTaskByPeriod" should {
    "update a TaskRow by giving the corresponding period." in {

      for {
        result1 <- dtbase.run(updateTaskByPeriod(taskUUID2, 5))
        result2 <- dtbase.run(getTaskByTaskId(taskUUID2).result)
        result3 <- dtbase.run(updateTaskByPeriod(taskUUID2, 4))
        result4 <- dtbase.run(getTaskByTaskId(taskUUID2).result)
      } yield {
        result1 mustBe 1
        result2.head mustBe TaskRow(taskUUID2, fileUUID2, 5, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), None, Some(10), Some(10))
        result3 mustBe 1
        result4.head mustBe TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), None, Some(10), Some(10))
      }
    }
  }

  "TaskMappings#updateTaskByValue" should {
    "update a TaskRow by giving the corresponding value." in {

      for {
        result1 <- dtbase.run(updateTaskByValue(taskUUID2, 5))
        result2 <- dtbase.run(getTaskByTaskId(taskUUID2).result)
        result3 <- dtbase.run(updateTaskByValue(taskUUID2, 3))
        result4 <- dtbase.run(getTaskByTaskId(taskUUID2).result)
      } yield {
        result1 mustBe 1
        result2.head mustBe TaskRow(taskUUID2, fileUUID2, 4, Some(5), Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), None, Some(10), Some(10))
        result3 mustBe 1
        result4.head mustBe TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), None, Some(10), Some(10))
      }
    }
  }

  "TaskMappings#updateTaskByStartDateAndTime" should {
    "update a TaskRow by giving the corresponding startDateAndTime." in {

      for {
        result1 <- dtbase.run(updateTaskByStartDateAndTime(taskUUID2, stringToDateFormat("2035-01-01 00:00:00", timeFormat)))
        result2 <- dtbase.run(getTaskByTaskId(taskUUID2).result)
        result3 <- dtbase.run(updateTaskByStartDateAndTime(taskUUID2, stringToDateFormat("2030-01-01 00:00:00", timeFormat)))
        result4 <- dtbase.run(getTaskByTaskId(taskUUID2).result)
      } yield {
        result1 mustBe 1
        result2.head mustBe TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2035-01-01 00:00:00", timeFormat)), None, Some(10), Some(10))
        result3 mustBe 1
        result4.head mustBe TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), None, Some(10), Some(10))
      }
    }
  }

  "TaskMappings#updateTaskByEndDateAndTime" should {
    "update a TaskRow by giving the corresponding endDateAndTime." in {

      for {
        result1 <- dtbase.run(updateTaskByEndDateAndTime(taskUUID3, stringToDateFormat("2045-01-01 00:00:00", timeFormat)))
        result2 <- dtbase.run(getTaskByTaskId(taskUUID3).result)
        result3 <- dtbase.run(updateTaskByEndDateAndTime(taskUUID3, stringToDateFormat("2040-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")))
        result4 <- dtbase.run(getTaskByTaskId(taskUUID3).result)
      } yield {
        result1 mustBe 1
        result2.head mustBe TaskRow(taskUUID3, fileUUID3, 7, None, Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), Some(stringToDateFormat("2045-01-01 00:00:00", timeFormat)), None, None, Some("PST"))
        result3 mustBe 1
        result4.head mustBe TaskRow(taskUUID3, fileUUID3, 7, None, Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), Some(stringToDateFormat("2040-01-01 00:00:00", timeFormat)), None, None, Some("PST"))
      }
    }
  }

  "TaskMappings#updateTaskByTotalOccurrences" should {
    "update a TaskRow by giving the corresponding totalOccurrences." in {

      for {
        result1 <- dtbase.run(updateTaskByTotalOccurrences(taskUUID2, 5))
        result2 <- dtbase.run(getTaskByTaskId(taskUUID2).result)
        result3 <- dtbase.run(updateTaskByTotalOccurrences(taskUUID2, 10))
        result4 <- dtbase.run(getTaskByTaskId(taskUUID2).result)
      } yield {
        result1 mustBe 1
        result2.head mustBe TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), None, Some(5), Some(10))
        result3 mustBe 1
        result4.head mustBe TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), None, Some(10), Some(10))
      }
    }
  }

  "TaskMappings#updateTaskByCurrentOccurrences" should {
    "update a TaskRow by giving the corresponding currentOccurrences." in {

      for {
        result1 <- dtbase.run(updateTaskByCurrentOccurrences(taskUUID2, 3))
        result2 <- dtbase.run(getTaskByTaskId(taskUUID2).result)
        result3 <- dtbase.run(updateTaskByCurrentOccurrences(taskUUID2, 10))
        result4 <- dtbase.run(getTaskByTaskId(taskUUID2).result)
      } yield {
        result1 mustBe 1
        result2.head mustBe TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), None, Some(10), Some(3))
        result3 mustBe 1
        result4.head mustBe TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), None, Some(10), Some(10))
      }
    }
  }

  "TaskMappings#updateTaskByTimezone" should {
    "update a TaskRow by giving the corresponding timezone." in {

      for {
        result1 <- dtbase.run(updateTaskByTimezone(taskUUID3, "EST"))
        result2 <- dtbase.run(getTaskByTaskId(taskUUID3).result)
        result3 <- dtbase.run(updateTaskByTimezone(taskUUID3, "PST"))
        result4 <- dtbase.run(getTaskByTaskId(taskUUID3).result)
      } yield {
        result1 mustBe 1
        result2.head mustBe TaskRow(taskUUID3, fileUUID3, 7, None, Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), Some(stringToDateFormat("2040-01-01 00:00:00", timeFormat)), None, None, Some("EST"))
        result3 mustBe 1
        result4.head mustBe TaskRow(taskUUID3, fileUUID3, 7, None, Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), Some(stringToDateFormat("2040-01-01 00:00:00", timeFormat)), None, None, Some("PST"))
      }
    }
  }

  "TaskMappings#deleteTaskByTaskId" should {
    "delete a TaskRow by giving the corresponding taskId." in {

      for {
        result1 <- dtbase.run(getTaskByTaskId(taskUUID1).result)
        result2 <- dtbase.run(deleteTaskByTaskId(taskUUID1))
        result3 <- dtbase.run(getTaskByTaskId(taskUUID1).result)
        result4 <- dtbase.run(insertTask(TaskRow(taskUUID1, fileUUID1, 0)))
      } yield {
        result1.nonEmpty mustBe true
        result2 mustBe 1
        result3.isEmpty mustBe true
        result4 mustBe 1
      }
    }
  }

  "TaskMappings#deleteTaskByFileId" should {
    "delete a TaskRow by giving the corresponding fileId." in {

      for {
        result1 <- dtbase.run(getTaskByFileId(fileUUID1).result)
        result2 <- dtbase.run(deleteTaskByFileId(fileUUID1))
        result3 <- dtbase.run(getTaskByFileId(fileUUID1).result)
        result4 <- dtbase.run(insertTask(TaskRow(taskUUID1, fileUUID1, 0)))
      } yield {
        result1.nonEmpty mustBe true
        result2 mustBe 1
        result3.isEmpty mustBe true
        result4 mustBe 1
      }
    }
  }

  "TaskMappings#deleteTaskByPeriod" should {
    "delete a TaskRow by giving the corresponding period." in {

      for {
        result1 <- dtbase.run(getTaskByPeriod(0).result)
        result2 <- dtbase.run(deleteTaskByPeriod(0))
        result3 <- dtbase.run(getTaskByPeriod(0).result)
        result4 <- dtbase.run(insertTask(TaskRow(taskUUID1, fileUUID1, 0)))
      } yield {
        result1.nonEmpty mustBe true
        result2 mustBe 1
        result3.isEmpty mustBe true
        result4 mustBe 1
      }
    }
  }

  "TaskMappings#deleteTaskByValue" should {
    "delete a TaskRow by giving the corresponding value." in {

      for {
        result1 <- dtbase.run(getTaskByValue(3).result)
        result2 <- dtbase.run(deleteTaskByValue(3))
        result3 <- dtbase.run(getTaskByValue(3).result)
        result4 <- dtbase.run(insertTask(TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), None, Some(10), Some(10))))
      } yield {
        result1.nonEmpty mustBe true
        result2 mustBe 1
        result3.isEmpty mustBe true
        result4 mustBe 1
      }
    }
  }

  "TaskMappings#deleteTaskByStartDateAndTime" should {
    "delete a TaskRow by giving the corresponding startDateAndTime." in {

      for {
        result1 <- dtbase.run(getTaskByStartDateAndTime(stringToDateFormat("2030-01-01 00:00:00", timeFormat)).result)
        result2 <- dtbase.run(deleteTaskByStartDateAndTime(stringToDateFormat("2030-01-01 00:00:00", timeFormat)))
        result3 <- dtbase.run(getTaskByStartDateAndTime(stringToDateFormat("2030-01-01 00:00:00", timeFormat)).result)
        result4 <- dtbase.run(insertTask(TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), None, Some(10), Some(10))))
        _ <- dtbase.run(insertTask(TaskRow(taskUUID3, fileUUID3, 7, None, Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), Some(stringToDateFormat("2040-01-01 00:00:00", timeFormat)), None, None, Some("PST"))))
      } yield {
        result1.nonEmpty mustBe true
        result2 mustBe 2
        result3.isEmpty mustBe true
        result4 mustBe 1
      }
    }
  }

  "TaskMappings#deleteTaskByEndDateAndTime" should {
    "delete a TaskRow by giving the corresponding endDateAndTime." in {

      for {
        result1 <- dtbase.run(getTaskByEndDateAndTime(stringToDateFormat("2040-01-01 00:00:00", timeFormat)).result)
        result2 <- dtbase.run(deleteTaskByEndDateAndTime(stringToDateFormat("2040-01-01 00:00:00", timeFormat)))
        result3 <- dtbase.run(getTaskByEndDateAndTime(stringToDateFormat("2040-01-01 00:00:00", timeFormat)).result)
        result4 <- dtbase.run(insertTask(TaskRow(taskUUID3, fileUUID3, 7, None, Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), Some(stringToDateFormat("2040-01-01 00:00:00", timeFormat)), None, None, Some("PST"))))
      } yield {
        result1.nonEmpty mustBe true
        result2 mustBe 1
        result3.isEmpty mustBe true
        result4 mustBe 1
      }
    }
  }

  "TaskMappings#deleteTaskByTotalOccurrences" should {
    "delete a TaskRow by giving the corresponding totalOccurrences." in {

      for {
        result1 <- dtbase.run(getTaskByTotalOccurrences(10).result)
        result2 <- dtbase.run(deleteTaskByTotalOccurrences(10))
        result3 <- dtbase.run(getTaskByTotalOccurrences(10).result)
        result4 <- dtbase.run(insertTask(TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), None, Some(10), Some(10))))
      } yield {
        result1.nonEmpty mustBe true
        result2 mustBe 1
        result3.isEmpty mustBe true
        result4 mustBe 1
      }
    }
  }

  "TaskMappings#deleteTaskByCurrentOccurrences" should {
    "delete a TaskRow by giving the corresponding currentOccurrences." in {

      for {
        result1 <- dtbase.run(getTaskByCurrentOccurrences(10).result)
        result2 <- dtbase.run(deleteTaskByCurrentOccurrences(10))
        result3 <- dtbase.run(getTaskByCurrentOccurrences(10).result)
        result4 <- dtbase.run(insertTask(TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), None, Some(10), Some(10))))
      } yield {
        result1.nonEmpty mustBe true
        result2 mustBe 1
        result3.isEmpty mustBe true
        result4 mustBe 1
      }
    }
  }

  "TaskMappings$deleteTaskByTimezone" should {
    "delete a TaskRow by giving the corresponding timezone." in {

      for {
        result1 <- dtbase.run(getTaskByTimezone("PST").result)
        result2 <- dtbase.run(deleteTaskByTimezone("PST"))
        result3 <- dtbase.run(getTaskByTimezone("PST").result)
        result4 <- dtbase.run(insertTask(TaskRow(taskUUID3, fileUUID3, 7, None, Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), Some(stringToDateFormat("2040-01-01 00:00:00", timeFormat)), None, None, Some("PST"))))
      } yield {
        result1.nonEmpty mustBe true
        result2 mustBe 1
        result3.isEmpty mustBe true
        result4 mustBe 1
      }
    }
  }

}