package actor.db

import scala.slick.ast.ColumnOption.AutoInc
import scala.slick.driver.H2Driver.simple._
import scala.slick.lifted.{ProvenShape}

class Queries(tag: Tag)
  extends Table[(Int, String, java.sql.Timestamp)](tag, "QUERIES") {


  def id: Column[Int] = column[Int]("QUERY_ID", O.PrimaryKey, O.AutoInc)
  def content: Column[String] = column[String]("CONTENT")
  def date: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("DATE")

  def * : ProvenShape[(Int, String, java.sql.Timestamp)] =
    (id, content, date)
}

class Products(tag: Tag)
  extends Table[(Int, String, String)](tag, "PRODUCTS") {


  def id: Column[Int] = column[Int]("PRODUCT_ID", O.PrimaryKey, O.AutoInc)
  def name: Column[String] = column[String]("NAME")
  def url: Column[String] = column[String]("URL")

  def uniqueUrl = index("URLs", url, unique = true)

  def * : ProvenShape[(Int, String, String)] =
    (id, name, url)
}

class Prices(tag: Tag)
  extends Table[(Int, Int, Double)](tag, "PRICES") {


  def queryId: Column[Int] = column[Int]("QUERY_ID")
  def prodId: Column[Int] = column[Int]("PRODUCT_ID")
  def value: Column[Double] = column[Double]("VALUE")

  def fk1 = foreignKey("QUERY_FK", queryId, TableQuery[Queries])(_.id)
  def fk2 = foreignKey("PROD_FK", prodId, TableQuery[Products])(_.id)
  def pk = primaryKey("PRICE_PK", (queryId, prodId))

  def * : ProvenShape[(Int, Int, Double)] =
    (queryId, prodId, value)
}