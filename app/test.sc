import java.util.{Calendar, Date}

val calendar1 = Calendar.getInstance()
val calendar2 = Calendar.getInstance()

calendar1.set(2030, 0, 1, 0, 0, 0)
calendar2.set(2030, 0, 32, 0, 0, 0)

val date1 = calendar1.getTime
val date2 = calendar2.getTime

calendar1.add(Calendar.DAY_OF_MONTH, 1)

calendar1.getTime