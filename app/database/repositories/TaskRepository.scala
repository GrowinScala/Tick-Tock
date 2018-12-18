package database.repositories

import api.dtos.TaskDTO

trait TaskRepository {

  def selectAllTasks: Seq[TaskDTO]

  /**
    * Deletes all tasks from the tasks table on the database.
    */
  def deleteAllTasks: Unit

  /**
    * Creates the tasks table on the database.
    */
  def createTasksTable: Unit

  /**
    * Drops the tasks table on the database.
    */
  def dropTasksTable: Unit

  /**
    * Inserts a task (row) on the tasks table on the database.
    * @param task TaskDTO to be inserted.
    */
  def insertInTasksTable(task: TaskDTO): Unit
}
