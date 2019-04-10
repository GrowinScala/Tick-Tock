package database.mappings

import java.util.UUID

import api.services.{ Criteria, DayType }
import api.utils.DateUtils._
import database.mappings.SchedulingMappings._
import org.scalatest.{ AsyncWordSpec, BeforeAndAfterAll, BeforeAndAfterEach, MustMatchers }
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext }

class SchedulingMappingsSuite extends AsyncWordSpec with BeforeAndAfterAll with BeforeAndAfterEach with MustMatchers {

  private lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
  private val dtbase: Database = appBuilder.injector.instanceOf[Database]
  implicit val ec: ExecutionContext = ExecutionContext.global

  private val schedulingUUID1 = UUID.randomUUID().toString
  private val schedulingUUID2 = UUID.randomUUID().toString
  private val schedulingUUID3 = UUID.randomUUID().toString
  private val schedulingUUID4 = UUID.randomUUID().toString

  private val taskUUID1 = UUID.randomUUID().toString
  private val taskUUID2 = UUID.randomUUID().toString
  private val taskUUID3 = UUID.randomUUID().toString

  private val timeFormat = "yyyy-MM-dd HH:mm:ss"

  override def beforeAll(): Unit = {
    Await.result(dtbase.run(createSchedulingsTableAction), Duration.Inf)
    Await.result(dtbase.run(insertScheduling(SchedulingRow(schedulingUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat))))), Duration.Inf)
    Await.result(dtbase.run(insertScheduling(SchedulingRow(schedulingUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5)))), Duration.Inf)
    Await.result(dtbase.run(insertScheduling(SchedulingRow(schedulingUUID3, taskUUID3, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.First)))), Duration.Inf)
  }

  override def afterAll(): Unit = {
    Await.result(dtbase.run(dropSchedulingsTableAction), Duration.Inf)
  }

  "SchedulingMappings#selectSchedulingBySchedulingId" should {
    "return the correct SchedulingRow when given an existing schedulingId." in {

      for {
        result1 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID1).result)
        result2 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID2).result)
      } yield {
        result1.size mustBe 1
        result1.head mustBe SchedulingRow(schedulingUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), None, None, None, None, None, None)
        result2.size mustBe 1
        result2.head mustBe SchedulingRow(schedulingUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5), None, None)
      }
    }
  }

  "SchedulingMappings#selectSchedulingByTaskId" should {
    "return the correct SchedulingRow when given an existing taskId." in {

      for {
        result1 <- dtbase.run(getSchedulingByTaskId(taskUUID1).result)
        result2 <- dtbase.run(getSchedulingByTaskId(taskUUID2).result)
      } yield {
        result1.size mustBe 1
        result1.head mustBe SchedulingRow(schedulingUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), None, None, None, None, None, None)
        result2.size mustBe 1
        result2.head mustBe SchedulingRow(schedulingUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5), None, None)
      }
    }
  }

  "SchedulingMappings#selectSchedulingBySchedulingDate" should {
    "return the correct SchedulingRow when given an existing schedulingDate." in {

      for {
        result1 <- dtbase.run(getSchedulingBySchedulingDate(stringToDateFormat("2030-01-01 00:00:00", timeFormat)).result)
      } yield {
        result1.size mustBe 1
        result1.head mustBe SchedulingRow(schedulingUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), None, None, None, None, None, None)
      }
    }
  }

  "SchedulingMappings#selectSchedulingByDay" should {
    "return the correct SchedulingRow when given an existing day." in {

      for {
        result1 <- dtbase.run(getSchedulingByDay(15).result)
      } yield {
        result1.size mustBe 1
        result1.head mustBe SchedulingRow(schedulingUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5), None, None)
      }
    }
  }

  "SchedulingMappings#selectSchedulingByDayOfWeek" should {
    "return the correct SchedulingRow when given an existing dayOfWeek." in {

      for {
        result1 <- dtbase.run(getSchedulingByDayOfWeek(3).result)
      } yield {
        result1.size mustBe 1
        result1.head mustBe SchedulingRow(schedulingUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5), None, None)
      }
    }
  }

  "SchedulingMappings#selectSchedulingByDayType" should {
    "return the correct SchedulingRow when given an existing dayType." in {

      for {
        result1 <- dtbase.run(getSchedulingByDayType(DayType.Weekday).result)
      } yield {
        result1.size mustBe 1
        result1.head mustBe SchedulingRow(schedulingUUID3, taskUUID3, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.First))
      }
    }
  }

  "SchedulingMappings#selectSchedulingByMonth" should {
    "return the correct SchedulingRow when given an existing month." in {

      for {
        result1 <- dtbase.run(getSchedulingByMonth(5).result)
      } yield {
        result1.size mustBe 1
        result1.head mustBe SchedulingRow(schedulingUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5), None, None)
      }
    }
  }

  "SchedulingMappings#selectSchedulingByYear" should {
    "return the correct SchedulingRow when given an existing year." in {

      for {
        result1 <- dtbase.run(getSchedulingByYear(2030).result)
      } yield {
        result1.size mustBe 1
        result1.head mustBe SchedulingRow(schedulingUUID3, taskUUID3, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.First))
      }
    }
  }

  "SchedulingMappings#selectSchedulingByCriteria" should {
    "return the correct SchedulingRow when given an existing taskId." in {

      for {
        result1 <- dtbase.run(getSchedulingByCriteria(Criteria.First).result)
      } yield {
        result1.size mustBe 1
        result1.head mustBe SchedulingRow(schedulingUUID3, taskUUID3, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.First))
      }
    }
  }

  "SchedulingMappings#insertScheduling" should {
    "insert the given scheduling." in {

      for {
        result1 <- dtbase.run(insertScheduling(SchedulingRow(schedulingUUID4, taskUUID1, None, Some(10))))
        result2 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID4).result)
        _ <- dtbase.run(deleteSchedulingBySchedulingId(schedulingUUID4))
      } yield {
        result1 mustBe 1
        result2.head mustBe SchedulingRow(schedulingUUID4, taskUUID1, None, Some(10), None, None, None, None, None)
      }
    }
  }

  "SchedulingMappings#updateSchedulingByTaskId" should {
    "update a SchedulingRow by giving the corresponding taskId." in {

      for {
        result1 <- dtbase.run(updateSchedulingByTaskId(schedulingUUID1, taskUUID3))
        result2 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID1).result)
        result3 <- dtbase.run(updateSchedulingByTaskId(schedulingUUID1, taskUUID1))
        result4 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID1).result)
      } yield {
        result1 mustBe 1
        result2.head mustBe SchedulingRow(schedulingUUID1, taskUUID3, Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), None, None, None, None, None, None)
        result3 mustBe 1
        result4.head mustBe SchedulingRow(schedulingUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), None, None, None, None, None, None)
      }
    }
  }

  "SchedulingMappings#updateSchedulingBySchedulingDate" should {
    "update a SchedulingRow by giving the corresponding schedulingDate." in {

      for {
        result1 <- dtbase.run(updateSchedulingBySchedulingDate(schedulingUUID1, stringToDateFormat("2035-01-01 00:00:00", timeFormat)))
        result2 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID1).result)
        result3 <- dtbase.run(updateSchedulingBySchedulingDate(schedulingUUID1, stringToDateFormat("2030-01-01 00:00:00", timeFormat)))
        result4 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID1).result)
      } yield {
        result1 mustBe 1
        result2.head mustBe SchedulingRow(schedulingUUID1, taskUUID1, Some(stringToDateFormat("2035-01-01 00:00:00", timeFormat)), None, None, None, None, None, None)
        result3 mustBe 1
        result4.head mustBe SchedulingRow(schedulingUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)), None, None, None, None, None, None)
      }
    }
  }

  "SchedulingMappings#updateSchedulingByDay" should {
    "update a SchedulingRow by giving the corresponding day." in {

      for {
        result1 <- dtbase.run(updateSchedulingByDay(schedulingUUID2, 10))
        result2 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID2).result)
        result3 <- dtbase.run(updateSchedulingByDay(schedulingUUID2, 15))
        result4 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID2).result)
      } yield {
        result1 mustBe 1
        result2.head mustBe SchedulingRow(schedulingUUID2, taskUUID2, None, Some(10), Some(3), None, Some(5), None, None)
        result3 mustBe 1
        result4.head mustBe SchedulingRow(schedulingUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5), None, None)
      }
    }
  }

  "SchedulingMappings#updateSchedulingByDayOfWeek" should {
    "update a SchedulingRow by giving the corresponding dayOfWeek." in {

      for {
        result1 <- dtbase.run(updateSchedulingByDayOfWeek(schedulingUUID2, 5))
        result2 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID2).result)
        result3 <- dtbase.run(updateSchedulingByDayOfWeek(schedulingUUID2, 3))
        result4 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID2).result)
      } yield {
        result1 mustBe 1
        result2.head mustBe SchedulingRow(schedulingUUID2, taskUUID2, None, Some(15), Some(5), None, Some(5), None, None)
        result3 mustBe 1
        result4.head mustBe SchedulingRow(schedulingUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5), None, None)
      }
    }
  }

  "SchedulingMappings#updateSchedulingByDayType" should {
    "update a SchedulingRow by giving the corresponding dayType." in {

      for {
        result1 <- dtbase.run(updateSchedulingByDayType(schedulingUUID3, DayType.Weekend))
        result2 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID3).result)
        result3 <- dtbase.run(updateSchedulingByDayType(schedulingUUID3, DayType.Weekday))
        result4 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID3).result)
      } yield {
        result1 mustBe 1
        result2.head mustBe SchedulingRow(schedulingUUID3, taskUUID3, None, None, None, Some(DayType.Weekend), None, Some(2030), Some(Criteria.First))
        result3 mustBe 1
        result4.head mustBe SchedulingRow(schedulingUUID3, taskUUID3, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.First))
      }
    }
  }

  "SchedulingMappings#updateSchedulingByMonth" should {
    "update a SchedulingRow by giving the corresponding month." in {

      for {
        result1 <- dtbase.run(updateSchedulingByMonth(schedulingUUID2, 2))
        result2 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID2).result)
        result3 <- dtbase.run(updateSchedulingByMonth(schedulingUUID2, 5))
        result4 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID2).result)
      } yield {
        result1 mustBe 1
        result2.head mustBe SchedulingRow(schedulingUUID2, taskUUID2, None, Some(15), Some(3), None, Some(2), None, None)
        result3 mustBe 1
        result4.head mustBe SchedulingRow(schedulingUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5), None, None)
      }
    }
  }

  "SchedulingMappings#updateSchedulingByYear" should {
    "update a SchedulingRow by giving the corresponding year." in {

      for {
        result1 <- dtbase.run(updateSchedulingByYear(schedulingUUID3, 2035))
        result2 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID3).result)
        result3 <- dtbase.run(updateSchedulingByYear(schedulingUUID3, 2030))
        result4 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID3).result)
      } yield {
        result1 mustBe 1
        result2.head mustBe SchedulingRow(schedulingUUID3, taskUUID3, None, None, None, Some(DayType.Weekday), None, Some(2035), Some(Criteria.First))
        result3 mustBe 1
        result4.head mustBe SchedulingRow(schedulingUUID3, taskUUID3, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.First))
      }
    }
  }

  "SchedulingMappings#updateSchedulingByCriteria" should {
    "update a SchedulingRow by giving the corresponding criteria." in {

      for {
        result1 <- dtbase.run(updateSchedulingByCriteria(schedulingUUID3, Criteria.Second))
        result2 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID3).result)
        result3 <- dtbase.run(updateSchedulingByCriteria(schedulingUUID3, Criteria.First))
        result4 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID3).result)
      } yield {
        result1 mustBe 1
        result2.head mustBe SchedulingRow(schedulingUUID3, taskUUID3, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.Second))
        result3 mustBe 1
        result4.head mustBe SchedulingRow(schedulingUUID3, taskUUID3, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.First))
      }
    }
  }

  "SchedulingMappings#deleteSchedulingBySchedulingId" should {
    "delete a SchedulingRow by giving the corresponding schedulingId." in {

      for {
        result1 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID1).result)
        result2 <- dtbase.run(deleteSchedulingBySchedulingId(schedulingUUID1))
        result3 <- dtbase.run(getSchedulingBySchedulingId(taskUUID1).result)
        result4 <- dtbase.run(insertScheduling(SchedulingRow(schedulingUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)))))
      } yield {
        result1.nonEmpty mustBe true
        result2 mustBe 1
        result3.isEmpty mustBe true
        result4 mustBe 1
      }
    }
  }

  "SchedulingMappings#deleteSchedulingByTaskId" should {
    "delete a SchedulingRow by giving the corresponding taskId." in {

      for {
        result1 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID1).result)
        result2 <- dtbase.run(deleteSchedulingByTaskId(taskUUID1))
        result3 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID1).result)
        result4 <- dtbase.run(insertScheduling(SchedulingRow(schedulingUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)))))
      } yield {
        result1.nonEmpty mustBe true
        result2 mustBe 1
        result3.isEmpty mustBe true
        result4 mustBe 1
      }
    }
  }

  "SchedulingMappings#deleteSchedulingBySchedulingDate" should {
    "delete a SchedulingRow by giving the corresponding schedulingDate." in {

      for {
        result1 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID1).result)
        result2 <- dtbase.run(deleteSchedulingBySchedulingDate(stringToDateFormat("2030-01-01 00:00:00", timeFormat)))
        result3 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID1).result)
        result4 <- dtbase.run(insertScheduling(SchedulingRow(schedulingUUID1, taskUUID1, Some(stringToDateFormat("2030-01-01 00:00:00", timeFormat)))))
      } yield {
        result1.nonEmpty mustBe true
        result2 mustBe 1
        result3.isEmpty mustBe true
        result4 mustBe 1
      }
    }
  }

  "SchedulingMappings#deleteSchedulingByDay" should {
    "delete a SchedulingRow by giving the corresponding day." in {

      for {
        result1 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID2).result)
        result2 <- dtbase.run(deleteSchedulingByDay(15))
        result3 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID2).result)
        result4 <- dtbase.run(insertScheduling(SchedulingRow(schedulingUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5))))
      } yield {
        result1.nonEmpty mustBe true
        result2 mustBe 1
        result3.isEmpty mustBe true
        result4 mustBe 1
      }
    }
  }

  "SchedulingMappings#deleteSchedulingByDayOfWeek" should {
    "delete a SchedulingRow by giving the corresponding dayOfWeek." in {

      for {
        result1 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID2).result)
        result2 <- dtbase.run(deleteSchedulingByDayOfWeek(3))
        result3 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID2).result)
        result4 <- dtbase.run(insertScheduling(SchedulingRow(schedulingUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5))))
      } yield {
        result1.nonEmpty mustBe true
        result2 mustBe 1
        result3.isEmpty mustBe true
        result4 mustBe 1
      }
    }
  }

  "SchedulingMappings#deleteSchedulingByDayType" should {
    "delete a SchedulingRow by giving the corresponding dayType." in {

      for {
        result1 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID3).result)
        result2 <- dtbase.run(deleteSchedulingByDayType(DayType.Weekday))
        result3 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID3).result)
        result4 <- dtbase.run(insertScheduling(SchedulingRow(schedulingUUID3, taskUUID3, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.First))))
      } yield {
        result1.nonEmpty mustBe true
        result2 mustBe 1
        result3.isEmpty mustBe true
        result4 mustBe 1
      }
    }
  }

  "SchedulingMappings#deleteSchedulingByMonth" should {
    "delete a SchedulingRow by giving the corresponding month." in {

      for {
        result1 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID2).result)
        result2 <- dtbase.run(deleteSchedulingByMonth(5))
        result3 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID2).result)
        result4 <- dtbase.run(insertScheduling(SchedulingRow(schedulingUUID2, taskUUID2, None, Some(15), Some(3), None, Some(5))))
      } yield {
        result1.nonEmpty mustBe true
        result2 mustBe 1
        result3.isEmpty mustBe true
        result4 mustBe 1
      }
    }
  }

  "SchedulingMappings#deleteSchedulingByYear" should {
    "delete a SchedulingRow by giving the corresponding year." in {

      for {
        result1 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID3).result)
        result2 <- dtbase.run(deleteSchedulingByYear(2030))
        result3 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID3).result)
        result4 <- dtbase.run(insertScheduling(SchedulingRow(schedulingUUID3, taskUUID3, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.First))))
      } yield {
        result1.nonEmpty mustBe true
        result2 mustBe 1
        result3.isEmpty mustBe true
        result4 mustBe 1
      }
    }
  }

  "SchedulingMappings#deleteSchedulingByCriteria" should {
    "delete a SchedulingRow by giving the corresponding criteria." in {

      for {
        result1 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID3).result)
        result2 <- dtbase.run(deleteSchedulingByCriteria(Criteria.First))
        result3 <- dtbase.run(getSchedulingBySchedulingId(schedulingUUID3).result)
        result4 <- dtbase.run(insertScheduling(SchedulingRow(schedulingUUID3, taskUUID3, None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.First))))
      } yield {
        result1.nonEmpty mustBe true
        result2 mustBe 1
        result3.isEmpty mustBe true
        result4 mustBe 1
      }
    }
  }

}