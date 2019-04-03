package api.utils

import java.util.Calendar

import api.services.DayType
import api.utils.DateUtils._
import org.scalatestplus.play.PlaySpec

class DateUtilsSuite extends PlaySpec {

  private val calendar = Calendar.getInstance()

  "DateUtils#stringToDateFormat" should {
    "convert a string in a certain format into a Date object. (yyyy-MM-dd HH:mm:ss format)" in {
      calendar.set(2019, 1 - 1, 1, 12, 0, 0)
      val date = calendar.getTime
      stringToDateFormat("2019-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss").toString mustBe "Tue Jan 01 12:00:00 GMT 2019"
    }

    "convert a string in a certain format into a Date object. (dd-MM-yyyy HH:mm:ss format)" in {
      calendar.set(2019, 2 - 1, 1, 14, 30, 0)
      val date = calendar.getTime
      stringToDateFormat("01-02-2019 14:30:00", "dd-MM-yyyy HH:mm:ss").toString mustBe "Fri Feb 01 14:30:00 GMT 2019"
    }

    "convert a string in a certain format into a Date object. (yyyy/MM/dd HH:mm:ss format)" in {
      calendar.set(2020, 6 - 1, 15, 16, 15, 45)
      val date = calendar.getTime
      stringToDateFormat("2020/06/15 16:15:45", "yyyy/MM/dd HH:mm:ss").toString mustBe "Mon Jun 15 16:15:45 BST 2020"
    }

    "convert a string in a certain format into a Date object. (dd/MM/yyyy HH:mm:ss format)" in {
      calendar.set(2021, 12 - 1, 25, 20, 30, 40)
      val date = calendar.getTime
      stringToDateFormat("25/12/2021 20:30:40", "dd/MM/yyyy HH:mm:ss").toString mustBe "Sat Dec 25 20:30:40 GMT 2021"
    }
  }

  "DateUtils#dateToDayOfWeekInt" should {

    "covert a date to its corresponding day of the week in integer form. (Monday => 1)" in {
      calendar.set(2019, 3 - 1, 3, 0, 0, 0)
      val date = calendar.getTime
      dateToDayOfWeekInt(date) mustBe 1
    }

    "covert a date to its corresponding day of the week in integer form. (Tuesday => 2)" in {
      calendar.set(2019, 3 - 1, 4, 0, 0, 0)
      val date = calendar.getTime
      dateToDayOfWeekInt(date) mustBe 2
    }

    "covert a date to its corresponding day of the week in integer form. (Wednesday => 3)" in {
      calendar.set(2019, 3 - 1, 5, 0, 0, 0)
      val date = calendar.getTime
      dateToDayOfWeekInt(date) mustBe 3
    }

    "covert a date to its corresponding day of the week in integer form. (Thursday => 4)" in {
      calendar.set(2019, 3 - 1, 6, 0, 0, 0)
      val date = calendar.getTime
      dateToDayOfWeekInt(date) mustBe 4
    }

    "covert a date to its corresponding day of the week in integer form. (Friday => 5)" in {
      calendar.set(2019, 3 - 1, 7, 0, 0, 0)
      val date = calendar.getTime
      dateToDayOfWeekInt(date) mustBe 5
    }

    "covert a date to its corresponding day of the week in integer form. (Saturday => 6)" in {
      calendar.set(2019, 3 - 1, 8, 0, 0, 0)
      val date = calendar.getTime
      dateToDayOfWeekInt(date) mustBe 6
    }

    "covert a date to its corresponding day of the week in integer form. (Sunday => 7)" in {
      calendar.set(2019, 3 - 1, 9, 0, 0, 0)
      val date = calendar.getTime
      dateToDayOfWeekInt(date) mustBe 7
    }

  }

