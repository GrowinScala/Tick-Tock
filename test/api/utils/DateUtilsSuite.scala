package api.utils

import java.util.{ Calendar, Date }

import api.services.DayType
import api.utils.DateUtils._
import org.scalatest.{ AsyncWordSpec, MustMatchers }
import org.scalatestplus.play.PlaySpec

class DateUtilsSuite extends AsyncWordSpec with MustMatchers {

  private val calendar = Calendar.getInstance()

  "DateUtils#stringToDateFormat" should {
    "convert a string in a certain format into a Date object. (yyyy-MM-dd HH:mm:ss format)" in {
      calendar.set(2019, 1 - 1, 1, 12, 0, 0)
      val date = calendar.getTime
      stringToDateFormat("2019-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss").toString mustBe date.toString
    }

    "convert a string in a certain format into a Date object. (dd-MM-yyyy HH:mm:ss format)" in {
      calendar.set(2019, 2 - 1, 1, 14, 30, 0)
      val date = calendar.getTime
      stringToDateFormat("01-02-2019 14:30:00", "dd-MM-yyyy HH:mm:ss").toString mustBe date.toString
    }

    "convert a string in a certain format into a Date object. (yyyy/MM/dd HH:mm:ss format)" in {
      calendar.set(2020, 6 - 1, 15, 16, 15, 45)
      val date = calendar.getTime
      stringToDateFormat("2020/06/15 16:15:45", "yyyy/MM/dd HH:mm:ss").toString mustBe date.toString
    }

    "convert a string in a certain format into a Date object. (dd/MM/yyyy HH:mm:ss format)" in {
      calendar.set(2021, 12 - 1, 25, 20, 30, 40)
      val date = calendar.getTime
      stringToDateFormat("25/12/2021 20:30:40", "dd/MM/yyyy HH:mm:ss").toString mustBe date.toString
    }
  }

  "DateUtils#dateToDayOfWeekInt" should {

    "convert a date to its corresponding day of the week in integer form. (Monday => 1)" in {
      calendar.set(2019, 3 - 1, 3, 0, 0, 0)
      val date = dateToLocalDate(calendar.getTime)
      dateToDayOfWeekInt(date) mustBe 1
    }

    "convert a date to its corresponding day of the week in integer form. (Tuesday => 2)" in {
      calendar.set(2019, 3 - 1, 4, 0, 0, 0)
      val date = dateToLocalDate(calendar.getTime)
      dateToDayOfWeekInt(date) mustBe 2
    }

    "convert a date to its corresponding day of the week in integer form. (Wednesday => 3)" in {
      calendar.set(2019, 3 - 1, 5, 0, 0, 0)
      val date = dateToLocalDate(calendar.getTime)
      dateToDayOfWeekInt(date) mustBe 3
    }

    "convert a date to its corresponding day of the week in integer form. (Thursday => 4)" in {
      calendar.set(2019, 3 - 1, 6, 0, 0, 0)
      val date = dateToLocalDate(calendar.getTime)
      dateToDayOfWeekInt(date) mustBe 4
    }

    "convert a date to its corresponding day of the week in integer form. (Friday => 5)" in {
      calendar.set(2019, 3 - 1, 7, 0, 0, 0)
      val date = dateToLocalDate(calendar.getTime)
      dateToDayOfWeekInt(date) mustBe 5
    }

    "convert a date to its corresponding day of the week in integer form. (Saturday => 6)" in {
      calendar.set(2019, 3 - 1, 8, 0, 0, 0)
      val date = dateToLocalDate(calendar.getTime)
      dateToDayOfWeekInt(date) mustBe 6
    }

    "convert a date to its corresponding day of the week in integer form. (Sunday => 7)" in {
      calendar.set(2019, 3 - 1, 9, 0, 0, 0)
      val date = dateToLocalDate(calendar.getTime)
      dateToDayOfWeekInt(date) mustBe 7
    }

  }

