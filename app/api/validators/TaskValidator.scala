package api.validators

import java.nio.file.{FileSystems, Files}
import java.text.{DateFormat, SimpleDateFormat}
import java.util.{Calendar, Date, TimeZone, UUID}

import api.dtos._
import api.services.Criteria.Criteria
import api.services.DayType.DayType
import api.services.{Criteria, DayType}
import api.services.PeriodType.PeriodType
import api.services.SchedulingType.SchedulingType
import api.utils.DateUtils._
import api.utils.UUIDGenerator
import api.validators.Error._
import database.repositories.{FileRepository, FileRepositoryImpl, TaskRepository}
import database.utils.DatabaseUtils.DEFAULT_DB
import javax.inject.{Inject, Singleton}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import scala.util.Try

/**
  * Object that handles the validation for the received JSON's on the HTTP request controller classes.
  */
@Singleton
class TaskValidator @Inject() (implicit val fileRepo: FileRepository, implicit val taskRepo: TaskRepository, implicit val UUIDGen: UUIDGenerator) {

  implicit val ec = ExecutionContext.global
  val calendar = Calendar.getInstance

  //---------------------------------------------------------
  //# TASK VALIDATORS
  //---------------------------------------------------------

  def scheduleValidator(task: CreateTaskDTO): Either[List[Error], TaskDTO] = {
    val taskId = UUIDGen.generateUUID
    val startDate = isValidStartDateFormat(task.startDateAndTime, task.timezone)
    val endDate = isValidEndDateFormat(task.endDateAndTime, task.timezone)
    val exclusions = isValidExclusionFormat(task.exclusions, taskId)
    val schedulings = isValidSchedulingFormat(task.schedulings, taskId)
    val errorList: List[(Boolean, Error)] = (List(
      (isValidCreateTask(task), invalidCreateTaskFormat),
      (task.startDateAndTime.isEmpty || startDate.isDefined, invalidStartDateFormat),
      (isValidStartDateValue(startDate), invalidStartDateValue),
      (isValidFileName(Some(task.fileName)), invalidFileName),
      (isValidTaskType(Some(task.taskType)), invalidTaskType),
      (isValidPeriodType(task.periodType), invalidPeriodType),
      (isValidPeriod(task.period), invalidPeriod),
      (task.endDateAndTime.isEmpty || endDate.isDefined, invalidEndDateFormat),
      (isValidEndDateValue(startDate, endDate), invalidEndDateValue),
      (isValidOccurrences(task.occurrences), invalidOccurrences),
      (task.timezone.isEmpty || isValidTimezone(task.timezone), invalidTimezone),
      (task.schedulings.isEmpty || schedulings.isDefined, invalidSchedulingFormat),
      (task.exclusions.isEmpty || exclusions.isDefined, invalidExclusionFormat))
      ::: areValidExclusionValues(exclusions, startDate)
      ::: areValidSchedulingValues(schedulings, startDate)
      ).filter(!_._1)
    if (errorList.isEmpty) Right(TaskDTO(taskId, task.fileName, task.taskType, startDate, task.periodType, task.period, endDate, task.occurrences, task.occurrences, task.timezone, exclusions, schedulings))
    else Left(errorList.unzip._2)
  }

