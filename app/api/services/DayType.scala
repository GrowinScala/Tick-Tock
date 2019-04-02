package api.services

object DayType extends Enumeration {

  type DayType = String

  lazy val Weekday: DayType = "Weekday"
  lazy val Weekend: DayType = "Weekend"
}
