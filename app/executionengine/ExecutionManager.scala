package executionengine

import scala.sys.process._

object ExecutionManager {

  val storagePath = "app/filestorage/"

  def run(file: String): Int = synchronized {
    //val pathSeq = Seq("java", "-jar", storagePath + file + ".jar")
    val pathSeq = Seq("java", "-jar", storagePath + file + ".jar")
    pathSeq.!
  }
}