  def updateValidator(id: String, task: UpdateTaskDTO): Either[List[Error], TaskDTO] = {
    val oldDTO = Await.result(taskRepo.selectTaskByTaskId(id), 5 seconds)
    if (oldDTO.isDefined) {
      val startDate = isValidStartDateFormat(task.startDateAndTime, task.timezone)
      val endDate = isValidEndDateFormat(task.endDateAndTime, task.timezone)
      val exclusions = isValidUpdateExclusionFormat(oldDTO.get.exclusions, task.exclusions, id)
      val schedulings = isValidUpdateSchedulingFormat(oldDTO.get.schedulings, task.schedulings, id)
      val errorList: List[(Boolean, Error)] = (List(
        (isValidUpdateTask(task, oldDTO.get), invalidUpdateTaskFormat),
        (isValidUUID(task.taskId), invalidTaskUUID),
        (task.startDateAndTime.isEmpty || startDate.isDefined, invalidStartDateFormat),
        (isValidStartDateValue(startDate), invalidStartDateValue),
        (isValidFileName(task.fileName), invalidFileName),
        (isValidTaskType(task.taskType) || task.taskType.isEmpty, invalidTaskType),
        (isValidPeriodType(task.periodType), invalidPeriodType),
        (isValidPeriod(task.period), invalidPeriod),
        (task.endDateAndTime.isEmpty || endDate.isDefined, invalidEndDateFormat),
        (isValidOccurrences(task.occurrences), invalidOccurrences),
        (task.timezone.isEmpty || isValidTimezone(task.timezone), invalidTimezone),
        (task.schedulings.isEmpty || schedulings.isDefined, invalidSchedulingFormat),
        (task.exclusions.isEmpty || exclusions.isDefined, invalidExclusionFormat))
        ::: areValidExclusionValues(exclusions, startDate)
        ::: areValidSchedulingValues(schedulings, startDate)
        ).filter(!_._1)

      val oldStartDate = if (oldDTO.get.startDateAndTime.isDefined) oldDTO.get.startDateAndTime else None
      val oldPeriodType = if (oldDTO.get.periodType.isDefined) oldDTO.get.periodType else None
      val oldPeriod = if (oldDTO.get.period.isDefined) oldDTO.get.period else None
      val oldEndDate = if (oldDTO.get.endDateAndTime.isDefined) oldDTO.get.endDateAndTime else None
      val oldTotalOccurrences = if (oldDTO.get.totalOccurrences.isDefined) oldDTO.get.totalOccurrences else None
      val oldCurrentOccurrences = if (oldDTO.get.currentOccurrences.isDefined) oldDTO.get.currentOccurrences else None
      val oldTimezone = if(oldDTO.get.timezone.isDefined) oldDTO.get.timezone else None
      val oldExclusions = if(oldDTO.get.exclusions.isDefined) oldDTO.get.exclusions else None
      val oldSchedulings = if(oldDTO.get.schedulings.isDefined) oldDTO.get.schedulings else None

      if (errorList.isEmpty) {
        Right(TaskDTO(
          task.taskId.getOrElse(oldDTO.get.taskId), //taskId
          task.fileName.getOrElse(oldDTO.get.fileName), //fileName
          task.taskType.getOrElse(oldDTO.get.taskType), //taskType
          if (startDate.isDefined) startDate else oldStartDate, //startDate
          if (task.periodType.isDefined) task.periodType else oldPeriodType, //periodType
          if (task.period.isDefined) task.period else oldPeriod, //period
          if (task.occurrences.isDefined && oldEndDate.isDefined) None else if (endDate.isDefined) endDate else oldEndDate, //endDate
          if (endDate.isDefined && oldTotalOccurrences.isDefined) None else if (task.occurrences.isDefined) task.occurrences else oldTotalOccurrences, //totalOccurrences
          if (endDate.isDefined && oldCurrentOccurrences.isDefined) None else if (task.occurrences.isDefined) task.occurrences else oldCurrentOccurrences, //currentOccurrences
          if (task.timezone.isDefined) task.timezone else oldTimezone, //timezone
          if (task.exclusions.isDefined) exclusions else oldExclusions, //exclusions
          if (task.schedulings.isDefined) schedulings else oldSchedulings //schedulings
        ))
      }
      else Left(errorList.unzip._2)
    }
    else Left(List(invalidEndpointId))
  }

  private def isValidCreateTask(task: CreateTaskDTO): Boolean = {
    task.taskType match {
      case "RunOnce" =>
        task.periodType.isEmpty ||
          task.period.isEmpty ||
          task.endDateAndTime.isEmpty ||
          task.occurrences.isEmpty
      case "Periodic" =>
        task.periodType.isDefined && task.period.isDefined &&
          ((task.endDateAndTime.isDefined && task.occurrences.isEmpty) || (task.endDateAndTime.isEmpty && task.occurrences.isDefined))
      case _ => false
    }
  }

