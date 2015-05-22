package actor.db

import scala.slick.ast.ColumnOption.AutoInc
import scala.slick.driver.H2Driver.simple._
import scala.slick.lifted.{ProvenShape}

class Request(tag: Tag)
  extends Table[(Int, String, java.sql.Timestamp)](tag, "REQUEST") {


  def id: Column[Int] = column[Int]("REQUEST_ID", O.PrimaryKey, O.AutoInc)
  def content: Column[String] = column[String]("CONTENT")
  def date: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("DATE")

  def * : ProvenShape[(Int, String, java.sql.Timestamp)] =
    (id, content, date)
}

class Product(tag: Tag)
  extends Table[(Int, String, String)](tag, "PRODUCT") {


  def id: Column[Int] = column[Int]("PRODUCT_ID", O.PrimaryKey, O.AutoInc)
  def name: Column[String] = column[String]("NAME")
  def url: Column[String] = column[String]("URL")

  def * : ProvenShape[(Int, String, String)] =
    (id, name, url)
}

class Price(tag: Tag)
  extends Table[(Int, Int, Double)](tag, "PRICE") {


  def reqId: Column[Int] = column[Int]("REQUEST_ID")
  def prodId: Column[Int] = column[Int]("PRODUCT_ID")
  def value: Column[Double] = column[Double]("VALUE")

  def fk1 = foreignKey("SUP_FK", reqId, TableQuery[Request])(_.id)
  def fk2 = foreignKey("PROD_FK", prodId, TableQuery[Product])(_.id)
  def pk = primaryKey("PRICE_PK", (reqId, prodId))

  def * : ProvenShape[(Int, Int, Double)] =
    (reqId, prodId, value)
}