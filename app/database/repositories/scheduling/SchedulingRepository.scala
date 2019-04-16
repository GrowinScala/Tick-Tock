package database.repositories.scheduling

import api.dtos.SchedulingDTO

import scala.concurrent.Future

trait SchedulingRepository {

  /**
   * Selects all rows from the schedulings table on the database.
   *
   * @return all schedulings in the database.
   */
  def selectAllSchedulings: Future[Seq[SchedulingDTO]]

  /**
   * Select a single scheduling from the database by giving its id.
   *
   * @param id - the identifier of the scheduling.
   * @return a SchedulingDTO of the selected scheduling.
   */
  def selectScheduling(id: String): Future[Option[SchedulingDTO]]

  def selectSchedulingsByTaskId(id: String): Future[Option[Seq[SchedulingDTO]]]

  /**
   * Deletes all schedulings from the scheduling table on the database.
   */
  def deleteAllSchedulings: Future[Int]

  /**
   * Given a an id deletes the corresponding scheduling.
   *
   * @param id - identifier of the scheduling to be deleted.
   */
  def deleteSchedulingById(id: String): Future[Int]

  /**
   * Inserts a scheduling (row) on the schedulings table on the database.
   *
   * @param scheduling SchedulingDTO to be inserted.
   */
  def insertInSchedulingsTable(scheduling: SchedulingDTO): Future[Boolean]
}