  private def isValidUpdateTask(task: UpdateTaskDTO, oldTask: TaskDTO): Boolean = {
    if(task.taskType.isDefined && task.taskType.get.equals("Periodic")){
      (task.startDateAndTime.isDefined || oldTask.startDateAndTime.isDefined) &&
        (task.periodType.isDefined || oldTask.periodType.isDefined) &&
        (task.period.isDefined || oldTask.period.isDefined) &&
        (task.endDateAndTime.isDefined || oldTask.endDateAndTime.isDefined || task.occurrences.isDefined || oldTask.totalOccurrences.isDefined)
    }
    else{
      (task.taskId.isDefined || task.fileName.isDefined || task.taskType.isDefined || task.startDateAndTime.isDefined ||
        task.periodType.isDefined || task.period.isDefined || task.endDateAndTime.isDefined || task.occurrences.isDefined) &&
        !(task.endDateAndTime.isDefined && task.occurrences.isDefined)
    }

  }

  private def isValidUUID(uuid: Option[String]): Boolean = {
    if (uuid.isDefined){
      val parsedUUID = Try(Some(UUID.fromString(uuid.get))).getOrElse(None)
      parsedUUID.isDefined
    }
    else true
  }

  private def isValidStartDateFormat(startDate: Option[String], timezone: Option[String]): Option[Date] = {
    if (startDate.isDefined){
      if(timezone.isDefined && isValidTimezone(timezone)) parseDateWithTimezone(startDate.get, timezone.get)
      else parseDate(startDate.get)
    }
    else None
  }

  /**
    * Checks if the date given is valid, (if it already happened or not)
    *
    * @param date The Date to be checked
    * @return Returns a ValidationError if its not valid. None otherwise.
    */
  private def isValidStartDateValue(startDate: Option[Date]): Boolean = {
    if (startDate.isDefined) startDate.get.after(getCurrentDate)
    else true
  }

  /**
    * Checks if the file with the given fileName exists.
    *
    * @param fileName The fileName to be checked.
    * @return Returns a ValidationError if its not valid. None otherwise.
    */
  private def isValidFileName(fileName: Option[String]): Boolean = {
    if (fileName.isDefined) Await.result(fileRepo.existsCorrespondingFileName(fileName.get), Duration.Inf)
    else true
  }

  private def isValidTaskType(taskType: Option[String]): Boolean = {
    if (taskType.isDefined) taskType.get.equals("RunOnce") || taskType.get.equals("Periodic")
    else false
  }

  private def isValidPeriodType(periodType: Option[String]): Boolean = {
    periodType.isEmpty ||
      periodType.get.equals("Minutely") ||
      periodType.get.equals("Hourly") ||
      periodType.get.equals("Daily") ||
      periodType.get.equals("Weekly") ||
      periodType.get.equals("Monthly") ||
      periodType.get.equals("Yearly")
  }

  private def isValidPeriod(period: Option[Int]): Boolean = {
    period.isEmpty || period.get > 0
  }

  private def isValidEndDateFormat(endDate: Option[String], timezone: Option[String]): Option[Date] = {
    if (endDate.isDefined){
      if(timezone.isDefined && isValidTimezone(timezone)) parseDateWithTimezone(endDate.get, timezone.get)
      else parseDate(endDate.get)
    }
    else None
  }

  private def isValidEndDateValue(startDate: Option[Date], endDate: Option[Date]): Boolean = {
    if (startDate.isDefined && endDate.isDefined) endDate.isEmpty || (endDate.get.after(startDate.get) && endDate.get.after(getCurrentDate))
    else true
  }

  private def isValidOccurrences(occurrences: Option[Int]): Boolean = {
    occurrences.isEmpty || occurrences.get > 0
  }

  private def isValidTimezone(timezone: Option[String]): Boolean = {
    if(timezone.isDefined) parseTimezone(timezone.get).isDefined
    else false
  }