  "DateUtils#dateToDayTypeString" should {

    "convert a date to its corresponding day type. (Monday => Weekday)" in {
      calendar.set(2019, 3 - 1, 3, 0, 0, 0)
      val date = calendar.getTime
      dateToDayTypeString(date) mustBe DayType.Weekday
    }

    "convert a date to its corresponding day type. (Tuesday => Weekday)" in {
      calendar.set(2019, 3 - 1, 4, 0, 0, 0)
      val date = calendar.getTime
      dateToDayTypeString(date) mustBe DayType.Weekday
    }

    "convert a date to its corresponding day type. (Wednesday => Weekday)" in {
      calendar.set(2019, 3 - 1, 5, 0, 0, 0)
      val date = calendar.getTime
      dateToDayTypeString(date) mustBe DayType.Weekday
    }

    "convert a date to its corresponding day type. (Thursday => Weekday)" in {
      calendar.set(2019, 3 - 1, 6, 0, 0, 0)
      val date = calendar.getTime
      dateToDayTypeString(date) mustBe DayType.Weekday
    }

    "convert a date to its corresponding day type. (Friday => Weekday)" in {
      calendar.set(2019, 3 - 1, 7, 0, 0, 0)
      val date = calendar.getTime
      dateToDayTypeString(date) mustBe DayType.Weekday
    }

    "convert a date to its corresponding day type. (Saturday => Weekend)" in {
      calendar.set(2019, 3 - 1, 8, 0, 0, 0)
      val date = calendar.getTime
      dateToDayTypeString(date) mustBe DayType.Weekend
    }

    "convert a date to its corresponding day type. (Sunday => Weekend)" in {
      calendar.set(2019, 3 - 1, 9, 0, 0, 0)
      val date = calendar.getTime
      println(date)
      dateToDayTypeString(date) mustBe DayType.Weekend
    }

  }

  "DateUtils#removeTimeFromDate" should {
    "remove the time from a date." in {
      calendar.set(2019, 1 - 1, 1, 16, 0, 0)
      val date1 = calendar.getTime
      getTimeFromDate(date1).toString mustBe "Tue Jan 01 00:00:00 GMT 2019"
      calendar.set(2020, 1 - 1, 1, 22, 0, 0)
      val date2 = calendar.getTime
      getTimeFromDate(date2).toString mustBe "Wed Jan 01 00:00:00 GMT 2020"
    }
  }

  "DateUtils#isLeapYear" should {
    "return false when given a non leap year." in (isLeapYear(2019) mustBe false)
    "return true when given a leap year" in (isLeapYear(2020) mustBe true)
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
      parseDate("2019-01-01 12:00:00").get.toString mustBe "Tue Jan 01 12:00:00 GMT 2019"
    }

    "try to parse a correct date in string form and return it in Date form. (dd-MM-yyyy HH:mm:ss format)" in {
      parseDate("01-02-2019 14:30:00").get.toString mustBe "Fri Feb 01 14:30:00 GMT 2019"
    }

    "try to parse a correct date in string form and return it in Date form. (yyyy/MM/dd HH:mm:ss format)" in {
      parseDate("2020/06/15 16:15:45").get.toString mustBe "Mon Jun 15 16:15:45 BST 2020"
    }

    "try to parse a correct date in string form and return it in Date form. (dd/MM/yyyy HH:mm:ss format)" in {
      parseDate("25/12/2021 20:30:40").get.toString mustBe "Sat Dec 25 20:30:40 GMT 2021"
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
      date.isDefined mustBe true
      date.get.toString mustBe "Tue Jan 01 20:00:00 GMT 2019"
    }

    "try to parse a valid date and an already validated timezone and return it in Date form. (dd-MM-yyyy HH:mm:ss format)" in {
      val date = parseDateWithTimezone("01-01-2019 12:00:00", "PST") // 8 hour difference with GMT
      date.isDefined mustBe true
      date.get.toString mustBe "Tue Jan 01 20:00:00 GMT 2019"
    }

    "try to parse a valid date and an already validated timezone and return it in Date form. (yyyy/MM/dd HH:mm:ss format)" in {
      val date = parseDateWithTimezone("2019/01/01 12:00:00", "PST") // 8 hour difference with GMT
      date.isDefined mustBe true
      date.get.toString mustBe "Tue Jan 01 20:00:00 GMT 2019"
    }

    "try to parse a valid date and an already validated timezone and return it in Date form. (dd/MM/yyyy HH:mm:ss format)" in {
      val date = parseDateWithTimezone("01/01/2019 12:00:00", "PST") // 8 hour difference with GMT
      date.isDefined mustBe true
      date.get.toString mustBe "Tue Jan 01 20:00:00 GMT 2019"
    }

    "try to parse an invalid date and an already validated timezone and return None." in {
      val date = parseDateWithTimezone("2019:01:01 12/00/00", "PST") // invalid date format given
      date.isDefined mustBe false
    }

  }

}
