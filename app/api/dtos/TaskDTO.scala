package api.dtos

import java.util.Date

case class TaskDTO(
                    startDateAndTime: Date,
                    taskName: String,
                    taskPath: String
                  )