  private def isValidSchedulingFormat(schedulings: Option[List[CreateSchedulingDTO]], taskId: String): Option[List[SchedulingDTO]] = {
    val toReturn: List[SchedulingDTO] = Nil
    if (schedulings.isDefined) {
      schedulings.get.foreach { scheduling =>
        if (scheduling.schedulingDate.isDefined) {
          val parsedDate = parseDate(scheduling.schedulingDate.get)
          if (parsedDate.isDefined && scheduling.day.isEmpty && scheduling.dayOfWeek.isEmpty &&
            scheduling.dayType.isEmpty && scheduling.month.isEmpty && scheduling.year.isEmpty && scheduling.criteria.isEmpty)
            SchedulingDTO(UUIDGen.generateUUID, taskId, parsedDate, scheduling.day, scheduling.dayOfWeek, scheduling.dayType, scheduling.month, scheduling.year, scheduling.criteria) :: toReturn

        }
        else {
          if ((scheduling.day.isDefined || scheduling.dayOfWeek.isDefined || scheduling.dayType.isDefined ||
            scheduling.month.isDefined || scheduling.year.isDefined) &&
            (scheduling.criteria.isEmpty || scheduling.criteria.get == Criteria.First || scheduling.criteria.get == Criteria.Second ||
              scheduling.criteria.get == Criteria.Third || scheduling.criteria.get == Criteria.Fourth || scheduling.criteria.get == Criteria.Last))
            SchedulingDTO(UUIDGen.generateUUID, taskId, None, scheduling.day, scheduling.dayOfWeek, scheduling.dayType, scheduling.month, scheduling.year, scheduling.criteria) :: toReturn
        }
      }
      if (toReturn.size == schedulings.get.size) Some(toReturn)
      else None
    }
    else None
  }

  private def isValidUpdateSchedulingFormat(oldSchedulings: Option[List[SchedulingDTO]], schedulings: Option[List[UpdateSchedulingDTO]], taskId: String): Option[List[SchedulingDTO]] = {
    val toReturn: Option[List[SchedulingDTO]] = None
    if(schedulings.isDefined) {
      schedulings.get.foreach{ scheduling =>
        val oldScheduling = getOldSchedulingWithSchedulingId(scheduling.schedulingId, oldSchedulings)
        if(oldScheduling.isDefined) {
          if (scheduling.schedulingDate.isDefined) {
            val parsedDate = parseDate(scheduling.schedulingDate.get)
            if (parsedDate.isDefined && scheduling.day.isEmpty && scheduling.dayOfWeek.isEmpty &&
              scheduling.dayType.isEmpty && scheduling.month.isEmpty && scheduling.year.isEmpty && scheduling.criteria.isEmpty) {
              SchedulingDTO(scheduling.schedulingId.get, taskId, parsedDate) :: toReturn.get
            }
          }
          else {
            if (scheduling.taskId.isDefined || scheduling.schedulingDate.isDefined || scheduling.day.isDefined
              || scheduling.dayOfWeek.isDefined || scheduling.dayType.isDefined || scheduling.month.isDefined || scheduling.year.isDefined
              || scheduling.criteria.isDefined) {
              SchedulingDTO(scheduling.schedulingId.get, taskId, None, scheduling.day, scheduling.dayOfWeek, scheduling.dayType, scheduling.month, scheduling.year, scheduling.criteria) :: toReturn.get

            }
          }
        }
        else None
      }
      if (toReturn.size == schedulings.get.size) toReturn
      else None
    }
    else None
  }

  private def getOldSchedulingWithSchedulingId(schedulingId: Option[String], oldSchedulings: Option[List[SchedulingDTO]]): Option[SchedulingDTO] = {
    def iter(oldSchedulings: Option[List[SchedulingDTO]]): Option[SchedulingDTO] = {
      if((oldSchedulings.isEmpty && oldSchedulings.get.isEmpty) || schedulingId.isEmpty) None
      else if(oldSchedulings.get.head.schedulingId.equals(schedulingId.get)) Some(oldSchedulings.get.head) else iter(Some(oldSchedulings.get.tail))
    }
    iter(oldSchedulings)
  }

