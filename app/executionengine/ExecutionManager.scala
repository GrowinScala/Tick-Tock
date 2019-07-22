package executionengine

trait ExecutionManager {

  /**
   * Method that is called when a scheduled task is fired and a certain file should be executed.
   * @param file target file that will be executed
   * @return
   */
  def runFile(file: String): Unit

}
