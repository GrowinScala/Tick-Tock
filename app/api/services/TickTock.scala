package api.services

import api.services.TaskService._
import api.validators.Validator.dateFormatsList
import database.mappings.FileMappings._
import database.mappings.TaskMappings
import database.mappings.TaskMappings._
import database.repositories.FileRepository._
import database.repositories.TaskRepository._

object TickTock{

  def retrieveDataFromDB = {
    println("retrieving data from DB")
    selectAllTasks.foreach(t => scheduleOnce(selectNameFromFileId(t.fileId), t.startDateAndTime))
  }

  def main(args: Array[String]): Unit = {

    /*val flyway: Flyway = Flyway.configure().dataSource(
      "jdbc:mysql://127.0.0.1:3306/ticktock?serverTimezone=Portugal",
      "root",
      "growin"
    ).load()

    flyway.baseline()
    flyway.migrate()*/

    retrieveDataFromDB

  }

}