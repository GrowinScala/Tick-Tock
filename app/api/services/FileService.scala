package api.services

import java.sql.Timestamp
import java.util.Calendar

/**
  * Object that contains all methods for the file uploading/manipulating related to the service layer.
  */
object FileService {

  /**
    * Auxiliary method that returns the current date in the Timestamp format.
    */
  def getCurrentDateTimestamp: Timestamp = {
    val now = Calendar.getInstance().getTime
    new Timestamp(now.getTime)
  }
}
