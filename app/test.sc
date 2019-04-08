import api.dtos.ExclusionDTO

val list1 = List(List(7, 4), List(7, 6))

val list2 = List(1, 5, 8)

val desired_list1 = List(List(1, 7, 4), List(5, 7, 4), List(8, 7, 4))

val desired_list2 = List(List(1, 7, 6), List(5, 7, 6), List(8, 7, 6))

//list1.flatMap(innerList => innerList.map(list2.map(_ :: innerList)))
list2.flatMap(element=> list1.map(innerList=> element:: innerList))


val a : Option[List[ExclusionDTO]] = None

a.get.head