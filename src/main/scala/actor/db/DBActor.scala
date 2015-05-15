package actor.db

/**
 * Created by klis on 15.05.15.
 */
import scala.slick.driver.H2Driver.simple._

class DBActor {

  // The query interface for the Suppliers table
  val prices: TableQuery[PricesData] = TableQuery[PricesData]

  val db = Database.forURL("jdbc:h2:mem:hello", driver = "org.h2.Driver")
  db.withSession { implicit session => prices.ddl.create }

}
