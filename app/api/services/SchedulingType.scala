package api.services

/**
  * Method that enumerates all the different types of scheduling
  *
  * RunOnce: Task to run once on a specified date.
  * Periodic: Task to run periodically after the specified date.
  */
object SchedulingType extends Enumeration {

  type SchedulingType = Value
  val RunOnce, Periodic = Value
}