  private def areValidSchedulingValues(schedulings: Option[List[SchedulingDTO]], startDate: Option[Date]): List[(Boolean, Error)] = {
    if (schedulings.isDefined){
      if (startDate.isDefined) calendar.setTime(startDate.get) else calendar.setTime(new Date())
      List(
        (areValidSchedulingDateValues(schedulings.get), invalidSchedulingDateValue),
        (areValidSchedulingDayValues(schedulings.get), invalidSchedulingDayValue),
        (areValidSchedulingDayOfWeekValues(schedulings.get), invalidSchedulingDayOfWeekValue),
        (areValidSchedulingDayTypeValues(schedulings.get), invalidSchedulingDayTypeValue),
        (areValidSchedulingMonthValues(schedulings.get), invalidSchedulingMonthValue),
        (areValidSchedulingYearValues(schedulings.get), invalidSchedulingYearValue),
        (areValidSchedulingCriteriaValues(schedulings.get), invalidSchedulingCriteriaValue)
      )
    }
    else Nil
  }

  private def areValidSchedulingDateValues(schedulings: List[SchedulingDTO]): Boolean = {
    def iter(list: List[SchedulingDTO]): Boolean = {
      if(list.nonEmpty) {
        val scheduling = list.head
        if(scheduling.schedulingDate.isDefined) if(scheduling.schedulingDate.get.after(calendar.getTime)) iter(list.tail) else false
        else iter(list.tail)
      }
      else true
    }
    iter(schedulings)
  }

  private def areValidSchedulingDayValues(schedulings: List[SchedulingDTO]): Boolean = {
    def iter(list: List[SchedulingDTO]): Boolean = {
      if(list.nonEmpty){
        val scheduling = list.head
        if(scheduling.day.isDefined) {
          if(scheduling.day.get >= 1 && scheduling.day.get <= 31) {
            if (scheduling.month.isDefined) {
              scheduling.day.get match {
                case 29 => {
                  if (scheduling.month.get == 2 && scheduling.year.isDefined && !isLeapYear(scheduling.year.get)) false
                }
                case 30 => if (scheduling.month.get == 2) false
                case 31 => if (scheduling.month.get == 2 || scheduling.month.get == 4 || scheduling.month.get == 6 ||
                  scheduling.month.get == 9 || scheduling.month.get == 11) false
              }
              if (scheduling.year.isDefined) {
                if (scheduling.month.get == calendar.get(Calendar.MONTH) && scheduling.year.get == calendar.get(Calendar.YEAR)) if(scheduling.day.get >= calendar.get(Calendar.DAY_OF_MONTH)) iter(list.tail) else false
                else if(scheduling.year.get >= calendar.get(Calendar.YEAR) && scheduling.month.get >= calendar.get(Calendar.MONTH)) iter(list.tail) else false
              }
              else iter(list.tail)
            }
            else iter(list.tail)
          }
          else false
        }
        else iter(list.tail)
      }
      else true

    }
    iter(schedulings)
  }

  private def areValidSchedulingDayOfWeekValues(schedulings: List[SchedulingDTO]): Boolean = {
    def iter(list: List[SchedulingDTO]): Boolean = {
      if (list.nonEmpty) {
        val scheduling = list.head
        if (scheduling.dayOfWeek.isDefined) {
          if (scheduling.dayOfWeek.get >= 1 && scheduling.dayOfWeek.get <= 7) {
            if (scheduling.dayType.isDefined) {
              if (scheduling.dayOfWeek.get >= 2 && scheduling.dayOfWeek.get <= 6) if(scheduling.dayType.get == DayType.Weekday) iter(list.tail) else false
              else if(scheduling.dayType.get == DayType.Weekend) iter(list.tail) else false
            }
            else iter(list.tail)
          }
          else false
        }
        else iter(list.tail)
      }
      else true
    }
    iter(schedulings)
  }

  private def areValidSchedulingDayTypeValues(schedulings: List[SchedulingDTO]): Boolean = {
    def iter(list: List[SchedulingDTO]): Boolean = {
      if (list.nonEmpty) {
        val scheduling = list.head
        if (scheduling.dayType.isDefined) {
          if (scheduling.dayType.get == DayType.Weekday) if (scheduling.dayOfWeek.get >= 2 && scheduling.dayOfWeek.get <= 6) iter(list.tail) else false
          else if (scheduling.dayType.get == DayType.Weekend) if (scheduling.dayOfWeek.get == 1 || scheduling.dayOfWeek.get == 7) iter(list.tail) else false
          else false
        }
        else iter(list.tail)
      }
      else true
    }
    iter(schedulings)
  }

