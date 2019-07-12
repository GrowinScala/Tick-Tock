# Tick-Tock

Tick-Tock is a scheduling application for executable files in the form of a web app. It receives .jar files and scheduling details and runs the uploaded files at the designated dates and times. It is being developed in [Scala](https://www.scala-lang.org/) with the [Play Framework](http://www.playframework.com).

## Features (in development)

- File execution scheduling for a specific date and time.
- .jar file uploading for later scheduling.
- Periodic scheduling with different settings. (Daily, Weekly, etc.)
- Task scheduling with different criteria instead of specific dates. (e.g. Run daily but only on weekdays)
- Highly personalizable, being able to define exception dates, limit of occurences to perform highly detailed tasks.
- Supports multiple timezones.


## Running/Testing

To start the project, run the command ```sbt run``` in the console and you can then send several different types of HTTP requests to test the different endpoints.
To test these endpoints, you can send a request to the localhost url with a JSON body. I recommend using [Postman](https://www.getpostman.com/) to send HTTP requests with ease. Also keep in mind you need a [MySQL](https://www.mysql.com/) database set up for this to work properly.
Here is a list of all the different available endpoints with the url and the JSON body you need to send:

- GET request at <http://localhost:9000> - returns the message "It works!". Used for basic testing. **[no JSON body required]**
- GET request at <http://localhost:9000/file> - returns info of all the stored files. **[no JSON body required]**
- GET request at <http://localhost:9000/task> - returns info of all the stored tasks. **[no JSON body required]**
- GET request at <http://localhost:9000/file/:id> - returns info of the stored file with the given id in the url. **[no JSON body required]**
- GET request at <http://localhost:9000/task/:id> - returns info of the stored task with the given id in the url. **[no JSON body required]**
- POST request at <http://localhost:9000/file> - used to send info on a new file through a JSON body. **[.jar file must be sent as an attachment]**
- POST request at <http://localhost:9000/task> - used to send info on a new task through a JSON body. **[JSON body is required]**

__Json body fields:__

**fileName** - String corresponding to the name of the file that the task will execute. The fileName must already exist or the field will be invalid. [Field is mandatory]
**taskType** - String corresponding to the type of task. This field can only be "RunOnce" (for single run tasks), "Periodic" (for periodic tasks) or "Personalized" (for tasks with highly personalized executions). Any other String value will be invalid. [Field is mandatory]
**startDateAndTime** - String with a time and date corresponding to the start of the task execution. For "RunOnce" tasks, this marks the time and date of the single execution. For "Periodic"/"Personalized" tasks, it marks the first execution and the start of all the subsequent executions. Not setting a date means the startDate is set to start right away. [Field is optional]
**periodType** - String corresponding to the type of periodicity of the task. This field can only be "Minutely", "Hourly", "Daily", "Weekly", "Monthly", "Yearly" depending on the periodicity. Any other String value will be invalid. [Field is mandatory for "Periodic"/"Personalized" tasks, but must be empty for "RunOnce" ones]
**period** - Integer corresponding to the frequency of the periodicity of the task. (example: Setting period to 2 and periodType to "Minutely", the task will execute every 2 minutes) This integer must be a positive number excluding 0. [Field is mandatory for "Periodic/Personalized" tasks, but must be empty for "RunOnce" ones]
**endDateAndTime** - String with a time and date corresponding to the end of the task execution. This field cannot be set if the "occurrences" field is already set. [Field is mandatory for "Periodic"/"Personalized" tasks if you don't have an occurrences field already set, but must be empty for "RunOnce" ones]
**occurrences** - Integer corresponding to the amount of times the task is executed until it stops. This field cannot be set if the "endDateAndTime" field is already set. [Field is mandatory for "Periodic"/"Personalized" tasks if you don't have an endDateAndTime field already set, but must be empty for "RunOnce" ones]
**timezone** - String corresponding to the timezone you want the dates to be processed at. Refer to this [list](https://garygregory.wordpress.com/2013/06/18/what-are-the-java-timezone-ids/) of all possible timezones. Any String not present in that list will be considered invalid. [Field is optional]
**exclusions** - Array with 1 or several exclusions that define what dates should be excluded from task execution. Below you can see the exclusion fields you can use. [Field is optional]

    **exclusionDate** - String with a date corresponding to the date that should be excluded from execution. [Field is mandatory if all other fields are empty. Field must be empty if any other field is set]
    **day** - Integer between 1 and 31 corresponding to the days with that day value that should be excluded from execution. [Field is optional. Field must be empty if an exclusionDate is already set]
    **dayOfWeek** - Integer between 1 and 7 corresponding to the days of the week in every week that should be excluded from execution. (1 is Sunday and 7 is Saturday) [Field is optional. Field must be empty if an exclusionDate is already set]
    **dayType** - String corresponding to the type of days that should be excluded from execution. This field can only be either "Weekday" or "Weekend". Any other String value will be invalid [Field is optional. Field must be empty if an exclusionDate is already set]
    **month** - Integer between 1 and 12 corresponding to months that should be excluded from execution. [Field is optional. Field must be empty if an exclusionDate is already set]
    **year** - Integer corresponding to the current year or a future year that should be excluded from execution. [Field is optional. Field must be empty if an exclusionDate is already set]
    **criteria** - String corresponding to a criteria. This criteria value can only be "First", "Second", "Third" or "Fourth" and defines a single date to be excluded from execution given all the other fields previously given. (example: If the day value is set to "20" and criteria set to "Second", the only excluded date will be the second day 20 since the startDate of the task) [Field is optional. Field must be empty if any exclusionDate is already set or if no field is set]

**schedulings** - Array with 1 or several schedulings that define what dates should be included in task execution. This is used for "Personalized" tasks to heavily customize your task schedule. Below you can see the scheduling fields you can use. [Field is mandatory for "Personalized" tasks, but must be empty for "RunOnce"/"Periodic" ones]

    **schedulingDate** - String with a date corresponding to the date that should be scheduled for execution. [Field is mandatory if all other fields are empty. Field must be empty if any other field is set]
    **day** - Integer between 1 and 31 corresponding to the days with that day value that should be scheduled for execution. [Field is optional. Field must be empty if an schedulingDate is already set]
    **dayOfWeek** - Integer between 1 and 7 corresponding to the days of the week in every week that should be scheduled for execution. (1 is Sunday and 7 is Saturday) [Field is optional. Field must be empty if an schedulingDate is already set]
    **dayType** - String corresponding to the type of days that should be scheduled for execution. This field can only be either "Weekday" or "Weekend". Any other String value will be invalid [Field is optional. Field must be empty if an schedulingDate is already set]
    **month** - Integer between 0 and 11 corresponding to months that should be scheduled for execution. (0 is January and 11 is December) [Field is optional. Field must be empty if an schedulingDate is already set]
    **year** - Integer corresponding to the current year or a future year that should be scheduled for execution. [Field is optional. Field must be empty if an schedulingDate is already set]
    **criteria** - String corresponding to a criteria. This criteria value can only be "First", "Second", "Third", "Fourth" or "Last" and defines a single date to be scheduled for execution given all the other fields previously given. (example: If the day value is set to "20" and criteria set to "Second", the only scheduled date will be the second day 20 since the startDate of the task) [Field is optional. Field must be empty if any schedulingDate is already set or if no field is set]


- DELETE request at <http://localhost:9000/file/:id> - used to delete data of a file with the given id in the url. **[no JSON body required]**
- DELETE request at <http://localhost:9000/task/:id> - used to delete data of a task with the given id in the url. **[no JSON body required]**
- PATCH request at <http://localhost:9000/task/:id> - used to replace 1 or more fields of an existing task with the given id in the url. Fields are given through a JSON body. **[JSON body is required]**

__JSON body fields:__

**toDelete** - Array with 1 or several strings with the names of the fields that are supposed
All other fields are the same as listed in the POST /task JSON fields, but keep in mind all of them are now optional since they are used to replace existing fields. Refer to the examples section below for more information.
Also keep in mind that you will get an error if the deletions/replacements change the task in a way where the task becomes invalid. Check the POST /task JSON field requirements above.


- PUT request at <http://localhost:9000/task/:id> - used to replace an existing task with the given id in the url with a completely new task given through a JSON body. **[JSON body is required]**

Refer to the POST /task JSON fields, since the format used in the PUT /task is the same as the POST /task


## JSON Body Examples

__POST /task JSON body example (RunOnce):__

```
{
    "fileName": "EmailSender",
    "taskType": "RunOnce",
    "startDateAndTime": "16/06/2035 15:00:00"
}
```

The fileName defines the name of the file to be executed, and will only run once, hence the "RunOnce" taskType.
The startDateAndTime here defines when the single run is executed, since the task is "RunOnce".
Note that all other fields are not present because its a "RunOnce" task. The only other field that could have been filled in this case would be the timezone field.

**[The task will run the file "EmailSender" at June 16th 2035 at 3pm]**

__POST /task JSON body example (Periodic):__

```
{
    "fileName": "EmailSender",
    "taskType": "Periodic",
    "periodType": "Daily",
    "period": 3,
    "occurrences": 10,
    "exclusions": [
        {
            "dayType": "Weekend"
        }
    ]

}
```

In this example, we run the file with the name "EmailSender" on a "Periodic" schedule.
We define periodType as "Daily" and period as 3, which defines the periodicity of the task to run every 3 days.
Note that the startDateAndTime field here is not defined, which makes the task's first execution to occur immediately. It also means that every execution in the subsequent days will occur at the same time as the first execution.
Also note that the occurrences field is defined as 10, which makes it so that the task is executed 10 times in the given periodicity, and then terminates.
An exclusion is also set to exclude any day on a weekend from execution. What this means is if any day on the established periodicity (which is every 3 days) ends up on a weekend day, that day will be excluded from execution and no occurrence will be counted.

**[The task will run the file "EmailSender" 10 times starting right now and every 3 days at the same time as the first run, except on weekdays]**

__POST /task JSON body example (Personalized):__

```
{
    "fileName": "EmailSender",
    "taskType": "Personalized",
	"startDateAndTime": "01/09/2030 10:00:00",
	"periodType": "Hourly",
	"period": 2,
	"endDateAndTime": "24/12/2030 00:00:00",
	"timezone": "PST",
	"exclusions":[
	    {
	        "day": 15,
	        "month": 9
	    },
	    {
            "dayOfWeek": 2,
            "criteria": "Last"
	    }
	],
	"schedulings": [
	    {
            "dayType": "Weekday"
	    }
	]
}
```

This example is a task that runs a file called "EmailSender" given a specific task schedule.
Given that its a Personalized task, its execution timing is defined not just by the period and periodType but also by the schedulings.
Therefore, although it runs every 2 hours, it only does so on weekdays starting on September 1st 2030 at 10am and keeps running until December 24th 2030 at midnight.
Note that there is no occurrences field, since a endDateAndTime is defined instead.
Given the exclusions, we can see that every day 15 in every month of October (there's only going to be the 15th of October of 2030 in this example, since the task only runs in the year 2030) and the last Monday in the scheduled task. (which would be December 23rd in this example)
Also keep in mind that the times given are all in PST, given the timezone.

**[The task will run starting September 1st 2030 at 10am and finishing December 24th 2030 at midnight and will run every 2 hours on weekdays only, excluding all 15ths of October and the last Monday of the timeframe]**


