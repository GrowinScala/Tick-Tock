package executionengine

import javax.inject.Singleton

import scala.sys.process._

/**
 * Object that handles file executions and file delaying asynchronously.
 */
@Singleton
class ExecutionManagerImpl extends ExecutionManager {

  //path to the file storage
  val storagePath = "app/filestorage/"

  /**
   * Method that is called when a scheduled task is fired and a certain file should be executed.
   * It creates a separate process for that file to run by running a console command.
   * @param file target file that will be executed
   * @return
   */
  def runFile(file: String): Int = synchronized {
    val pathSeq = Seq("java", "-jar", storagePath + file + ".jar")
    pathSeq.!
  }
}