  private def areValidSchedulingMonthValues(schedulings: List[SchedulingDTO]): Boolean = {
    def iter(list: List[SchedulingDTO]): Boolean = {
      if (list.nonEmpty) {
        val scheduling = list.head
        if (scheduling.month.isDefined) {
          if (scheduling.month.get >= 1 && scheduling.month.get <= 12) {
            if (scheduling.year.isDefined) {
              if (scheduling.year.get == calendar.get(Calendar.YEAR)) if(scheduling.month.get >= calendar.get(Calendar.MONTH)) iter(list.tail) else false
              else if(scheduling.year.get >= calendar.get(Calendar.YEAR)) iter(list.tail) else false
            }
            else iter(list.tail)
          }
          else false
        }
        else iter(list.tail)
      }
      else true
    }
    iter(schedulings)
  }

  private def areValidSchedulingYearValues(schedulings: List[SchedulingDTO]): Boolean = {
    def iter(list: List[SchedulingDTO]): Boolean = {
      if (list.nonEmpty) {
        val scheduling = list.head
        if (scheduling.year.isDefined) if(scheduling.year.get >= calendar.get(Calendar.YEAR)) iter(list.tail) else false
        else iter(list.tail)
      }
      else true
    }
    iter(schedulings)
  }

  private def areValidSchedulingCriteriaValues(schedulings: List[SchedulingDTO]): Boolean = {
    def iter(list: List[SchedulingDTO]): Boolean = {
      if (list.nonEmpty) {
        val scheduling = list.head
        if (scheduling.criteria.isDefined) if(scheduling.criteria.get == Criteria.First || scheduling.criteria.get == Criteria.Second || scheduling.criteria.get == Criteria.Third ||
          scheduling.criteria.get == Criteria.Fourth || scheduling.criteria.get == Criteria.Last) iter(list.tail) else false
        else iter(list.tail)
      }
      else true
    }
    iter(schedulings)
  }

  private def isValidExclusionFormat(exclusions: Option[List[CreateExclusionDTO]], taskId: String): Option[List[ExclusionDTO]] = {
    val toReturn: List[ExclusionDTO] = Nil
    if (exclusions.isDefined) {
      exclusions.get.foreach { exclusion =>
        if (exclusion.exclusionDate.isDefined) {
          val parsedDate = parseDate(exclusion.exclusionDate.get)
          if (parsedDate.isDefined && exclusion.day.isEmpty && exclusion.dayOfWeek.isEmpty &&
            exclusion.dayType.isEmpty && exclusion.month.isEmpty && exclusion.year.isEmpty && exclusion.criteria.isEmpty)
            ExclusionDTO(UUIDGen.generateUUID, taskId, parsedDate, exclusion.day, exclusion.dayOfWeek, exclusion.dayType, exclusion.month, exclusion.year, exclusion.criteria) :: toReturn

        }
        else {
          if ((exclusion.day.isDefined || exclusion.dayOfWeek.isDefined || exclusion.dayType.isDefined ||
            exclusion.month.isDefined || exclusion.year.isDefined) &&
            (exclusion.criteria.isEmpty || exclusion.criteria.get == Criteria.First || exclusion.criteria.get == Criteria.Second ||
              exclusion.criteria.get == Criteria.Third || exclusion.criteria.get == Criteria.Fourth || exclusion.criteria.get == Criteria.Last))
            ExclusionDTO(UUIDGen.generateUUID, taskId, None, exclusion.day, exclusion.dayOfWeek, exclusion.dayType, exclusion.month, exclusion.year, exclusion.criteria) :: toReturn
        }
      }
      if (toReturn.size == exclusions.get.size) Some(toReturn)
      else None
    }
    else None
  }

