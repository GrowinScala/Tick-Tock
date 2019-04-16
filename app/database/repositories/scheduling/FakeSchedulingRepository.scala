package database.repositories.scheduling

import api.dtos.{ ExclusionDTO, SchedulingDTO }
import api.services.{ Criteria, DayType }
import api.utils.DateUtils._

import scala.concurrent.Future

class FakeSchedulingRepository extends SchedulingRepository {

  /**
   * Selects all rows from the exclusions table on the database.
   *
   * @return all exclusions in the database
   */
  def selectAllSchedulings: Future[Seq[SchedulingDTO]] = {
    Future.successful(Seq(
      SchedulingDTO("dsa1", "asd1", Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss"))),
      SchedulingDTO("dsa2", "asd2", None, Some(15), None, Some(DayType.Weekday), None, Some(2030)),
      SchedulingDTO("dsa3", "asd3", None, None, Some(3), None, Some(5), None, Some(Criteria.Third))))
  }

  /**
   * Selects an exclusion from the database given an id
   *
   * @return An exclusionDTO containing the selected exclusion
   */
  def selectScheduling(id: String): Future[Option[SchedulingDTO]] = {
    Future.successful(Some(SchedulingDTO("dsa1", "asd1", Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))))
  }

  def selectSchedulingsByTaskId(id: String): Future[Option[Seq[SchedulingDTO]]] = {
    Future.successful(Some(Seq(SchedulingDTO("dsa1", "asd1", Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss"))))))
  }

  /**
   * Deletes all schedulings from the schedulings table on the database.
   */
  def deleteAllSchedulings: Future[Int] = {
    Future.successful(3)
  }

  /**
   * Deletes a scheduling from the database given an id.
   * @param id - identifier of the scheduling to be deleted.
   * @return An Int representing the number of rows deleted.
   */
  def deleteSchedulingById(id: String): Future[Int] = {
    Future.successful(1)
  }

  /**
   * Method that inserts an exclusion (row) on the exclusions table on the database.
   *
   * @param file ExclusionDTO to be inserted on the database.
   */
  def insertInSchedulingsTable(file: SchedulingDTO): Future[Boolean] = {
    Future.successful(true)
  }
}
