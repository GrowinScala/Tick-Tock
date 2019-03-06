package api.utils

//This could be done with Play Framework "MessagesAPI". Explore MessagesAPI although the "obeject/method/val" approach is totally fine and sometimes better (less stuff being loaded from libs)
object ErrorMessages {
  def fileNotFoundError(id: String): String = "File with id " + id + " does not found."

  val dumbestErrorEver: String = "No clue what happened. But it all went South..."
}
