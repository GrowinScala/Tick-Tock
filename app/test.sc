import java.util.Calendar

val calendar1 =  Calendar.getInstance()
calendar1.set(2030,1,1)
val calendar2 = Calendar.getInstance()
calendar2.set(2031,1,1)


calendar1.get(Calendar.MONTH)// mes
calendar1.get(Calendar.DAY_OF_MONTH) // dia

val time =  calendar2.getTimeInMillis -  calendar1.getTimeInMillis

time / (1000 * 60 * 60 * 24)