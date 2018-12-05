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

To run the project, run the command ```sbt run``` and go to <http://localhost:9000>. You should see a message saying "It works!".

To test other endpoints, you can try sending a POST request to <http://localhost:9000/schedule> with a JSON body looking like this:

```
{
	"startDateAndTime": "05/12/2018 10:42:00",
	"taskName": "EmailSender"
}
```
This will schedule the EmailSender file (which is already available within the project) to run on the 5th of December at 10:42 AM. Keep in mind you need a [MySQL](https://www.mysql.com/) database set up for this to work properly.

I reccomend using [Postman](https://www.getpostman.com/) to test HTTP requests with ease.


