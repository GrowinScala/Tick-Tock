package database.mappings

import java.util.UUID

import api.services.Criteria._
import api.services.DayType._
import api.services.{ Criteria, DayType }
import api.utils.DateUtils.stringToDateFormat
import database.mappings.ExclusionMappings._
import org.scalatest.Matchers._
import org.scalatest._
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext }

class ExclusionMappingsSuite extends AsyncWordSpec with BeforeAndAfterAll with BeforeAndAfterEach with MustMatchers {

  private lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
  private val dtbase: Database = appBuilder.injector.instanceOf[Database]
  implicit val ec = ExecutionContext.global

  private val exclusionUUID1 = UUID.randomUUID().toString
  private val exclusionUUID2 = UUID.randomUUID().toString
  private val exclusionUUID3 = UUID.randomUUID().toString
  private val exclusionUUID4 = UUID.randomUUID().toString

  private val taskUUID1 = UUID.randomUUID().toString
  private val taskUUID2 = UUID.randomUUID().toString
  private val taskUUID3 = UUID.randomUUID().toString

  override def beforeAll(): Unit = {
    Await.result(dtbase.run(createExclusionsTableAction), Duration.Inf)
    Await.result(dtbase.run(insertExclusion(ExclusionRow(exclusionUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))), Duration.Inf)
    Await.result(dtbase.run(insertExclusion(ExclusionRow(exclusionUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5)))), Duration.Inf)
    Await.result(dtbase.run(insertExclusion(ExclusionRow(exclusionUUID3, taskUUID3, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.First)))), Duration.Inf)
  }

  override def afterAll(): Unit = {
    Await.result(dtbase.run(dropExclusionsTableAction), Duration.Inf)
  }

  "ExclusionMappings#selectExclusionByExclusionId" should {
    "return the correct ExclusionRow when given an existing exclusionId." in {

      val result = for {
        result1 <- dtbase.run(getExclusionByExclusionId(exclusionUUID1).result)
        result2 <- dtbase.run(getExclusionByExclusionId(exclusionUUID2).result)
      } yield List(
        result1.size mustBe 1,
        result1.head mustBe ExclusionRow(exclusionUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, None, None),
        result2.size mustBe 1,
        result2.head mustBe ExclusionRow(exclusionUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5), None, None))

      result.map(assertions => assert(assertions.forall(_ == Succeeded)))
    }
  }

  "ExclusionMappings#selectExclusionByTaskId" should {
    "return the correct ExclusionRow when given an existing taskId." in {

      val result = for {
        result1 <- dtbase.run(getExclusionByTaskId(taskUUID1).result)
        result2 <- dtbase.run(getExclusionByTaskId(taskUUID2).result)
      } yield List(
        result1.size mustBe 1,
        result1.head mustBe ExclusionRow(exclusionUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, None, None),
        result2.size mustBe 1,
        result2.head mustBe ExclusionRow(exclusionUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5), None, None))

      result.map(assertions => assert(assertions.forall(_ == Succeeded)))
    }
  }

  "ExclusionMappings#selectExclusionBySchedulingDate" should {
    "return the correct ExclusionRow when given an existing schedulingDate." in {

      val result = for {
        result <- dtbase.run(getExclusionByExclusionDate(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")).result)
      } yield List(
        result.size mustBe 1,
        result.head mustBe ExclusionRow(exclusionUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, None, None))

      result.map(assertions => assert(assertions.forall(_ == Succeeded)))
    }
  }

  "ExclusionMappings#selectExclusionByDay" should {
    "return the correct ExclusionRow when given an existing day." in {

      val result = for {
        result <- dtbase.run(getExclusionByDay(15).result)
      } yield List(
        result.size mustBe 1,
        result.head mustBe ExclusionRow(exclusionUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5), None, None))

      result.map(assertions => assert(assertions.forall(_ == Succeeded)))
    }
  }

  "ExclusionMappings#selectExclusionByDayOfWeek" should {
    "return the correct ExclusionRow when given an existing dayOfWeek." in {

      val result = for {
        result <- dtbase.run(getExclusionByDayOfWeek(3).result)
      } yield List(
        result.size mustBe 1,
        result.head mustBe ExclusionRow(exclusionUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5), None, None))

      result.map(assertions => assert(assertions.forall(_ == Succeeded)))
    }
  }

  "ExclusionMappings#selectExclusionByDayType" should {
    "return the correct ExclusionRow when given an existing dayType." in {

      val result = for {
        result <- dtbase.run(getExclusionByDayType(DayType.Weekday).result)
      } yield List(
        result.size mustBe 1,
        result.head mustBe ExclusionRow(exclusionUUID3, taskUUID3, None, None, None, Some(Weekday), None, Some(2030), Some(First)))

      result.map(assertions => assert(assertions.forall(_ == Succeeded)))
    }
  }

  "ExclusionMappings#selectExclusionByMonth" should {
    "return the correct ExclusionRow when given an existing month." in {

      val result = for {
        result <- dtbase.run(getExclusionByMonth(5).result)
      } yield List(
        result.size mustBe 1,
        result.head mustBe ExclusionRow(exclusionUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5), None, None))

      result.map(assertions => assert(assertions.forall(_ == Succeeded)))
    }
  }

  "ExclusionMappings#selectExclusionByYear" should {
    "return the correct ExclusionRow when given an existing year." in {

      val result = for {
        result <- dtbase.run(getExclusionByYear(2030).result)
      } yield List(
        result.size mustBe 1,
        result.head mustBe ExclusionRow(exclusionUUID3, taskUUID3, None, None, None, Some(Weekday), None, Some(2030), Some(First)))

      result.map(assertions => assert(assertions.forall(_ == Succeeded)))
    }
  }

  "ExclusionMappings#selectExclusionByCriteria" should {
    "return the correct ExclusionRow when given an existing taskId." in {

      val result = for {
        result <- dtbase.run(getExclusionByCriteria(Criteria.First).result)
      } yield List(
        result.size mustBe 1,
        result.head mustBe ExclusionRow(exclusionUUID3, taskUUID3, None, None, None, Some(Weekday), None, Some(2030), Some(First)))

      result.map(assertions => assert(assertions.forall(_ == Succeeded)))
    }
  }

  "ExclusionMappings#insertExclusion" should {
    "insert the given exclusion." in {

      val result = for {
        result1 <- dtbase.run(insertExclusion(ExclusionRow(exclusionUUID4, taskUUID1, None, Some(10))))
        result2 <- dtbase.run(getExclusionByExclusionId(exclusionUUID4).result)
        _ <- dtbase.run(deleteExclusionByExclusionId(exclusionUUID4))
      } yield List(
        result1 mustBe 1,
        result2.head mustBe ExclusionRow(exclusionUUID4, taskUUID1, None, Some(10), None, None, None, None, None))

      result.map(assertions => assert(assertions.forall(_ == Succeeded)))
    }
  }

  "ExclusionMappings#updateExclusionByTaskId" should {
    "update a ExclusionRow by giving the corresponding taskId." in {

      val result = for {
        result1 <- dtbase.run(updateExclusionByTaskId(exclusionUUID1, taskUUID2))
        result2 <- dtbase.run(getExclusionByExclusionId(exclusionUUID1).result)
        result3 <- dtbase.run(updateExclusionByTaskId(exclusionUUID1, taskUUID1))
        result4 <- dtbase.run(getExclusionByExclusionId(exclusionUUID1).result)
      } yield List(
        result1 mustBe 1,
        result2.head mustBe ExclusionRow(exclusionUUID1, taskUUID2, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, None, None),
        result3 mustBe 1,
        result4.head mustBe ExclusionRow(exclusionUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, None, None))

      result.map(assertions => assert(assertions.forall(_ == Succeeded)))
    }
  }

  "ExclusionMappings#updateExclusionByExclusionDate" should {
    "update a ExclusionRow by giving the corresponding schedulingDate." in {

      val result = for {
        result1 <- dtbase.run(updateExclusionByExclusionDate(exclusionUUID1, stringToDateFormat("2035-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")))
        result2 <- dtbase.run(getExclusionByExclusionId(exclusionUUID1).result)
        result3 <- dtbase.run(updateExclusionByExclusionDate(exclusionUUID1, stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")))
        result4 <- dtbase.run(getExclusionByExclusionId(exclusionUUID1).result)
      } yield List(
        result1 mustBe 1,
        result2.head mustBe ExclusionRow(exclusionUUID1, taskUUID1, Some(stringToDateFormat("2035-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, None, None),
        result3 mustBe 1,
        result4.head mustBe ExclusionRow(exclusionUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, None, None))

      result.map(assertions => assert(assertions.forall(_ == Succeeded)))
    }
  }

  "ExclusionMappings#updateExclusionByDay" should {
    "update a ExclusionRow by giving the corresponding day." in {

      val result = for {
        result1 <- dtbase.run(updateExclusionByDay(exclusionUUID2, 10))
        result2 <- dtbase.run(getExclusionByExclusionId(exclusionUUID2).result)
        result3 <- dtbase.run(updateExclusionByDay(exclusionUUID2, 15))
        result4 <- dtbase.run(getExclusionByExclusionId(exclusionUUID2).result)
      } yield List(
        result1 mustBe 1,
        result2.head mustBe ExclusionRow(exclusionUUID2, taskUUID2, None, Some(10), Some(3), None, Some(5), None, None),
        result3 mustBe 1,
        result4.head mustBe ExclusionRow(exclusionUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5), None, None))

      result.map(assertions => assert(assertions.forall(_ == Succeeded)))
    }
  }

  "ExclusionMappings#updateExclusionByDayOfWeek" should {
    "update a ExclusionRow by giving the corresponding dayOfWeek." in {

      val result = for {
        result1 <- dtbase.run(updateExclusionByDayOfWeek(exclusionUUID2, 5))
        result2 <- dtbase.run(getExclusionByExclusionId(exclusionUUID2).result)
        result3 <- dtbase.run(updateExclusionByDayOfWeek(exclusionUUID2, 3))
        result4 <- dtbase.run(getExclusionByExclusionId(exclusionUUID2).result)
      } yield List(
        result1 mustBe 1,
        result2.head mustBe ExclusionRow(exclusionUUID2, taskUUID2, None, Some(15), Some(5), None, Some(5), None, None),
        result3 mustBe 1,
        result4.head mustBe ExclusionRow(exclusionUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5), None, None))

      result.map(assertions => assert(assertions.forall(_ == Succeeded)))
    }
  }

  "ExclusionMappings#updateExclusionByDayType" should {
    "update a ExclusionRow by giving the corresponding dayType." in {

      val result = for {
        result1 <- dtbase.run(updateExclusionByDayType(exclusionUUID3, DayType.Weekend))
        result2 <- dtbase.run(getExclusionByExclusionId(exclusionUUID3).result)
        result3 <- dtbase.run(updateExclusionByDayType(exclusionUUID3, DayType.Weekday))
        result4 <- dtbase.run(getExclusionByExclusionId(exclusionUUID3).result)
      } yield List(
        result1 mustBe 1,
        result2.head mustBe ExclusionRow(exclusionUUID3, taskUUID3, None, None, None, Some(Weekend), None, Some(2030), Some(First)),
        result3 mustBe 1,
        result4.head mustBe ExclusionRow(exclusionUUID3, taskUUID3, None, None, None, Some(Weekday), None, Some(2030), Some(First)))

      result.map(assertions => assert(assertions.forall(_ == Succeeded)))
    }
  }

  "ExclusionMappings#updateExclusionByMonth" should {
    "update a ExclusionRow by giving the corresponding month." in {

      val result = for {
        result1 <- dtbase.run(updateExclusionByMonth(exclusionUUID2, 2))
        result2 <- dtbase.run(getExclusionByExclusionId(exclusionUUID2).result)
        result3 <- dtbase.run(updateExclusionByMonth(exclusionUUID2, 5))
        result4 <- dtbase.run(getExclusionByExclusionId(exclusionUUID2).result)
      } yield List(
        result1 mustBe 1,
        result2.head mustBe ExclusionRow(exclusionUUID2, taskUUID2, None, Some(15), Some(3), None, Some(2), None, None),
        result3 mustBe 1,
        result4.head mustBe ExclusionRow(exclusionUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5), None, None))

      result.map(assertions => assert(assertions.forall(_ == Succeeded)))
    }
  }

  "ExclusionMappings#updateExclusionByYear" should {
    "update a ExclusionRow by giving the corresponding year." in {

      val result = for {
        result1 <- dtbase.run(updateExclusionByYear(exclusionUUID3, 2035))
        result2 <- dtbase.run(getExclusionByExclusionId(exclusionUUID3).result)
        result3 <- dtbase.run(updateExclusionByYear(exclusionUUID3, 2030))
        result4 <- dtbase.run(getExclusionByExclusionId(exclusionUUID3).result)
      } yield List(
        result1 mustBe 1,
        result2.head mustBe ExclusionRow(exclusionUUID3, taskUUID3, None, None, None, Some(Weekday), None, Some(2035), Some(First)),
        result3 mustBe 1,
        result4.head mustBe ExclusionRow(exclusionUUID3, taskUUID3, None, None, None, Some(Weekday), None, Some(2030), Some(First)))

      result.map(assertions => assert(assertions.forall(_ == Succeeded)))
    }
  }

  "ExclusionMappings#updateExclusionByCriteria" should {
    "update a ExclusionRow by giving the corresponding criteria." in {

      val result = for {
        result1 <- dtbase.run(updateExclusionByCriteria(exclusionUUID3, Criteria.Second))
        result2 <- dtbase.run(getExclusionByExclusionId(exclusionUUID3).result)
        result3 <- dtbase.run(updateExclusionByCriteria(exclusionUUID3, Criteria.First))
        result4 <- dtbase.run(getExclusionByExclusionId(exclusionUUID3).result)
      } yield List(
        result1 mustBe 1,
        result2.head mustBe ExclusionRow(exclusionUUID3, taskUUID3, None, None, None, Some(Weekday), None, Some(2030), Some(Second)),
        result3 mustBe 1,
        result4.head mustBe ExclusionRow(exclusionUUID3, taskUUID3, None, None, None, Some(Weekday), None, Some(2030), Some(First)))

      result.map(assertions => assert(assertions.forall(_ == Succeeded)))
    }
  }

  "ExclusionMappings#deleteExclusionByExclusionId" should {
    "delete a ExclusionRow by giving the corresponding schedulingId." in {

      val result = for {
        result1 <- dtbase.run(getExclusionByExclusionId(exclusionUUID1).result)
        result2 <- dtbase.run(deleteExclusionByExclusionId(exclusionUUID1))
        result3 <- dtbase.run(getExclusionByExclusionId(exclusionUUID1).result)
        _ <- dtbase.run(insertExclusion(ExclusionRow(exclusionUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")))))
      } yield List(
        result1.nonEmpty mustBe true,
        result2 mustBe 1,
        result3.isEmpty mustBe true)

      result.map(assertions => assert(assertions.forall(_ == Succeeded)))
    }
  }
  /*
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
  }*/
}