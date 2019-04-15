package executionengine

class FakeExecutionManager extends ExecutionManager {

  /**
   * Method that is called when a scheduled task is fired and a certain file should be executed.
   * It always returns 0 signaling that the task execution occurred without issues.
   * @param file target file that will be executed
   * @return
   */
  def runFile(file: String): Int = 0
}