  "DateUtils#dateToDayTypeString" should {

    "convert a date to its corresponding day type. (Monday => Weekday)" in {
      calendar.set(2019, 3 - 1, 4, 0, 0, 0)
      val date = dateToLocalDate(calendar.getTime)
      dateToDayTypeString(date) mustBe DayType.Weekday
    }

    "convert a date to its corresponding day type. (Tuesday => Weekday)" in {
      calendar.set(2019, 3 - 1, 5, 0, 0, 0)
      val date = dateToLocalDate(calendar.getTime)
      dateToDayTypeString(date) mustBe DayType.Weekday
    }

    "convert a date to its corresponding day type. (Wednesday => Weekday)" in {
      calendar.set(2019, 3 - 1, 6, 0, 0, 0)
      val date = dateToLocalDate(calendar.getTime)
      dateToDayTypeString(date) mustBe DayType.Weekday
    }

    "convert a date to its corresponding day type. (Thursday => Weekday)" in {
      calendar.set(2019, 3 - 1, 7, 0, 0, 0)
      val date = dateToLocalDate(calendar.getTime)
      dateToDayTypeString(date) mustBe DayType.Weekday
    }

    "convert a date to its corresponding day type. (Friday => Weekday)" in {
      calendar.set(2019, 3 - 1, 8, 0, 0, 0)
      val date = dateToLocalDate(calendar.getTime)
      dateToDayTypeString(date) mustBe DayType.Weekday
    }

    "convert a date to its corresponding day type. (Saturday => Weekend)" in {
      calendar.set(2019, 3 - 1, 9, 0, 0, 0)
      val date = dateToLocalDate(calendar.getTime)
      dateToDayTypeString(date) mustBe DayType.Weekend
    }

    "convert a date to its corresponding day type. (Sunday => Weekend)" in {
      calendar.set(2019, 3 - 1, 10, 0, 0, 0)
      val date = dateToLocalDate(calendar.getTime)
      dateToDayTypeString(date) mustBe DayType.Weekend
    }

  }

  "DateUtils#removeTimeFromDate" should {
    "remove the time from a date." in {
      calendar.set(2019, 1 - 1, 1, 16, 0, 0)

      val date = calendar.getTime
      calendar.set(2019, 1 - 1, 1, 0, 0, 0)
      removeTimeFromDate(date).toString mustBe calendar.getTime.toString

    }
  }

  "DateUtils#isLeapYear" should {
    "return false when given None." in (isLeapYear(None) mustBe false)
    "return false when given a non leap year." in (isLeapYear(Some(2019)) mustBe false)
    "return true when given a leap year." in (isLeapYear(Some(2020)) mustBe true)
  }

