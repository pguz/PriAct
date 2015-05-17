
import javax.swing.table.{DefaultTableModel, TableModel}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.swing._
import scala.util.{Success, Failure }
import scala.swing.event._
import swing.Swing._
import javax.swing.UIManager
import Orientation._
import rx.lang.scala.Observable
import observablex._

object Main extends SimpleSwingApplication with ConcreteSwingApi with ClientActorApi {
  
  {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch {
      case t: Throwable =>
    }
  }
  

  def top = new MainFrame {
    title = "Price Actual"
    minimumSize = new Dimension(900, 600)

    val cboxAllegro = new CheckBox("Allegro")
    val cboxGumtree = new CheckBox("Gumtree")
    val cboxOlx = new CheckBox("Olx")

    val btnFind = new Button("Find")
    val btnChoose = new Button("Choose")
    val searchTermField = new TextField
    val tableModel = new DefaultTableModel( new Array[Array[AnyRef]](0), Array[AnyRef]("Sklep", "Nazwa", "Cena") )
    val productsList = new Table(25, 3) {
      import javax.swing.table._
      rowHeight = 25
      autoResizeMode = Table.AutoResizeMode.NextColumn
      showGrid = true
      gridColor = new java.awt.Color(150, 150, 150)
      model = tableModel

      peer.setRowSorter(new TableRowSorter(model))
    }
    val status = new Label(" ")
    val editorpane = new EditorPane {
      import javax.swing.border._
      border = new EtchedBorder(EtchedBorder.LOWERED)
      editable = false
      peer.setContentType("text/html")
    }

    contents = new BoxPanel(orientation = Vertical) {
      border = EmptyBorder(top = 5, left = 5, bottom = 5, right = 5)
      contents += new BoxPanel(orientation = Horizontal) {
        contents += new BoxPanel(orientation = Vertical) {
          maximumSize = new Dimension(240, 900)
          border = EmptyBorder(top = 10, left = 10, bottom = 10, right = 10)
          contents += new BoxPanel(orientation = Horizontal) {
            maximumSize = new Dimension(640, 30)
            border = EmptyBorder(top = 5, left = 0, bottom = 5, right = 0)
            contents.append(cboxAllegro, cboxGumtree, cboxOlx)
          }
          contents += new BoxPanel(orientation = Vertical) {
            maximumSize = new Dimension(640, 30)
            border = EmptyBorder(top = 5, left = 0, bottom = 5, right = 0)
            contents += new BoxPanel(orientation = Horizontal) {
              contents += searchTermField
            }
            contents += new BorderPanel {
              add(btnFind, BorderPanel.Position.Center)
            }
          }

          contents += new ScrollPane(productsList)
          contents += new BorderPanel {
            maximumSize = new Dimension(640, 30)
            add(btnChoose, BorderPanel.Position.Center)
          }
        }
        contents += new ScrollPane(editorpane)
      }
      contents += status
    }

    def displayCom(s: String) = {
      status.text = s
    }
    
    val eventScheduler = SchedulerEx.SwingEventThreadScheduler

    val crawList = List("Allegro", "Gumtree")

    crawList foreach {
      crawler => createCrawler(crawler) onComplete {
        case Success(true)    => displayCom(crawler + " has been added.")
        case Success(false)   => displayCom(crawler + " has not been found.")
        case Failure(err)     => displayCom("createCrawler error: " + err.getMessage)
      }
    }

    val obs: Observable[String] = btnFind.clicks.observeOn(eventScheduler).map(
        _ => searchTermField.text)

    val cboxObs: Observable[Boolean] = cboxAllegro.stateValues.observeOn(eventScheduler)
    cboxObs.subscribe(
      v => println("value: " + v)
    )

    obs.subscribe(
      n => getPrices(n) onComplete {
        case Success(results)   =>
          if(tableModel.getRowCount > 0) tableModel.setRowCount(0)
          results.sortBy(_._2).foreach{ res =>
            tableModel.addRow(Array[AnyRef](res._1, n ,res._2.toString()))}
        case Failure(err) => displayCom("getPrices error: " + err.getMessage)
      }
    )
  }


}

trait ConcreteSwingApi extends SwingApi {
  type ValueChanged = scala.swing.event.ValueChanged
  object ValueChanged {
    def unapply(x: Event) = x match {
      case vc: ValueChanged => Some(vc.source.asInstanceOf[TextField])
      case _ => None
    }
  }
  type ButtonClicked = scala.swing.event.ButtonClicked
  object ButtonClicked {
    def unapply(x: Event) = x match {
      case bc: ButtonClicked => Some(bc.source.asInstanceOf[Button])
      case _ => None
    }
  }

  type StateChanged = scala.swing.event.ButtonClicked
  object StateChanged {
    def unapply(x: Event) = x match {
      case sc: StateChanged => Some(sc.source.asInstanceOf[CheckBox])
      case _ => None
    }
  }
  
  type TextField = scala.swing.TextField
  type Button = scala.swing.Button
  type CheckBox = scala.swing.CheckBox
}
