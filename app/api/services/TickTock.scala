package api.services

import api.services.TaskService._
import database.mappings.FileMappings.FileRow
import database.repositories.FileRepository._
import database.repositories.TaskRepository._

object TickTock{

//  def retrieveDataFromDB = {
//    println("retrieving data from DB")
//    selectAllTasks.foreach(t => scheduleOnce(selectNameFromFileId(t.fileId), t.startDateAndTime))
//  }

  def main(args: Array[String]): Unit = {

//    createFilesTable
//    createTasksTable

    insertFilesTableAction(FileRow(0, "EmailSender","EmailSender", getCurrentDateTimestamp))

//    retrieveDataFromDB
  }

}