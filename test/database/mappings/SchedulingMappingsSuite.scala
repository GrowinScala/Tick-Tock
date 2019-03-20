package database.mappings

import java.util.UUID

import api.services.{Criteria, DayType}
import org.scalatestplus.play.PlaySpec
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.MySQLProfile.api._
import database.mappings.SchedulingMappings._
import org.scalatest.BeforeAndAfterAll
import api.utils.DateUtils._
import database.mappings.TaskMappings.{TaskRow, selectTaskByTaskId, updateTaskByTaskId}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class SchedulingMappingsSuite extends PlaySpec with BeforeAndAfterAll{

  lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
  val dtbase: Database = appBuilder.injector.instanceOf[Database]

  val schedulingUUID1 = UUID.randomUUID().toString
  val schedulingUUID2 = UUID.randomUUID().toString
  val schedulingUUID3 = UUID.randomUUID().toString
  val schedulingUUID4 = UUID.randomUUID().toString

  val taskUUID1 = UUID.randomUUID().toString
  val taskUUID2 = UUID.randomUUID().toString
  val taskUUID3 = UUID.randomUUID().toString

  override def beforeAll() = {
    val result = for {
      _ <- dtbase.run(createSchedulingsTableAction)
      _ <- dtbase.run(insertScheduling(SchedulingRow(schedulingUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")))))
      _ <- dtbase.run(insertScheduling(SchedulingRow(schedulingUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5))))
      res <- dtbase.run(insertScheduling(SchedulingRow(schedulingUUID3, taskUUID3, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.First))))
    } yield res
    Await.result(result, Duration.Inf)
    Await.result(dtbase.run(selectAllFromSchedulingsTable.result), Duration.Inf).size mustBe 3
  }

  override def afterAll() = {
    Await.result(dtbase.run(dropSchedulingsTableAction), Duration.Inf)
  }

  "SchedulingMappings#selectSchedulingBySchedulingId" should {
    "return the correct SchedulingRow when given an existing schedulingId." in {
      val result1 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID1).result), Duration.Inf)
      result1.size mustBe 1
      result1.head.toString mustBe "SchedulingRow(" + schedulingUUID1 + "," + taskUUID1 + ",Some(Tue Jan 01 00:00:00 GMT 2030),None,None,None,None,None,None)"
      val result2 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID2).result), Duration.Inf)
      result2.size mustBe 1
      result2.head.toString mustBe "SchedulingRow(" + schedulingUUID2 + "," + taskUUID2 + ",None,Some(15),Some(3),None,Some(5),None,None)"
    }
  }

  "SchedulingMappings#selectSchedulingByTaskId" should {
    "return the correct SchedulingRow when given an existing taskId." in {
      val result1 = Await.result(dtbase.run(selectSchedulingByTaskId(taskUUID1).result), Duration.Inf)
      result1.size mustBe 1
      result1.head.toString mustBe "SchedulingRow(" + schedulingUUID1 + "," + taskUUID1 + ",Some(Tue Jan 01 00:00:00 GMT 2030),None,None,None,None,None,None)"
      val result2 = Await.result(dtbase.run(selectSchedulingByTaskId(taskUUID2).result), Duration.Inf)
      result2.size mustBe 1
      result2.head.toString mustBe "SchedulingRow(" + schedulingUUID2 + "," + taskUUID2 + ",None,Some(15),Some(3),None,Some(5),None,None)"
    }
  }

  "SchedulingMappings#selectSchedulingBySchedulingDate" should {
    "return the correct SchedulingRow when given an existing schedulingDate." in {
      val result = Await.result(dtbase.run(selectSchedulingBySchedulingDate(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")).result), Duration.Inf)
      result.size mustBe 1
      result.head.toString mustBe "SchedulingRow(" + schedulingUUID1 + "," + taskUUID1 + ",Some(Tue Jan 01 00:00:00 GMT 2030),None,None,None,None,None,None)"
    }
  }

  "SchedulingMappings#selectSchedulingByDay" should {
    "return the correct SchedulingRow when given an existing day." in {
      val result = Await.result(dtbase.run(selectSchedulingByDay(15).result), Duration.Inf)
      result.size mustBe 1
      result.head.toString mustBe "SchedulingRow(" + schedulingUUID2 + "," + taskUUID2 + ",None,Some(15),Some(3),None,Some(5),None,None)"
    }
  }

  "SchedulingMappings#selectSchedulingByDayOfWeek" should {
    "return the correct SchedulingRow when given an existing dayOfWeek." in {
      val result = Await.result(dtbase.run(selectSchedulingByDayOfWeek(3).result), Duration.Inf)
      result.size mustBe 1
      result.head.toString mustBe "SchedulingRow(" + schedulingUUID2 + "," + taskUUID2 + ",None,Some(15),Some(3),None,Some(5),None,None)"
    }
  }

  "SchedulingMappings#selectSchedulingByDayType" should {
    "return the correct SchedulingRow when given an existing dayType." in {
      val result = Await.result(dtbase.run(selectSchedulingByDayType(DayType.Weekday).result), Duration.Inf)
      result.size mustBe 1
      result.head.toString mustBe "SchedulingRow(" + schedulingUUID3 + "," + taskUUID3 + ",None,None,None,Some(Weekday),None,Some(2030),Some(First))"
    }
  }

  "SchedulingMappings#selectSchedulingByMonth" should {
    "return the correct SchedulingRow when given an existing month." in {
      val result = Await.result(dtbase.run(selectSchedulingByMonth(5).result), Duration.Inf)
      result.size mustBe 1
      result.head.toString mustBe "SchedulingRow(" + schedulingUUID2 + "," + taskUUID2 + ",None,Some(15),Some(3),None,Some(5),None,None)"
    }
  }

  "SchedulingMappings#selectSchedulingByYear" should {
    "return the correct SchedulingRow when given an existing year." in {
      val result = Await.result(dtbase.run(selectSchedulingByYear(2030).result), Duration.Inf)
      result.size mustBe 1
      result.head.toString mustBe "SchedulingRow(" + schedulingUUID3 + "," + taskUUID3 + ",None,None,None,Some(Weekday),None,Some(2030),Some(First))"
    }
  }

  "SchedulingMappings#selectSchedulingByCriteria" should {
    "return the correct SchedulingRow when given an existing taskId." in {
      val result = Await.result(dtbase.run(selectSchedulingByCriteria(Criteria.First).result), Duration.Inf)
      result.size mustBe 1
      result.head.toString mustBe "SchedulingRow(" + schedulingUUID3 + "," + taskUUID3 + ",None,None,None,Some(Weekday),None,Some(2030),Some(First))"
    }
  }

  "SchedulingMappings#insertScheduling" should {
    "insert the given scheduling." in {
      val insertResult = Await.result(dtbase.run(insertScheduling(SchedulingRow(schedulingUUID4, taskUUID1, None, Some(10)))), Duration.Inf)
      insertResult mustBe 1
      val selectResult = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID4).result), Duration.Inf)
      selectResult.head.toString mustBe "SchedulingRow(" + schedulingUUID4 + "," + taskUUID1 + ",None,Some(10),None,None,None,None,None)"
      Await.result(dtbase.run(deleteSchedulingBySchedulingId(schedulingUUID4)), Duration.Inf)
    }

  }

  "SchedulingMappings#updateSchedulingBySchedulingId" should {
    "update a SchedulingRow by giving the corresponding schedulingId." in {
      val updateResult1 = Await.result(dtbase.run(updateSchedulingBySchedulingId(schedulingUUID1, SchedulingRow(schedulingUUID4, taskUUID2, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID4).result), Duration.Inf)
      selectResult1.head.toString mustBe "SchedulingRow(" + schedulingUUID4 + "," + taskUUID2 + ",Some(Tue Jan 01 00:00:00 GMT 2030),None,None,None,None,None,None)"
      val selectResult2 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID1).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateSchedulingBySchedulingId(schedulingUUID4, SchedulingRow(schedulingUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID4).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID1).result), Duration.Inf)
      selectResult4.head.toString mustBe "SchedulingRow(" + schedulingUUID1 + "," + taskUUID1 + ",Some(Tue Jan 01 00:00:00 GMT 2030),None,None,None,None,None,None)"
    }
  }

  "SchedulingMappings#updateSchedulingByTaskId" should {
    "update a SchedulingRow by giving the corresponding taskId." in {
      val updateResult1 = Await.result(dtbase.run(updateSchedulingByTaskId(taskUUID1, SchedulingRow(schedulingUUID4, taskUUID2, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID4).result), Duration.Inf)
      selectResult1.head.toString mustBe "SchedulingRow(" + schedulingUUID4 + "," + taskUUID2 + ",Some(Tue Jan 01 00:00:00 GMT 2030),None,None,None,None,None,None)"
      val selectResult2 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID1).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateSchedulingByTaskId(taskUUID2, SchedulingRow(schedulingUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID4).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID1).result), Duration.Inf)
      selectResult4.head.toString mustBe "SchedulingRow(" + schedulingUUID1 + "," + taskUUID1 + ",Some(Tue Jan 01 00:00:00 GMT 2030),None,None,None,None,None,None)"
    }
  }

  "SchedulingMappings#updateSchedulingBySchedulingDate" should {
    "update a SchedulingRow by giving the corresponding schedulingDate." in {
      val updateResult1 = Await.result(dtbase.run(updateSchedulingBySchedulingDate(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"), SchedulingRow(schedulingUUID4, taskUUID2, Some(stringToDateFormat("2035-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID4).result), Duration.Inf)
      selectResult1.head.toString mustBe "SchedulingRow(" + schedulingUUID4 + "," + taskUUID2 + ",Some(Mon Jan 01 00:00:00 GMT 2035),None,None,None,None,None,None)"
      val selectResult2 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID1).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateSchedulingBySchedulingDate(stringToDateFormat("2035-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"), SchedulingRow(schedulingUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID4).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID1).result), Duration.Inf)
      selectResult4.head.toString mustBe "SchedulingRow(" + schedulingUUID1 + "," + taskUUID1 + ",Some(Tue Jan 01 00:00:00 GMT 2030),None,None,None,None,None,None)"
    }
  }

  "SchedulingMappings#updateSchedulingByDay" should {
    "update a SchedulingRow by giving the corresponding day." in {
      val updateResult1 = Await.result(dtbase.run(updateSchedulingByDay(15, SchedulingRow(schedulingUUID4, taskUUID1, None, Some(10), Some(3), None, Some(5)))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID4).result), Duration.Inf)
      selectResult1.head.toString mustBe "SchedulingRow(" + schedulingUUID4 + "," + taskUUID1 + ",None,Some(10),Some(3),None,Some(5),None,None)"
      val selectResult2 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID2).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateSchedulingByDay(10, SchedulingRow(schedulingUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5)))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID4).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID2).result), Duration.Inf)
      selectResult4.head.toString mustBe "SchedulingRow(" + schedulingUUID2 + "," + taskUUID2 + ",None,Some(15),Some(3),None,Some(5),None,None)"
    }
  }

  "SchedulingMappings#updateSchedulingByDayOfWeek" should {
    "update a SchedulingRow by giving the corresponding dayOfWeek." in {
      val updateResult1 = Await.result(dtbase.run(updateSchedulingByDayOfWeek(3, SchedulingRow(schedulingUUID4, taskUUID1, None, Some(15), Some(5), None, Some(5)))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID4).result), Duration.Inf)
      selectResult1.head.toString mustBe "SchedulingRow(" + schedulingUUID4 + "," + taskUUID1 + ",None,Some(15),Some(5),None,Some(5),None,None)"
      val selectResult2 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID2).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateSchedulingByDayOfWeek(5, SchedulingRow(schedulingUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5)))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID4).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID2).result), Duration.Inf)
      selectResult4.head.toString mustBe "SchedulingRow(" + schedulingUUID2 + "," + taskUUID2 + ",None,Some(15),Some(3),None,Some(5),None,None)"
    }
  }

  "SchedulingMappings#updateSchedulingByDayType" should {
    "update a SchedulingRow by giving the corresponding dayType." in {
      val updateResult1 = Await.result(dtbase.run(updateSchedulingByDayType(DayType.Weekday, SchedulingRow(schedulingUUID4, taskUUID1, None, None, None, Some(DayType.Weekend), None, Some(2030), Some(Criteria.First)))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID4).result), Duration.Inf)
      selectResult1.head.toString mustBe "SchedulingRow(" + schedulingUUID4 + "," + taskUUID1 + ",None,None,None,Some(Weekend),None,Some(2030),Some(First))"
      val selectResult2 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID3).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateSchedulingByDayType(DayType.Weekend, SchedulingRow(schedulingUUID3, taskUUID3, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.First)))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID4).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID3).result), Duration.Inf)
      selectResult4.head.toString mustBe "SchedulingRow(" + schedulingUUID3 + "," + taskUUID3 + ",None,None,None,Some(Weekday),None,Some(2030),Some(First))"
    }
  }

  "SchedulingMappings#updateSchedulingByMonth" should {
    "update a SchedulingRow by giving the corresponding month." in {
      val updateResult1 = Await.result(dtbase.run(updateSchedulingByMonth(5, SchedulingRow(schedulingUUID4, taskUUID1, None, Some(15), Some(5), None, Some(2)))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID4).result), Duration.Inf)
      selectResult1.head.toString mustBe "SchedulingRow(" + schedulingUUID4 + "," + taskUUID1 + ",None,Some(15),Some(5),None,Some(2),None,None)"
      val selectResult2 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID2).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateSchedulingByMonth(2, SchedulingRow(schedulingUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5)))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID4).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID2).result), Duration.Inf)
      selectResult4.head.toString mustBe "SchedulingRow(" + schedulingUUID2 + "," + taskUUID2 + ",None,Some(15),Some(3),None,Some(5),None,None)"
    }
  }

  "SchedulingMappings#updateSchedulingByYear" should {
    "update a SchedulingRow by giving the corresponding year." in {
      val updateResult1 = Await.result(dtbase.run(updateSchedulingByYear(2030, SchedulingRow(schedulingUUID4, taskUUID1, None, None, None, Some(DayType.Weekday), None, Some(2035), Some(Criteria.First)))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID4).result), Duration.Inf)
      selectResult1.head.toString mustBe "SchedulingRow(" + schedulingUUID4 + "," + taskUUID1 + ",None,None,None,Some(Weekday),None,Some(2035),Some(First))"
      val selectResult2 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID3).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateSchedulingByYear(2035, SchedulingRow(schedulingUUID3, taskUUID3, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.First)))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID4).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID3).result), Duration.Inf)
      selectResult4.head.toString mustBe "SchedulingRow(" + schedulingUUID3 + "," + taskUUID3 + ",None,None,None,Some(Weekday),None,Some(2030),Some(First))"
    }
  }

  "SchedulingMappings#updateSchedulingByCriteria" should {
    "update a SchedulingRow by giving the corresponding criteria." in {
      val updateResult1 = Await.result(dtbase.run(updateSchedulingByCriteria(Criteria.First, SchedulingRow(schedulingUUID4, taskUUID1, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.Second)))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID4).result), Duration.Inf)
      selectResult1.head.toString mustBe "SchedulingRow(" + schedulingUUID4 + "," + taskUUID1 + ",None,None,None,Some(Weekday),None,Some(2030),Some(Second))"
      val selectResult2 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID3).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateSchedulingByCriteria(Criteria.Second, SchedulingRow(schedulingUUID3, taskUUID3, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.First)))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID4).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID3).result), Duration.Inf)
      selectResult4.head.toString mustBe "SchedulingRow(" + schedulingUUID3 + "," + taskUUID3 + ",None,None,None,Some(Weekday),None,Some(2030),Some(First))"
    }
  }

  "SchedulingMappings#deleteSchedulingBySchedulingId" should {
    "delete a SchedulingRow by giving the corresponding schedulingId." in {
      val selectResult1 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID1).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteSchedulingBySchedulingId(schedulingUUID1)), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(selectSchedulingBySchedulingId(taskUUID1).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      Await.result(dtbase.run(insertScheduling(SchedulingRow(schedulingUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))), Duration.Inf)
    }
  }

  "SchedulingMappings#deleteSchedulingByTaskId" should {
    "delete a SchedulingRow by giving the corresponding taskId." in {
      val selectResult1 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID1).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteSchedulingByTaskId(taskUUID1)), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID1).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      Await.result(dtbase.run(insertScheduling(SchedulingRow(schedulingUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))), Duration.Inf)
    }
  }

  "SchedulingMappings#deleteSchedulingBySchedulingDate" should {
    "delete a SchedulingRow by giving the corresponding schedulingDate." in {
      val selectResult1 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID1).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteSchedulingBySchedulingDate(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID1).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      Await.result(dtbase.run(insertScheduling(SchedulingRow(schedulingUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))), Duration.Inf)
    }
  }

  "SchedulingMappings#deleteSchedulingByDay" should {
    "delete a SchedulingRow by giving the corresponding day." in {
      val selectResult1 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID1).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteSchedulingBySchedulingDate(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID1).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      Await.result(dtbase.run(insertScheduling(SchedulingRow(schedulingUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))), Duration.Inf)
    }
  }

  "SchedulingMappings#deleteSchedulingByDayOfWeek" should {
    "delete a SchedulingRow by giving the corresponding dayOfWeek." in {
      val selectResult1 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID2).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteSchedulingByDayOfWeek(3)), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID2).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      Await.result(dtbase.run(insertScheduling(SchedulingRow(schedulingUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5)))), Duration.Inf)
    }
  }

  "SchedulingMappings#deleteSchedulingByDayType" should {
    "delete a SchedulingRow by giving the corresponding dayType." in {
      val selectResult1 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID3).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteSchedulingByDayType(DayType.Weekday)), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID3).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      Await.result(dtbase.run(insertScheduling(SchedulingRow(schedulingUUID3, taskUUID3, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.First)))), Duration.Inf)
    }
  }

  "SchedulingMappings#deleteSchedulingByMonth" should {
    "delete a SchedulingRow by giving the corresponding month." in {
      val selectResult1 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID2).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteSchedulingByMonth(5)), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID2).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      Await.result(dtbase.run(insertScheduling(SchedulingRow(schedulingUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5)))), Duration.Inf)
    }
  }

  "SchedulingMappings#deleteSchedulingByYear" should {
    "delete a SchedulingRow by giving the corresponding year." in {
      val selectResult1 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID3).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteSchedulingByYear(2030)), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID3).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      Await.result(dtbase.run(insertScheduling(SchedulingRow(schedulingUUID3, taskUUID3, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.First)))), Duration.Inf)
    }
  }

  "SchedulingMappings#deleteSchedulingByCriteria" should {
    "delete a SchedulingRow by giving the corresponding criteria." in {
      val selectResult1 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID3).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteSchedulingByCriteria(Criteria.First)), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(selectSchedulingBySchedulingId(schedulingUUID3).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      Await.result(dtbase.run(insertScheduling(SchedulingRow(schedulingUUID3, taskUUID3, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.First)))), Duration.Inf)
    }
  }

}
