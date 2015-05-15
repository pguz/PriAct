package actor.db
import scala.slick.driver.H2Driver.simple._
import scala.slick.lifted.{ProvenShape, ForeignKeyQuery}

class PricesData(tag: Tag)
  extends Table[(Int, String, String, String, String, String)](tag, "PRICES") {

  def id: Column[Int] = column[Int]("PRICE_ID", O.PrimaryKey)
  def name: Column[String] = column[String]("PROD_NAME")
  def shop: Column[String] = column[String]("SHOP")
  def price: Column[String] = column[String]("PRICE")
  def date: Column[String] = column[String]("DATE")

  // Every table needs a * projection with the same type as the table's type parameter
  def * : ProvenShape[(Int, String, String, String, String)] =
    (id, name, shop, price, date)
}