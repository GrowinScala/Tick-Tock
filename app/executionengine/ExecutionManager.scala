package executionengine

import akka.actor.Cancellable

import scala.sys.process._

/**
  * Object that handles file executions and file delaying asynchronously.
  */
object ExecutionManager{

  //path to the file storage
  val storagePath = "app/filestorage/"

  /**
    * Method that is called when a scheduled task is fired and a certain file should be executed.
    * It creates a separate process for that file to run by running a console command.
    * @param file
    * @return
    */
  def runFile(file: String): Int = synchronized {
    val pathSeq = Seq("java", "-jar", storagePath + file)
    pathSeq.!
  }

  /**
    * Method that is called when a scheduled task is fired to delay it by calling another ExecutionJob.
    * @param job ExecutionJob received to be re-launched after the delaying.
    */
  def delayFile(job: ExecutionJob): Unit = synchronized {
    job.start
  }
}
