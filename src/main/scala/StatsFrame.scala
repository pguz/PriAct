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
    maximumSize = new Dimension(900, 640)
    size = new Dimension(900, 640)
    minimumSize = new Dimension(900, 640)
    preferredSize = new Dimension(900, 640)
    val txtSearch = new TextField

    val btnAbstSearch = new Button("Search")
    val mdlAbstSearch = new DefaultTableModel( new Array[Array[AnyRef]](0), Array[AnyRef]("ID", "Query", "Timestamp") ) {
      override def isCellEditable(r: Int, c: Int): Boolean = false
    }
    val tblAbstProds      = new Table(25, 1) {
      rowHeight = 25
      autoResizeMode = Table.AutoResizeMode.NextColumn
      showGrid = true
      gridColor = new java.awt.Color(150, 150, 150)
      model = mdlAbstSearch

      val colQueryId = peer.getColumn("ID")
      colQueryId.setMinWidth(40)
      colQueryId.setPreferredWidth(40)
      colQueryId.setMaxWidth(40)

      val colTimestamp = peer.getColumn("Timestamp")
      colTimestamp.setMinWidth(200)
      colTimestamp.setPreferredWidth(200)
      colTimestamp.setMaxWidth(200)
    }

    val mdlAbstStats = new DefaultTableModel( new Array[Array[AnyRef]](0), Array[AnyRef]("Min", "Max", "Avg") ) {
      override def isCellEditable(r: Int, c: Int): Boolean = false
    }
    val tblAbstStats     = new Table(1, 3) {
      rowHeight = 25
      autoResizeMode = Table.AutoResizeMode.NextColumn
      showGrid = true
      gridColor = new java.awt.Color(150, 150, 150)
      model = mdlAbstStats
    }


    val btnConcSearch = new Button("Get")
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
    val btnConcStats = new Button("Stats")
    val mdlConcStats = new DefaultTableModel( new Array[Array[AnyRef]](0), Array[AnyRef]("Min", "Max", "Avg") ) {
      override def isCellEditable(r: Int, c: Int): Boolean = false
    }
    val tblConcStats     = new Table(1, 3) {
      rowHeight = 25
      autoResizeMode = Table.AutoResizeMode.NextColumn
      showGrid = true
      gridColor = new java.awt.Color(150, 150, 150)
      model = mdlConcStats
    }
    val txtStatus     = new Label(" ")

    contents = new BoxPanel(orientation = Vertical) {
      border = EmptyBorder(top = 5, left = 5, bottom = 5, right = 5)
      contents += new BoxPanel(orientation = Horizontal) {
        contents += new BoxPanel(orientation = Vertical) {
          border = EmptyBorder(top = 10, left = 10, bottom = 10, right = 10)
          contents += new BoxPanel(orientation = Vertical) {
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
            maximumSize = new Dimension(450, 30)
            add(btnConcSearch, BorderPanel.Position.Center)
          }


        }
        contents += new BoxPanel(orientation = Vertical) {
          border = EmptyBorder(top = 10, left = 10, bottom = 10, right = 10)

          val paneAbstStats = new ScrollPane(tblAbstStats)
          paneAbstStats.maximumSize = new Dimension(450, 56)
          paneAbstStats.minimumSize = new Dimension(450, 56)
          paneAbstStats.preferredSize = new Dimension(450, 56)
          contents += paneAbstStats

          contents += new ScrollPane(tblConcProds)

          contents += new BorderPanel {
            maximumSize = new Dimension(450, 30)
            add(btnConcStats, BorderPanel.Position.Center)
          }

          val paneConcStats = new ScrollPane(tblConcStats)
          paneConcStats.maximumSize = new Dimension(450, 56)
          paneConcStats.minimumSize = new Dimension(450, 56)
          paneConcStats.preferredSize = new Dimension(450, 56)
          contents += paneConcStats
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

  val obsConcStats: Observable[Int] = btnConcStats.clicks.observeOn(eventScheduler).filter(_
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
        fRequestQueryStats(id) onComplete {
          case Success(stats)   =>
            if(mdlAbstStats.getRowCount > 0) mdlAbstStats.setRowCount(0)
            println("Success list: " + stats)
            mdlAbstStats.addRow(Array[AnyRef](stats._1.toString, stats._2.toString, stats._3.toString))
          case Failure(err) => txtStatus.text = "getPrices error: " + err.getMessage
        }
      case Failure(err) => txtStatus.text = "getPrices error: " + err.getMessage
    }
  )

  obsConcStats.subscribe(
    id => fRequestProductStats(id) onComplete {
      case Success(stats)   =>
        if(mdlConcStats.getRowCount > 0) mdlConcStats.setRowCount(0)
        println("Success list: " + stats)
        mdlConcStats.addRow(Array[AnyRef](stats._1.toString, stats._2.toString, stats._3.toString))
      case Failure(err) => txtStatus.text = "getPrices error: " + err.getMessage
    }
  )
}
