package database.repositories.exclusion

import api.dtos.{ ExclusionDTO, TaskDTO }
import database.mappings.ExclusionMappings.ExclusionRow
import javax.inject.Inject
import slick.jdbc.MySQLProfile.api._
import database.mappings.ExclusionMappings._
import database.mappings.SchedulingMappings.deleteAllFromSchedulingsTable

import scala.concurrent.{ ExecutionContext, Future }

class ExclusionRepositoryImpl @Inject() (dtbase: Database) extends ExclusionRepository {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  private def exclusionDTOToExclusionRow(exclusion: ExclusionDTO): ExclusionRow = {
    ExclusionRow(exclusion.exclusionId, exclusion.taskId, exclusion.exclusionDate, exclusion.day, exclusion.dayOfWeek, exclusion.dayType, exclusion.month, exclusion.year, exclusion.criteria)
  }

  private def exclusionRowToExclusionDTO(exclusion: ExclusionRow): ExclusionDTO = {
    ExclusionDTO(exclusion.exclusionId, exclusion.taskId, exclusion.exclusionDate, exclusion.day, exclusion.dayOfWeek, exclusion.dayType, exclusion.month, exclusion.year, exclusion.criteria)
  }

  def selectAllExclusions: Future[Seq[ExclusionDTO]] = {
    dtbase.run(selectAllFromExclusionsTable.result).map { seq =>
      seq.map(elem => exclusionRowToExclusionDTO(elem))
    }
  }

  def selectExclusionById(id: String): Future[Option[ExclusionDTO]] = {
    dtbase.run(getExclusionByExclusionId(id).result).map { seq =>
      if (seq.isEmpty) None
      else Some(exclusionRowToExclusionDTO(seq.head))
    }
  }

  def deleteAllExclusions: Future[Int] = {
    dtbase.run(deleteAllFromExclusionsTable)
  }

  def deleteExclusionById(id: String): Future[Int] = {
    dtbase.run(deleteExclusionByExclusionId(id))
  }

  def insertInExclusionsTable(exclusion: ExclusionDTO): Future[Boolean] = {
    dtbase.run(insertExclusion(exclusionDTOToExclusionRow(exclusion))).map(i => i == 1)
  }

}
