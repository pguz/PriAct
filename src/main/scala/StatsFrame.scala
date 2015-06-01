import java.awt.Dimension
import javax.swing.table.DefaultTableModel

import observablex.SchedulerEx
import rx.lang.scala.Observable

import scala.swing.Orientation._
import scala.swing.Swing._
import scala.swing._
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global


class StatsFrame extends Frame with ConcreteSwingApi with ClientActorApi {

    title = "Price Monitoring"
    minimumSize = new Dimension(900, 600)
    val txtSearch = new TextField

    val btnAbstSearch = new Button("Szukaj")
    val mdlAbstSearch = new DefaultTableModel( new Array[Array[AnyRef]](0), Array[AnyRef]("Query ID", "Query", "Timestamp") ) {
      override def isCellEditable(r: Int, c: Int): Boolean = false
    }
    val tblAbstProds      = new Table(25, 1) {
      rowHeight = 25
      autoResizeMode = Table.AutoResizeMode.NextColumn
      showGrid = true
      gridColor = new java.awt.Color(150, 150, 150)
      model = mdlAbstSearch
    }
    val btnConcSearch = new Button("Pobierz")
    val mdlConcSearch = new DefaultTableModel( new Array[Array[AnyRef]](0), Array[AnyRef]("Product ID", "Price") ) {
      override def isCellEditable(r: Int, c: Int): Boolean = false
    }
    val tblConcProds      = new Table(25, 1) {
      rowHeight = 25
      autoResizeMode = Table.AutoResizeMode.NextColumn
      showGrid = true
      gridColor = new java.awt.Color(150, 150, 150)
      model = mdlConcSearch
    }
    val btnStats = new Button("Statystyki")
    val mdlStats = new DefaultTableModel( new Array[Array[AnyRef]](0), Array[AnyRef]("Min", "Max", "Avg") ) {
      override def isCellEditable(r: Int, c: Int): Boolean = false
    }
    val tblStats     = new Table(25, 3) {
      rowHeight = 25
      autoResizeMode = Table.AutoResizeMode.NextColumn
      showGrid = true
      gridColor = new java.awt.Color(150, 150, 150)
      model = mdlStats
    }
    val txtStatus     = new Label(" ")

    contents = new BoxPanel(orientation = Vertical) {
      border = EmptyBorder(top = 5, left = 5, bottom = 5, right = 5)
      contents += new BoxPanel(orientation = Horizontal) {
        contents += new BoxPanel(orientation = Vertical) {
          maximumSize = new Dimension(240, 900)
          border = EmptyBorder(top = 10, left = 10, bottom = 10, right = 10)
          contents += new BoxPanel(orientation = Vertical) {
            maximumSize = new Dimension(640, 30)
            border = EmptyBorder(top = 5, left = 0, bottom = 5, right = 0)
            contents += new BoxPanel(orientation = Horizontal) {
              contents += txtSearch
            }
            contents += new BorderPanel {
              add(btnAbstSearch, BorderPanel.Position.Center)
            }
          }

          contents += new ScrollPane(tblAbstProds)
          contents += new BorderPanel {
            maximumSize = new Dimension(640, 30)
            add(btnConcSearch, BorderPanel.Position.Center)
          }
        }
        contents += new BoxPanel(orientation = Vertical) {
          //maximumSize = new Dimension(240, 900)
          border = EmptyBorder(top = 10, left = 10, bottom = 10, right = 10)
          contents += new ScrollPane(tblConcProds)
          contents += btnStats
          contents += new ScrollPane(tblStats)
        }
      }
      contents += txtStatus
    }

    val eventScheduler = SchedulerEx.SwingEventThreadScheduler

  val obsSearchAbst: Observable[String] = btnAbstSearch.clicks.observeOn(eventScheduler).map(
    _ => txtSearch.text)

  val obsSearchConc: Observable[Int] = btnConcSearch.clicks.observeOn(eventScheduler).filter(_
    => tblAbstProds.peer.getSelectedRowCount == 1).map(_
    => mdlAbstSearch.getValueAt(tblAbstProds.peer.getSelectedRow, 0).toString.toInt)

  val obsStats: Observable[Int] = btnStats.clicks.observeOn(eventScheduler).filter(_
    => tblConcProds.peer.getSelectedRowCount == 1).map(_
    => mdlConcSearch.getValueAt(tblConcProds.peer.getSelectedRow, 0).toString.toInt)


  obsSearchAbst.subscribe(
    txt => fRequestQuriesList(txt) onComplete {
      case Success(list)   =>
        if(mdlAbstSearch.getRowCount > 0) mdlAbstSearch.setRowCount(0)
        list.foreach{ res =>
          mdlAbstSearch.addRow(Array[AnyRef](res._1.toString, res._2, res._3))}
      case Failure(err) => txtStatus.text = "getPrices error: " + err.getMessage
    }
  )

  obsSearchConc.subscribe(
    id => fRequestQueryResult(id) onComplete {
      case Success(list)   =>
        if(mdlConcSearch.getRowCount > 0) mdlConcSearch.setRowCount(0)
        println("Success list: " + list)
        list.foreach{ res =>
          mdlConcSearch.addRow(Array[AnyRef](res._1.toString, res._2.toString))}
      case Failure(err) => txtStatus.text = "getPrices error: " + err.getMessage
    }
  )

  obsStats.subscribe(
    id => fRequestQueryStats(id) onComplete {
      case Success(stats)   =>
        if(mdlStats.getRowCount > 0) mdlStats.setRowCount(0)
        println("Success list: " + stats)
        mdlStats.addRow(Array[AnyRef](stats._1.toString, stats._2.toString, stats._3.toString))
      case Failure(err) => txtStatus.text = "getPrices error: " + err.getMessage
    }
  )
}