  private def isValidUpdateExclusionFormat(oldExclusions: Option[List[ExclusionDTO]], exclusions: Option[List[UpdateExclusionDTO]], taskId: String): Option[List[ExclusionDTO]] = {
    val toReturn: Option[List[ExclusionDTO]] = None
    if(exclusions.isDefined) {
      exclusions.get.foreach{ exclusion =>
        val oldExclusion = getOldExclusionWithExclusionId(exclusion.exclusionId, oldExclusions)
        if(oldExclusion.isDefined) {
          if (exclusion.exclusionDate.isDefined) {
            val parsedDate = parseDate(exclusion.exclusionDate.get)
            if (parsedDate.isDefined && exclusion.day.isEmpty && exclusion.dayOfWeek.isEmpty &&
              exclusion.dayType.isEmpty && exclusion.month.isEmpty && exclusion.year.isEmpty && exclusion.criteria.isEmpty) {
              ExclusionDTO(exclusion.exclusionId.get, taskId, parsedDate) :: toReturn.get
            }
          }
          else {
            if (exclusion.taskId.isDefined || exclusion.exclusionDate.isDefined || exclusion.day.isDefined
              || exclusion.dayOfWeek.isDefined || exclusion.dayType.isDefined || exclusion.month.isDefined || exclusion.year.isDefined
              || exclusion.criteria.isDefined) {
              ExclusionDTO(exclusion.exclusionId.get, taskId, None, exclusion.day, exclusion.dayOfWeek, exclusion.dayType, exclusion.month, exclusion.year, exclusion.criteria) :: toReturn.get

            }
          }
        }
        else None
      }
      if (toReturn.size == exclusions.get.size) toReturn
      else None
    }
    else None
  }

  private def getOldExclusionWithExclusionId(exclusionId: Option[String], oldExclusions: Option[List[ExclusionDTO]]): Option[ExclusionDTO] = {
    def iter(oldExclusions: Option[List[ExclusionDTO]]): Option[ExclusionDTO] = {
      if((oldExclusions.isEmpty && oldExclusions.get.isEmpty) || exclusionId.isEmpty) None
      else if(oldExclusions.get.head.exclusionId.equals(exclusionId.get)) Some(oldExclusions.get.head) else iter(Some(oldExclusions.get.tail))
    }
    iter(oldExclusions)
  }

  private def areValidExclusionValues(exclusions: Option[List[ExclusionDTO]], startDate: Option[Date]): List[(Boolean, Error)] = {
    if (exclusions.isDefined){
      if (startDate.isDefined) calendar.setTime(startDate.get) else calendar.setTime(new Date())
        List(
        (areValidExclusionDateValues(exclusions.get), invalidExclusionDateValue),
        (areValidExclusionDayValues(exclusions.get), invalidExclusionDayValue),
        (areValidExclusionDayOfWeekValues(exclusions.get), invalidExclusionDayOfWeekValue),
        (areValidExclusionDayTypeValues(exclusions.get), invalidExclusionDayTypeValue),
        (areValidExclusionMonthValues(exclusions.get), invalidExclusionMonthValue),
        (areValidExclusionYearValues(exclusions.get), invalidExclusionYearValue),
        (areValidExclusionCriteriaValues(exclusions.get), invalidExclusionCriteriaValue)
        )
    }
    else Nil

  }

  private def areValidExclusionDateValues(exclusions: List[ExclusionDTO]): Boolean = {
    def iter(list: List[ExclusionDTO]): Boolean = {
      if(list.nonEmpty) {
        val exclusion = list.head
        if(exclusion.exclusionDate.isDefined) if(exclusion.exclusionDate.get.after(calendar.getTime)) iter(list.tail) else false
        else iter(list.tail)
      }
      else true
    }
    iter(exclusions)

  }

  private def areValidExclusionDayValues(exclusions: List[ExclusionDTO]): Boolean = {
    def iter(list: List[ExclusionDTO]): Boolean = {
      if(list.nonEmpty){
        val exclusion = list.head
        if(exclusion.day.isDefined) {
          if(exclusion.day.get >= 1 && exclusion.day.get <= 31) {
            if (exclusion.month.isDefined) {
              exclusion.day.get match {
                case 29 => exclusion.month.get != 2 || exclusion.year.isEmpty || isLeapYear(exclusion.year.get)
                case 30 => exclusion.month.get != 2
                case 31 => exclusion.month.get != 2 && exclusion.month.get != 4 && exclusion.month.get != 6 && exclusion.month.get != 9 && exclusion.month.get != 11
              }
              if (exclusion.year.isDefined) {
                if (exclusion.month.get == calendar.get(Calendar.MONTH) && exclusion.year.get == calendar.get(Calendar.YEAR)) if(exclusion.day.get >= calendar.get(Calendar.DAY_OF_MONTH)) iter(list.tail) else false
                else if(exclusion.year.get >= calendar.get(Calendar.YEAR) && exclusion.month.get >= calendar.get(Calendar.MONTH)) iter(list.tail) else false
              }
              else iter(list.tail)
            }
            else iter(list.tail)
          }
          else false
        }
        else iter(list.tail)
      }
      else true

    }
    iter(exclusions)

  }

