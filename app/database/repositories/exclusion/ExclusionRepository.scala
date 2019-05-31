package database.repositories.exclusion

import api.dtos.ExclusionDTO
import database.mappings.ExclusionMappings.ExclusionRow

import scala.concurrent.Future

trait ExclusionRepository {

  def exclusionRowToExclusionDTO(exclusion: ExclusionRow): ExclusionDTO

  def exclusionDTOToExclusionRow(exclusion: ExclusionDTO): ExclusionRow

  /**
   * Selects all rows from the exclusions table on the database.
   *
   * @return all exclusions in the database.
   */
  def selectAllExclusions: Future[Seq[ExclusionDTO]]

  /**
   * Select a single exclusion from the database by giving its id.
   *
   * @param id - the identifier of the exclusions.
   * @return a ExclusionDTO of the selected exclusion.
   */
  def selectExclusion(id: String): Future[Option[ExclusionDTO]]

  def selectExclusionsByTaskId(id: String): Future[Option[Seq[ExclusionDTO]]]

  /**
   * Deletes all exclusion from the exclusions table on the database.
   */
  def deleteAllExclusions: Future[Int]

  /**
   * Given a an id deletes the corresponding exclusion.
   *
   * @param id - identifier of the exclusion to be deleted.
   */
  def deleteExclusionById(id: String): Future[Int]

  /**
   * Inserts a exclusion (row) on the exclusions table on the database.
   *
   * @param exclusion ExclusionDTO to be inserted.
   */
  def insertInExclusionsTable(exclusion: ExclusionDTO): Future[Boolean]

}
