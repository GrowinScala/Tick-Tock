package generators

import org.scalacheck.Gen
import org.scalacheck.Gen._

class Generator extends ImplicitGeneratorConverter {

  /**
   * Generates a random alphanumeric sequence with max
   * @param max maximum number of alphanumeric chars
   * @return Sequence of alphanumeric chars as a String
   */
  private def strGen(max: Int): Gen[String] =
    choose(min = 1, max)
      .flatMap(n => listOfN(n, Gen.alphaNumChar)
        .map(_.mkString))

  def id: String = uuid

  def fileName: String = strGen(10)

}
