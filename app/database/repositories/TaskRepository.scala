package database.repositories

import api.dtos.TaskDTO
import database.mappings.TaskMappings
import database.mappings.TaskMappings.TaskRow
import slick.jdbc.MySQLProfile.api._
import database.mappings.TaskMappings._
import database.repositories.FileRepository._

import scala.concurrent.{ExecutionContext, Future}


object TaskRepository extends BaseRepository{

  def selectAllTasks: Future[Seq[TaskRow]] = {
    exec(selectAllFromTasksTable.result)
  }

  def selectTaskById(id:Int): Future[Seq[TaskRow]] = {
    exec(selectByTaskId(id).result)
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

  def insertTasksTableAction(task: TaskDTO)(implicit ec: ExecutionContext): Unit = {
    if(existsCorrespondingFileName(task.taskName)) {
      selectFileIdFromName(task.taskName).map(id => exec(insertTask(TaskRow(0, id, task.startDateAndTime))))
    }
    else println("Could not insert Task with name " + task.taskName + "due to not finding a corresponding File.")
  }
}
