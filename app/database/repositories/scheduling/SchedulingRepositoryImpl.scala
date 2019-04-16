package database.repositories.scheduling

import api.dtos.{ SchedulingDTO, TaskDTO }
import database.mappings.SchedulingMappings._
import javax.inject.Inject
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ ExecutionContext, Future }

class SchedulingRepositoryImpl @Inject() (dtbase: Database) extends SchedulingRepository {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  private def schedulingDTOToSchedulingRow(scheduling: SchedulingDTO): SchedulingRow = {
    SchedulingRow(scheduling.schedulingId, scheduling.taskId, scheduling.schedulingDate, scheduling.day, scheduling.dayOfWeek, scheduling.dayType, scheduling.month, scheduling.year, scheduling.criteria)
  }

  private def schedulingRowToSchedulingDTO(schedulingRow: SchedulingRow): SchedulingDTO = {
    SchedulingDTO(schedulingRow.schedulingId, schedulingRow.taskId, schedulingRow.schedulingDate, schedulingRow.day, schedulingRow.dayOfWeek, schedulingRow.dayType, schedulingRow.month, schedulingRow.year, schedulingRow.criteria)
  }

  def selectAllSchedulings: Future[Seq[SchedulingDTO]] = {
    dtbase.run(selectAllFromSchedulingsTable.result).map { seq =>
      seq.map(elem => schedulingRowToSchedulingDTO(elem))
    }
  }

  def selectScheduling(id: String): Future[Option[SchedulingDTO]] = {
    dtbase.run(getSchedulingBySchedulingId(id).result).map { seq =>
      if (seq.isEmpty) None
      else Some(schedulingRowToSchedulingDTO(seq.head))
    }
  }

  def selectSchedulingsByTaskId(id: String): Future[Option[Seq[SchedulingDTO]]] = {
    dtbase.run(getSchedulingByTaskId(id).result).map { seq =>
      if (seq.isEmpty) None
      else Some(seq.map(elem => schedulingRowToSchedulingDTO(elem)))
    }
  }

  def deleteAllSchedulings: Future[Int] = {
    dtbase.run(deleteAllFromSchedulingsTable)
  }

  def deleteSchedulingById(id: String): Future[Int] = {
    dtbase.run(deleteSchedulingBySchedulingId(id))
  }

  def insertInSchedulingsTable(scheduling: SchedulingDTO): Future[Boolean] = {
    dtbase.run(insertScheduling(schedulingDTOToSchedulingRow(scheduling))).map(i => i == 1)
  }
}
