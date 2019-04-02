package database.mappings

import java.util.UUID

import api.services.{ Criteria, DayType }
import api.utils.DateUtils.{ stringToDateFormat, _ }
import database.mappings.ExclusionMappings._
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ExclusionMappingsSuite extends PlaySpec with BeforeAndAfterAll {

  lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
  val dtbase: Database = appBuilder.injector.instanceOf[Database]

  val exclusionUUID1 = UUID.randomUUID().toString
  val exclusionUUID2 = UUID.randomUUID().toString
  val exclusionUUID3 = UUID.randomUUID().toString
  val exclusionUUID4 = UUID.randomUUID().toString

  val taskUUID1 = UUID.randomUUID().toString
  val taskUUID2 = UUID.randomUUID().toString
  val taskUUID3 = UUID.randomUUID().toString

  override def beforeAll() = {
    val result = for {
      _ <- dtbase.run(createExclusionsTableAction)
      _ <- dtbase.run(insertExclusion(ExclusionRow(exclusionUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")))))
      _ <- dtbase.run(insertExclusion(ExclusionRow(exclusionUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5))))
      res <- dtbase.run(insertExclusion(ExclusionRow(exclusionUUID3, taskUUID3, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.First))))
    } yield res
    Await.result(result, Duration.Inf)
    Await.result(dtbase.run(selectAllFromExclusionsTable.result), Duration.Inf).size mustBe 3
  }

  override def afterAll() = {
    Await.result(dtbase.run(dropExclusionsTableAction), Duration.Inf)
  }

  "ExclusionMappings#selectExclusionByExclusionId" should {
    "return the correct ExclusionRow when given an existing exclusionId." in {
      val result1 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID1).result), Duration.Inf)
      result1.size mustBe 1
      result1.head.toString mustBe "ExclusionRow(" + exclusionUUID1 + "," + taskUUID1 + ",Some(Tue Jan 01 00:00:00 GMT 2030),None,None,None,None,None,None)"
      val result2 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID2).result), Duration.Inf)
      result2.size mustBe 1
      result2.head.toString mustBe "ExclusionRow(" + exclusionUUID2 + "," + taskUUID2 + ",None,Some(15),Some(3),None,Some(5),None,None)"
    }
  }

  "ExclusionMappings#selectExclusionByTaskId" should {
    "return the correct ExclusionRow when given an existing taskId." in {
      val result1 = Await.result(dtbase.run(getExclusionByTaskId(taskUUID1).result), Duration.Inf)
      result1.size mustBe 1
      result1.head.toString mustBe "ExclusionRow(" + exclusionUUID1 + "," + taskUUID1 + ",Some(Tue Jan 01 00:00:00 GMT 2030),None,None,None,None,None,None)"
      val result2 = Await.result(dtbase.run(getExclusionByTaskId(taskUUID2).result), Duration.Inf)
      result2.size mustBe 1
      result2.head.toString mustBe "ExclusionRow(" + exclusionUUID2 + "," + taskUUID2 + ",None,Some(15),Some(3),None,Some(5),None,None)"
    }
  }

  "ExclusionMappings#selectExclusionBySchedulingDate" should {
    "return the correct ExclusionRow when given an existing schedulingDate." in {
      val result = Await.result(dtbase.run(getExclusionByExclusionDate(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")).result), Duration.Inf)
      result.size mustBe 1
      result.head.toString mustBe "ExclusionRow(" + exclusionUUID1 + "," + taskUUID1 + ",Some(Tue Jan 01 00:00:00 GMT 2030),None,None,None,None,None,None)"
    }
  }

  "ExclusionMappings#selectExclusionByDay" should {
    "return the correct ExclusionRow when given an existing day." in {
      val result = Await.result(dtbase.run(getExclusionByDay(15).result), Duration.Inf)
      result.size mustBe 1
      result.head.toString mustBe "ExclusionRow(" + exclusionUUID2 + "," + taskUUID2 + ",None,Some(15),Some(3),None,Some(5),None,None)"
    }
  }

  "ExclusionMappings#selectExclusionByDayOfWeek" should {
    "return the correct ExclusionRow when given an existing dayOfWeek." in {
      val result = Await.result(dtbase.run(getExclusionByDayOfWeek(3).result), Duration.Inf)
      result.size mustBe 1
      result.head.toString mustBe "ExclusionRow(" + exclusionUUID2 + "," + taskUUID2 + ",None,Some(15),Some(3),None,Some(5),None,None)"
    }
  }

  "ExclusionMappings#selectExclusionByDayType" should {
    "return the correct ExclusionRow when given an existing dayType." in {
      val result = Await.result(dtbase.run(getExclusionByDayType(DayType.Weekday).result), Duration.Inf)
      result.size mustBe 1
      result.head.toString mustBe "ExclusionRow(" + exclusionUUID3 + "," + taskUUID3 + ",None,None,None,Some(Weekday),None,Some(2030),Some(First))"
    }
  }

  "ExclusionMappings#selectExclusionByMonth" should {
    "return the correct ExclusionRow when given an existing month." in {
      val result = Await.result(dtbase.run(getExclusionByMonth(5).result), Duration.Inf)
      result.size mustBe 1
      result.head.toString mustBe "ExclusionRow(" + exclusionUUID2 + "," + taskUUID2 + ",None,Some(15),Some(3),None,Some(5),None,None)"
    }
  }

  "ExclusionMappings#selectExclusionByYear" should {
    "return the correct ExclusionRow when given an existing year." in {
      val result = Await.result(dtbase.run(getExclusionByYear(2030).result), Duration.Inf)
      result.size mustBe 1
      result.head.toString mustBe "ExclusionRow(" + exclusionUUID3 + "," + taskUUID3 + ",None,None,None,Some(Weekday),None,Some(2030),Some(First))"
    }
  }

  "ExclusionMappings#selectExclusionByCriteria" should {
    "return the correct ExclusionRow when given an existing taskId." in {
      val result = Await.result(dtbase.run(getExclusionByCriteria(Criteria.First).result), Duration.Inf)
      result.size mustBe 1
      result.head.toString mustBe "ExclusionRow(" + exclusionUUID3 + "," + taskUUID3 + ",None,None,None,Some(Weekday),None,Some(2030),Some(First))"
    }
  }

  "ExclusionMappings#insertExclusion" should {
    "insert the given exclusion." in {
      val insertResult = Await.result(dtbase.run(insertExclusion(ExclusionRow(exclusionUUID4, taskUUID1, None, Some(10)))), Duration.Inf)
      insertResult mustBe 1
      val selectResult = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID4).result), Duration.Inf)
      selectResult.head.toString mustBe "ExclusionRow(" + exclusionUUID4 + "," + taskUUID1 + ",None,Some(10),None,None,None,None,None)"
      Await.result(dtbase.run(deleteExclusionByExclusionId(exclusionUUID4)), Duration.Inf)
    }

  }

  "ExclusionMappings#updateExclusionByExclusionId" should {
    "update a ExclusionRow by giving the corresponding schedulingId." in {
      val updateResult1 = Await.result(dtbase.run(updateExclusionByExclusionId(exclusionUUID1, ExclusionRow(exclusionUUID4, taskUUID2, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID4).result), Duration.Inf)
      selectResult1.head.toString mustBe "ExclusionRow(" + exclusionUUID4 + "," + taskUUID2 + ",Some(Tue Jan 01 00:00:00 GMT 2030),None,None,None,None,None,None)"
      val selectResult2 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID1).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateExclusionByExclusionId(exclusionUUID4, ExclusionRow(exclusionUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID4).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID1).result), Duration.Inf)
      selectResult4.head.toString mustBe "ExclusionRow(" + exclusionUUID1 + "," + taskUUID1 + ",Some(Tue Jan 01 00:00:00 GMT 2030),None,None,None,None,None,None)"
    }
  }

  "ExclusionMappings#updateExclusionByTaskId" should {
    "update a ExclusionRow by giving the corresponding taskId." in {
      val updateResult1 = Await.result(dtbase.run(updateExclusionByTaskId(taskUUID1, ExclusionRow(exclusionUUID4, taskUUID2, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID4).result), Duration.Inf)
      selectResult1.head.toString mustBe "ExclusionRow(" + exclusionUUID4 + "," + taskUUID2 + ",Some(Tue Jan 01 00:00:00 GMT 2030),None,None,None,None,None,None)"
      val selectResult2 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID1).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateExclusionByTaskId(taskUUID2, ExclusionRow(exclusionUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID4).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID1).result), Duration.Inf)
      selectResult4.head.toString mustBe "ExclusionRow(" + exclusionUUID1 + "," + taskUUID1 + ",Some(Tue Jan 01 00:00:00 GMT 2030),None,None,None,None,None,None)"
    }
  }

  "ExclusionMappings#updateExclusionByExclusionDate" should {
    "update a ExclusionRow by giving the corresponding schedulingDate." in {
      val updateResult1 = Await.result(dtbase.run(updateExclusionByExclusionDate(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"), ExclusionRow(exclusionUUID4, taskUUID2, Some(stringToDateFormat("2035-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID4).result), Duration.Inf)
      selectResult1.head.toString mustBe "ExclusionRow(" + exclusionUUID4 + "," + taskUUID2 + ",Some(Mon Jan 01 00:00:00 GMT 2035),None,None,None,None,None,None)"
      val selectResult2 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID1).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateExclusionByExclusionDate(stringToDateFormat("2035-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"), ExclusionRow(exclusionUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID4).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID1).result), Duration.Inf)
      selectResult4.head.toString mustBe "ExclusionRow(" + exclusionUUID1 + "," + taskUUID1 + ",Some(Tue Jan 01 00:00:00 GMT 2030),None,None,None,None,None,None)"
    }
  }

  "ExclusionMappings#updateExclusionByDay" should {
    "update a ExclusionRow by giving the corresponding day." in {
      val updateResult1 = Await.result(dtbase.run(updateExclusionByDay(15, ExclusionRow(exclusionUUID4, taskUUID1, None, Some(10), Some(3), None, Some(5)))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID4).result), Duration.Inf)
      selectResult1.head.toString mustBe "ExclusionRow(" + exclusionUUID4 + "," + taskUUID1 + ",None,Some(10),Some(3),None,Some(5),None,None)"
      val selectResult2 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID2).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateExclusionByDay(10, ExclusionRow(exclusionUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5)))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID4).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID2).result), Duration.Inf)
      selectResult4.head.toString mustBe "ExclusionRow(" + exclusionUUID2 + "," + taskUUID2 + ",None,Some(15),Some(3),None,Some(5),None,None)"
    }
  }

  "ExclusionMappings#updateExclusionByDayOfWeek" should {
    "update a ExclusionRow by giving the corresponding dayOfWeek." in {
      val updateResult1 = Await.result(dtbase.run(updateExclusionByDayOfWeek(3, ExclusionRow(exclusionUUID4, taskUUID1, None, Some(15), Some(5), None, Some(5)))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID4).result), Duration.Inf)
      selectResult1.head.toString mustBe "ExclusionRow(" + exclusionUUID4 + "," + taskUUID1 + ",None,Some(15),Some(5),None,Some(5),None,None)"
      val selectResult2 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID2).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateExclusionByDayOfWeek(5, ExclusionRow(exclusionUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5)))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID4).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID2).result), Duration.Inf)
      selectResult4.head.toString mustBe "ExclusionRow(" + exclusionUUID2 + "," + taskUUID2 + ",None,Some(15),Some(3),None,Some(5),None,None)"
    }
  }

  "ExclusionMappings#updateExclusionByDayType" should {
    "update a ExclusionRow by giving the corresponding dayType." in {
      val updateResult1 = Await.result(dtbase.run(updateExclusionByDayType(DayType.Weekday, ExclusionRow(exclusionUUID4, taskUUID1, None, None, None, Some(DayType.Weekend), None, Some(2030), Some(Criteria.First)))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID4).result), Duration.Inf)
      selectResult1.head.toString mustBe "ExclusionRow(" + exclusionUUID4 + "," + taskUUID1 + ",None,None,None,Some(Weekend),None,Some(2030),Some(First))"
      val selectResult2 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID3).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateExclusionByDayType(DayType.Weekend, ExclusionRow(exclusionUUID3, taskUUID3, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.First)))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID4).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID3).result), Duration.Inf)
      selectResult4.head.toString mustBe "ExclusionRow(" + exclusionUUID3 + "," + taskUUID3 + ",None,None,None,Some(Weekday),None,Some(2030),Some(First))"
    }
  }

  "ExclusionMappings#updateExclusionByMonth" should {
    "update a ExclusionRow by giving the corresponding month." in {
      val updateResult1 = Await.result(dtbase.run(updateExclusionByMonth(5, ExclusionRow(exclusionUUID4, taskUUID1, None, Some(15), Some(5), None, Some(2)))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID4).result), Duration.Inf)
      selectResult1.head.toString mustBe "ExclusionRow(" + exclusionUUID4 + "," + taskUUID1 + ",None,Some(15),Some(5),None,Some(2),None,None)"
      val selectResult2 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID2).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateExclusionByMonth(2, ExclusionRow(exclusionUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5)))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID4).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID2).result), Duration.Inf)
      selectResult4.head.toString mustBe "ExclusionRow(" + exclusionUUID2 + "," + taskUUID2 + ",None,Some(15),Some(3),None,Some(5),None,None)"
    }
  }

  "ExclusionMappings#updateExclusionByYear" should {
    "update a ExclusionRow by giving the corresponding year." in {
      val updateResult1 = Await.result(dtbase.run(updateExclusionByYear(2030, ExclusionRow(exclusionUUID4, taskUUID1, None, None, None, Some(DayType.Weekday), None, Some(2035), Some(Criteria.First)))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID4).result), Duration.Inf)
      selectResult1.head.toString mustBe "ExclusionRow(" + exclusionUUID4 + "," + taskUUID1 + ",None,None,None,Some(Weekday),None,Some(2035),Some(First))"
      val selectResult2 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID3).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateExclusionByYear(2035, ExclusionRow(exclusionUUID3, taskUUID3, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.First)))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID4).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID3).result), Duration.Inf)
      selectResult4.head.toString mustBe "ExclusionRow(" + exclusionUUID3 + "," + taskUUID3 + ",None,None,None,Some(Weekday),None,Some(2030),Some(First))"
    }
  }

  "ExclusionMappings#updateExclusionByCriteria" should {
    "update a ExclusionRow by giving the corresponding criteria." in {
      val updateResult1 = Await.result(dtbase.run(updateExclusionByCriteria(Criteria.First, ExclusionRow(exclusionUUID4, taskUUID1, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.Second)))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID4).result), Duration.Inf)
      selectResult1.head.toString mustBe "ExclusionRow(" + exclusionUUID4 + "," + taskUUID1 + ",None,None,None,Some(Weekday),None,Some(2030),Some(Second))"
      val selectResult2 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID3).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateExclusionByCriteria(Criteria.Second, ExclusionRow(exclusionUUID3, taskUUID3, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.First)))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID4).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID3).result), Duration.Inf)
      selectResult4.head.toString mustBe "ExclusionRow(" + exclusionUUID3 + "," + taskUUID3 + ",None,None,None,Some(Weekday),None,Some(2030),Some(First))"
    }
  }

  "ExclusionMappings#deleteExclusionByExclusionId" should {
    "delete a ExclusionRow by giving the corresponding schedulingId." in {
      val selectResult1 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID1).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteExclusionByExclusionId(exclusionUUID1)), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID1).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      Await.result(dtbase.run(insertExclusion(ExclusionRow(exclusionUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))), Duration.Inf)
    }
  }

  "ExclusionMappings#deleteExclusionByTaskId" should {
    "delete a ExclusionRow by giving the corresponding taskId." in {
      val selectResult1 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID1).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteExclusionByTaskId(taskUUID1)), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID1).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      Await.result(dtbase.run(insertExclusion(ExclusionRow(exclusionUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))), Duration.Inf)
    }
  }

  "ExclusionMappings#deleteExclusionByExclusionDate" should {
    "delete a ExclusionRow by giving the corresponding schedulingDate." in {
      val selectResult1 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID1).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteExclusionByExclusionDate(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID1).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      Await.result(dtbase.run(insertExclusion(ExclusionRow(exclusionUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))), Duration.Inf)
    }
  }

  "ExclusionMappings#deleteExclusionByDay" should {
    "delete a ExclusionRow by giving the corresponding day." in {
      val selectResult1 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID1).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteExclusionByExclusionDate(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID1).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      Await.result(dtbase.run(insertExclusion(ExclusionRow(exclusionUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))), Duration.Inf)
    }
  }

  "ExclusionMappings#deleteExclusionByDayOfWeek" should {
    "delete a ExclusionRow by giving the corresponding dayOfWeek." in {
      val selectResult1 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID2).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteExclusionByDayOfWeek(3)), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID2).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      Await.result(dtbase.run(insertExclusion(ExclusionRow(exclusionUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5)))), Duration.Inf)
    }
  }

  "ExclusionMappings#deleteExclusionByDayType" should {
    "delete a ExclusionRow by giving the corresponding dayType." in {
      val selectResult1 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID3).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteExclusionByDayType(DayType.Weekday)), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID3).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      Await.result(dtbase.run(insertExclusion(ExclusionRow(exclusionUUID3, taskUUID3, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.First)))), Duration.Inf)
    }
  }

  "ExclusionMappings#deleteExclusionByMonth" should {
    "delete a ExclusionRow by giving the corresponding month." in {
      val selectResult1 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID2).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteExclusionByMonth(5)), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID2).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      Await.result(dtbase.run(insertExclusion(ExclusionRow(exclusionUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5)))), Duration.Inf)
    }
  }

  "ExclusionMappings#deleteExclusionByYear" should {
    "delete a ExclusionRow by giving the corresponding year." in {
      val selectResult1 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID3).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteExclusionByYear(2030)), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID3).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      Await.result(dtbase.run(insertExclusion(ExclusionRow(exclusionUUID3, taskUUID3, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.First)))), Duration.Inf)
    }
  }

  "ExclusionMappings#deleteExclusionByCriteria" should {
    "delete a ExclusionRow by giving the corresponding criteria." in {
      val selectResult1 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID3).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteExclusionByCriteria(Criteria.First)), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(getExclusionByExclusionId(exclusionUUID3).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      Await.result(dtbase.run(insertExclusion(ExclusionRow(exclusionUUID3, taskUUID3, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.First)))), Duration.Inf)
    }
  }
}