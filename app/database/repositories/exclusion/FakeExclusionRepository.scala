package database.repositories.exclusion

import api.dtos.ExclusionDTO
import api.services.{ Criteria, DayType }
import api.utils.DateUtils._

import scala.concurrent.Future

class FakeExclusionRepository extends ExclusionRepository {

  /**
   * Selects all rows from the exclusions table on the database.
   *
   * @return all exclusions in the database
   */
  def selectAllExclusions: Future[Seq[ExclusionDTO]] = {
    Future.successful(Seq(
      ExclusionDTO("dsa1", "asd1", Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss"))),
      ExclusionDTO("dsa2", "asd2", None, Some(15), None, Some(DayType.Weekday), None, Some(2030)),
      ExclusionDTO("dsa3", "asd3", None, None, Some(3), None, Some(5), None, Some(Criteria.Third))))
  }

  /**
   * Selects an exclusion from the database given an id
   *
   * @return An exclusionDTO containing the selected exclusion
   */
  def selectExclusionById(id: String): Future[Option[ExclusionDTO]] = {
    Future.successful(Some(ExclusionDTO("dsa1", "asd1", Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))))
  }

  /**
   * Deletes all exclusions from the exclusions table on the database.
   */
  def deleteAllExclusions: Future[Int] = {
    Future.successful(3)
  }

  /**
   * Method that inserts an exclusion (row) on the exclusions table on the database.
   *
   * @param file ExclusionDTO to be inserted on the database.
   */
  def insertInExclusionsTable(file: ExclusionDTO): Future[Boolean] = {
    Future.successful(true)
  }

}
