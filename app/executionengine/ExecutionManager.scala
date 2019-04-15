package executionengine

trait ExecutionManager {

  /**
   * Method that is called when a scheduled task is fired and a certain file should be executed.
   * @param file target file that will be executed
   * @return
   */
  def runFile(file: String): Int

  /**
   * Method that is called when a scheduled task is fired to delay it by calling another ExecutionJob.
   * @param job ExecutionJob received to be re-launched after the delaying.
   */
  def delayFile(job: ExecutionJob): Unit = synchronized { job.start() }
}
