/*package purchase

import slick.driver.MySQLDriver.api._

object ReceiptMappings {
  case class ReceiptRow(
                         receiptId: Int,
                         userId: Int,
                         total: Double
                       )

  class ReceiptTable(tag: Tag) extends Table[ReceiptRow](tag, "receipts"){
    def receiptId = column[Int]("receiptId", O.PrimaryKey, O.AutoInc)
    def userId = column[Int]("userId", O.Length(100))
    def total = column[Double]("total", O.Length(100))

    def * = (receiptId, userId, total) <> (ReceiptRow.tupled, ReceiptRow.unapply)
  }

  lazy val all = TableQuery[ReceiptTable]

  object queryExtensions {
    def byId(id: Int): Query[ReceiptTable, ReceiptRow, Seq] = {
      all.filter(_.receiptId === id)
    }
  }
}*/