  private def areValidExclusionDayOfWeekValues(exclusions: List[ExclusionDTO]): Boolean = {
    def iter(list: List[ExclusionDTO]): Boolean = {
      if (list.nonEmpty) {
        val exclusion = list.head
        if (exclusion.dayOfWeek.isDefined) {
          if (exclusion.dayOfWeek.get >= 1 && exclusion.dayOfWeek.get <= 7) {
            if (exclusion.dayType.isDefined) {
              if (exclusion.dayOfWeek.get >= 2 && exclusion.dayOfWeek.get <= 6) if(exclusion.dayType.get == DayType.Weekday) iter(list.tail) else false
              else if(exclusion.dayType.get == DayType.Weekend) iter(list.tail) else false
            }
            else iter(list.tail)
          }
          else false
        }
        else iter(list.tail)
      }
      else true
    }
    iter(exclusions)
  }

  private def areValidExclusionDayTypeValues(exclusions: List[ExclusionDTO]): Boolean = {
    def iter(list: List[ExclusionDTO]): Boolean = {
      if (list.nonEmpty) {
        val exclusion = list.head
        if (exclusion.dayType.isDefined) {
          if (exclusion.dayType.get == DayType.Weekday) if (exclusion.dayOfWeek.get >= 2 && exclusion.dayOfWeek.get <= 6) iter(list.tail) else false
          else if (exclusion.dayType.get == DayType.Weekend) if (exclusion.dayOfWeek.get == 1 || exclusion.dayOfWeek.get == 7) iter(list.tail) else false
          else false
        }
        else iter(list.tail)
      }
      else true
    }
    iter(exclusions)
  }

  private def areValidExclusionMonthValues(exclusions: List[ExclusionDTO]): Boolean = {
    def iter(list: List[ExclusionDTO]): Boolean = {
      if (list.nonEmpty) {
        val exclusion = list.head
        if (exclusion.month.isDefined) {
          if (exclusion.month.get >= 1 && exclusion.month.get <= 12) {
            if (exclusion.year.isDefined) {
              if (exclusion.year.get == calendar.get(Calendar.YEAR)) if(exclusion.month.get >= calendar.get(Calendar.MONTH)) iter(list.tail) else false
              else if(exclusion.year.get >= calendar.get(Calendar.YEAR)) iter(list.tail) else false
            }
            else iter(list.tail)
          }
          else false
        }
        else iter(list.tail)
      }
      else true
    }
    iter(exclusions)
  }

  private def areValidExclusionYearValues(exclusions: List[ExclusionDTO]): Boolean = {
    def iter(list: List[ExclusionDTO]): Boolean = {
      if (list.nonEmpty) {
        val exclusion = list.head
        if (exclusion.year.isDefined) if(exclusion.year.get >= calendar.get(Calendar.YEAR)) iter(list.tail) else false
        else iter(list.tail)
      }
      else true
    }
    iter(exclusions)
  }

  private def areValidExclusionCriteriaValues(exclusions: List[ExclusionDTO]): Boolean = {
    def iter(list: List[ExclusionDTO]): Boolean = {
      if (list.nonEmpty) {
        val exclusion = list.head
        if (exclusion.criteria.isDefined) if(exclusion.criteria.get == Criteria.First || exclusion.criteria.get == Criteria.Second || exclusion.criteria.get == Criteria.Third ||
            exclusion.criteria.get == Criteria.Fourth || exclusion.criteria.get == Criteria.Last) iter(list.tail) else false
        else iter(list.tail)
      }
      else true
    }
    iter(exclusions)
  }

 }
