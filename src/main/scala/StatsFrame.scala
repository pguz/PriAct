import java.awt.Dimension
import javax.swing.UIManager
import javax.swing.table.DefaultTableModel

import observablex.SchedulerEx

import scala.swing.Orientation._
import scala.swing.Swing._
import scala.swing._


class StatsFrame extends Frame with ConcreteSwingApi {

    title = "Price Monitoring"
    minimumSize = new Dimension(900, 600)
    val txtSearch = new TextField

    val btnAbstSearch = new Button("Szukaj")
    val mdlAbstSearch = new DefaultTableModel( new Array[Array[AnyRef]](0), Array[AnyRef]("Rezultat") ) {
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
    val mdlConcSearch = new DefaultTableModel( new Array[Array[AnyRef]](0), Array[AnyRef]("Rezultat") ) {
      override def isCellEditable(r: Int, c: Int): Boolean = false
    }
    val tblConcProds      = new Table(25, 1) {
      rowHeight = 25
      autoResizeMode = Table.AutoResizeMode.NextColumn
      showGrid = true
      gridColor = new java.awt.Color(150, 150, 150)
      model = mdlConcSearch
    }

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
          contents += new ScrollPane(tblStats)
        }
      }
      contents += txtStatus
    }

    val eventScheduler = SchedulerEx.SwingEventThreadScheduler
}
