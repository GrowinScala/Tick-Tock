import java.text.SimpleDateFormat

import api.services.SchedulingType
import executionengine.ExecutionJob
import org.scalatest.FunSuite
import org.scalatestplus.play.PlaySpec

class TaskServiceSuite extends PlaySpec{

  "TaskService#scheduleOnce" should {
    "schedule a task for a specific file without specifying a date." in {
      new ExecutionJob("EmailSender", SchedulingType.RunOnce)
    }
    "schedule a task for a specific file specifying a date." in {
      val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
      val date = format.parse("1970-01-01 00:00:00")
      new ExecutionJob("EmailSender", SchedulingType.RunOnce, Some(date))
    }
  }



  /*test("TaskService: scheduleOnce with no date."){

  }

  test("TaskService: scheduleOnce with date."){

  }*/

}