  "DateUtils#getDateWithAddedSeconds" should {
    "return a date with added seconds." in {
      val date = getDateWithAddedSeconds(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"), 5)
      date mustBe stringToDateFormat("2030-01-01 00:00:05", "yyyy-MM-dd HH:mm:ss")
    }
  }

  "DateUtils#getDateWithSubtractedSeconds" should {
    "return a date with subtracted seconds." in {
      val date = getDateWithSubtractedSeconds(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"), 5)
      date mustBe stringToDateFormat("2029-12-31 23:59:55", "yyyy-MM-dd HH:mm:ss")
    }
  }

  "DateUtils#dateToStringFormat" should {
    "convert a date to its string format in a certain format. (yyyy-MM-dd HH:mm:ss format)" in {
      calendar.set(2019, 1 - 1, 1, 12, 0, 0)
      val date = calendar.getTime
      dateToStringFormat(date, "yyyy-MM-dd HH:mm:ss") mustBe "2019-01-01 12:00:00"
    }

    "convert a date to its string format in a certain format. (dd-MM-yyyy HH:mm:ss format)" in {
      calendar.set(2019, 2 - 1, 1, 14, 30, 0)
      val date = calendar.getTime
      dateToStringFormat(date, "dd-MM-yyyy HH:mm:ss") mustBe "01-02-2019 14:30:00"
    }

    "convert a date to its string format in a certain format. (yyyy/MM/dd HH:mm:ss format)" in {
      calendar.set(2020, 6 - 1, 15, 16, 15, 45)
      val date = calendar.getTime
      dateToStringFormat(date, "yyyy/MM/dd HH:mm:ss") mustBe "2020/06/15 16:15:45"
    }

    "convert a date to its string format in a certain format. (dd/MM/yyyy HH:mm:ss format)" in {
      calendar.set(2021, 12 - 1, 25, 20, 30, 40)
      val date = calendar.getTime
      dateToStringFormat(date, "dd/MM/yyyy HH:mm:ss") mustBe "25/12/2021 20:30:40"
    }
  }

  "DateUtils#parseDate" should {
    "try to parse a correct date in string form and return it in Date form. (yyyy-MM-dd HH:mm:ss format)" in {
      calendar.set(2019, 1 - 1, 1, 12, 0, 0)
      val date = calendar.getTime
      parseDate("2019-01-01 12:00:00").get.toString mustBe date.toString
    }

    "try to parse a correct date in string form and return it in Date form. (dd-MM-yyyy HH:mm:ss format)" in {
      calendar.set(2019, 2 - 1, 1, 14, 30, 0)
      val date = calendar.getTime
      parseDate("01-02-2019 14:30:00").get.toString mustBe date.toString
    }

    "try to parse a correct date in string form and return it in Date form. (yyyy/MM/dd HH:mm:ss format)" in {
      calendar.set(2020, 6 - 1, 15, 16, 15, 45)
      val date = calendar.getTime
      parseDate("2020/06/15 16:15:45").get.toString mustBe date.toString
    }

    "try to parse a correct date in string form and return it in Date form. (dd/MM/yyyy HH:mm:ss format)" in {
      calendar.set(2021, 12 - 1, 25, 20, 30, 40)
      val date = calendar.getTime
      parseDate("25/12/2021 20:30:40").get.toString mustBe date.toString
    }

    "try to parse an incorrect date in string form and return None." in {
      parseDate("123-456/789 123456789") mustBe None
    }
  }

  "DateUtils#parseTimezone" should {
    "try to parse a valid string into a TimeZone object and return it." in {
      val PST = parseTimezone("PST")
      PST.isDefined mustBe true
      PST.get.getID mustBe "PST"
      val CST = parseTimezone("CST")
      CST.isDefined mustBe true
      CST.get.getID mustBe "CST"
    }

    "try to parse an invalid string into a TimeZone object and return None." in {
      val ADT = parseTimezone("ADT")
      ADT.isDefined mustBe false
      val BDT = parseTimezone("BDT")
      BDT.isDefined mustBe false
    }
  }

  "DateUtils#parseDateWithTimezone" should {
    "try to parse a valid date and an already validated timezone and return it in Date form. (yyyy-MM-dd HH:mm:ss format)" in {
      val date = parseDateWithTimezone("2019-01-01 12:00:00", "PST") // 8 hour difference with GMT

      calendar.set(2019, 1 - 1, 1, 20, 0, 0)

      date.isDefined mustBe true
      date.get.toString mustBe calendar.getTime.toString
    }

    "try to parse a valid date and an already validated timezone and return it in Date form. (dd-MM-yyyy HH:mm:ss format)" in {
      val date = parseDateWithTimezone("01-01-2019 12:00:00", "PST") // 8 hour difference with GMT
      calendar.set(2019, 1 - 1, 1, 20, 0, 0)

      date.isDefined mustBe true
      date.get.toString mustBe calendar.getTime.toString
    }

    "try to parse a valid date and an already validated timezone and return it in Date form. (yyyy/MM/dd HH:mm:ss format)" in {
      val date = parseDateWithTimezone("2019/01/01 12:00:00", "PST") // 8 hour difference with GMT

      calendar.set(2019, 1 - 1, 1, 20, 0, 0)

      date.isDefined mustBe true
      date.get.toString mustBe calendar.getTime.toString
    }

    "try to parse a valid date and an already validated timezone and return it in Date form. (dd/MM/yyyy HH:mm:ss format)" in {
      val date = parseDateWithTimezone("01/01/2019 12:00:00", "PST") // 8 hour difference with GMT

      calendar.set(2019, 1 - 1, 1, 20, 0, 0)

      date.isDefined mustBe true
      date.get.toString mustBe calendar.getTime.toString
    }

    "try to parse an invalid date and an already validated timezone and return None." in {
      val date = parseDateWithTimezone("2019:01:01 12/00/00", "PST") // invalid date format given
      date.isDefined mustBe false
    }

  }

  "DateUtils#isPossibleDateWithoutYear" should {
    "return true give a possible date. (any day before a 29th in any month)" in {
      isPossibleDateWithoutYear(1, 1) mustBe true
      isPossibleDateWithoutYear(5, 3) mustBe true
      isPossibleDateWithoutYear(10, 4) mustBe true
      isPossibleDateWithoutYear(12, 5) mustBe true
      isPossibleDateWithoutYear(17, 7) mustBe true
      isPossibleDateWithoutYear(21, 9) mustBe true
      isPossibleDateWithoutYear(25, 11) mustBe true
      isPossibleDateWithoutYear(27, 12) mustBe true
    }

    "return true given a possible date. (29th of any month except February)" in {
      isPossibleDateWithoutYear(29, 3) mustBe true
      isPossibleDateWithoutYear(29, 6) mustBe true
      isPossibleDateWithoutYear(29, 9) mustBe true
      isPossibleDateWithoutYear(29, 12) mustBe true
    }

    "return true given a possible date. (30th of any month except February)" in {
      isPossibleDateWithoutYear(30, 1) mustBe true
      isPossibleDateWithoutYear(30, 4) mustBe true
      isPossibleDateWithoutYear(30, 7) mustBe true
      isPossibleDateWithoutYear(30, 10) mustBe true
    }

    "return true given a possible date. (31st of any month except February/April/June/September/November)" in {
      isPossibleDateWithoutYear(31, 1) mustBe true
      isPossibleDateWithoutYear(31, 3) mustBe true
      isPossibleDateWithoutYear(31, 5) mustBe true
      isPossibleDateWithoutYear(31, 7) mustBe true
      isPossibleDateWithoutYear(31, 8) mustBe true
      isPossibleDateWithoutYear(31, 10) mustBe true
      isPossibleDateWithoutYear(31, 12) mustBe true
    }

    "return false given an impossible date. (any day value after 31)" in {
      isPossibleDateWithoutYear(32, 5) mustBe false
      isPossibleDateWithoutYear(35, 8) mustBe false
      isPossibleDateWithoutYear(40, 11) mustBe false
    }

    "return false given an impossible date. (any day value that is zero or negative)" in {
      isPossibleDateWithoutYear(0, 4) mustBe false
      isPossibleDateWithoutYear(-5, 9) mustBe false
      isPossibleDateWithoutYear(-15, 12) mustBe false
    }

    "return false given an impossible date. (any month value after 12)" in {
      isPossibleDateWithoutYear(5, 13) mustBe false
      isPossibleDateWithoutYear(12, 15) mustBe false
      isPossibleDateWithoutYear(26, 20) mustBe false
    }

    "return false given an impossible date. (any month value that is zero or negative)" in {
      isPossibleDateWithoutYear(1, 0) mustBe false
      isPossibleDateWithoutYear(11, -6) mustBe false
      isPossibleDateWithoutYear(22, -12) mustBe false
    }

    "return false given an impossible date. (29th of February)" in {
      isPossibleDateWithoutYear(29, 2) mustBe false
    }

    "return false given an impossible date. (30th of February)" in {
      isPossibleDateWithoutYear(30, 2) mustBe false
    }

    "return false given an impossible date. (31st of February)" in {
      isPossibleDateWithoutYear(31, 2) mustBe false
    }

    "return false given an impossible date. (31st of April)" in {
      isPossibleDateWithoutYear(31, 4) mustBe false
    }

    "return false given an impossible date. (31st of June)" in {
      isPossibleDateWithoutYear(31, 6) mustBe false
    }

    "return false given an impossible date. (31st of September)" in {
      isPossibleDateWithoutYear(31, 9) mustBe false
    }

    "return false given an impossible date. (31st of November)" in {
      isPossibleDateWithoutYear(31, 11) mustBe false
    }

  }

  "DateUtils#isPossibleDateWithoutMonth" should {
    "return true given a possible date. (any day value between 1 and 31)" in {
      isPossibleDateWithoutMonth(5, 2030) mustBe true
      isPossibleDateWithoutMonth(15, 2035) mustBe true
      isPossibleDateWithoutMonth(25, 2040) mustBe true
    }

    "return false given an impossible date. (any day value before 1 or after 31)" in {
      isPossibleDateWithoutMonth(-5, 2030) mustBe false
      isPossibleDateWithoutMonth(0, 2032) mustBe false
      isPossibleDateWithoutMonth(32, 2034) mustBe false
      isPossibleDateWithoutMonth(50, 2036) mustBe false
    }
  }

}
