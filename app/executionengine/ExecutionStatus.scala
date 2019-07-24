package executionengine

object ExecutionStatus extends Enumeration {

  type ExecutionStatus = String

  lazy val Idle = "Idle"
  lazy val RunOnceWaiting = "RunOnceWaiting"
  lazy val PeriodicWaiting = "PeriodicWaiting"
  lazy val PersonalizedWaiting = "PersonalizedWaiting"
  lazy val RunOnceRunning = "RunOnceRunning"
  lazy val PeriodicRunning = "PeriodicRunning"
  lazy val PersonalizedRunning = "PersonalizedRunning"
  lazy val Canceled = "Canceled"

  val executionStatusList: List[ExecutionStatus] = List(Idle, RunOnceWaiting, PeriodicWaiting, PersonalizedWaiting, RunOnceRunning, PeriodicRunning, PersonalizedRunning, Canceled)
}

