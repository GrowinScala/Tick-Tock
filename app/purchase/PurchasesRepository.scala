/*package purchase

import purchase.ReceiptMappings._

import scala.concurrent._

class PurchasesRepository {
  val db = Database.forConfig("dbinfo")

  def insertPurchase(purchase: PurchaseRepositoryDTO): Unit = {
    //calculo do total:
    //(...)
    val total = 10
    val newEntry = ReceiptRow(0, purchase.userId, total)

    val query = receiptTable += newEntry

    val result = for {
      receiptId <- receiptTable += newEntry
      purchaseItem <- purchase.PurchasedItems
      productId <- productsTable.filter()
    } yield (purchasedItemsTable += PurchaseItemRow(receiptId, ))
  }
}*/
