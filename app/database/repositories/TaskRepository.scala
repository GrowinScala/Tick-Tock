package database.repositories

import api.dtos.TaskDTO
import database.mappings.TaskMappings.TaskRow
import slick.driver.MySQLDriver.api._
import slick.dbio.DBIO
import database.mappings.TaskMappings._
import database.repositories.FileRepository._

import scala.concurrent.duration._
import scala.concurrent._


object TaskRepository extends Repository{

  def selectAllTasks: Seq[TaskRow] = {
    exec(selectAllFromTasksTable.result)
  }

  def deleteAllTasks: Unit = {
    exec(deleteAllFromTasksTable)
  }

  def createTasksTable: Unit = {
    exec(createTasksTableAction)
  }

  def dropTasksTable: Unit = {
    exec(dropTasksTableAction)
  }

  def insertTasksTableAction(task: TaskRow): Unit = {
    if(existsCorrespondingFileId(task.fileId)) exec(insertTask(task))
    else println("Could not insert Task with id " + task.fileId + " due to not finding a corresponding File.")
  }
}
