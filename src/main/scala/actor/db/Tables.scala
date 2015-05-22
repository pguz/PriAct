package actor.db

import scala.slick.ast.ColumnOption.AutoInc
import scala.slick.driver.H2Driver.simple._
import scala.slick.lifted.{ProvenShape}

class PricesData(tag: Tag)
  extends Table[(Int, String, String, Double, java.sql.Timestamp)](tag, "PRICES") {

  def id: Column[Int] = column[Int]("PRICE_ID", O.PrimaryKey, O.AutoInc)
  def name: Column[String] = column[String]("PROD_NAME")
  def shop: Column[String] = column[String]("SHOP")
  def price: Column[Double] = column[Double]("PRICE")
  def date: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("DATE")

  // Every table needs a * projection with the same type as the table's type parameter
  def * : ProvenShape[(Int, String, String, Double, java.sql.Timestamp)] =
    (id, name, shop, price, date)
}