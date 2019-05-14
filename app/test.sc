import java.util.{Calendar, Date}

import api.dtos.ExclusionDTO
import api.utils.{FakeUUIDGenerator, UUIDGenerator}
import api.utils.DateUtils._
import api.validators.TaskValidator
import database.repositories.file.{FakeFileRepository, FileRepository}
import database.repositories.task.{FakeTaskRepository, TaskRepository}
import java.time.Duration

import scala.util.Try


val date = new Date()
getDateWithSubtractedSeconds(3600)
getDateWithAddedSeconds(3600)