
package generators

import java.util.UUID

import org.scalacheck.Gen

class ImplicitGeneratorConverter {

  /**
   * Implicit conversion from Gen[T] to T
   * @param g Gen of Generic Type
   * @tparam T Generic Type
   * @return Generic Type
   */
  implicit def transformAny[T](g: Gen[T]): T = {
    g.sample.get
  }

  /**
   * Implicit conversion of Gen[UUID] to String
   * @param s Gen of UUID
   * @return String
   */
  implicit def transformEmailUUID(s: Gen[UUID]): String = {
    s.sample.get.toString
  }

}