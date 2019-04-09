package api.services

object PeriodType extends Enumeration {

  type PeriodType = String

  lazy val Minutely = "Minutely"
  lazy val Hourly = "Hourly"
  lazy val Daily = "Daily"
  lazy val Weekly = "Weekly"
  lazy val Monthly = "Monthly"
  lazy val Yearly = "Yearly"

  val periodTypeList: List[PeriodType] = List(Minutely, Hourly, Daily, Weekly, Monthly, Yearly)
}

