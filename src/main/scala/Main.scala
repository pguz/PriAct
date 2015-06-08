import observablex._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.swing._
import swing.Swing._
import javax.swing.table.{DefaultTableModel}
import javax.swing.UIManager
import Orientation._
import scala.util.{Success, Failure }
import rx.lang.scala.Observable


object Main extends SimpleSwingApplication with ConcreteSwingApi with ClientActorApi {
  
  {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch {
      case t: Throwable =>
    }
  }
  

  def top = new MainFrame {
    val crawList = List("Allegro", "Olx", "Gumtree")

    title = "Price Actual"
    minimumSize = new Dimension(900, 600)

    val cboxAllegro   = new CheckBox("Allegro") {
      selected = true
    }
    val cboxGumtree   = new CheckBox("Gumtree") {
      selected = true
    }
    val cboxOlx       = new CheckBox("Olx") {
      selected = true
    }
    val txtSearchProd = new TextField
    val btnSearchProd = new Button("Product")
    val btnSearchDesc = new Button("Description")
    val mdlProds      = new DefaultTableModel( new Array[Array[AnyRef]](0), Array[AnyRef]("Sklep", "Id", "Nazwa", "Cena") ) {
      override def isCellEditable(r: Int, c: Int): Boolean = false
    }
    val btnStats      = new Button("Statistics")

    val tblProds      = new Table(25, 4) {
      rowHeight = 25
      autoResizeMode = Table.AutoResizeMode.NextColumn
      showGrid = true
      gridColor = new java.awt.Color(150, 150, 150)
      model = mdlProds
      val colId = peer.getColumn("Id")
      colId.setMinWidth(0)
      colId.setPreferredWidth(0)
      colId.setMaxWidth(0)
      val colShop = peer.getColumn("Sklep")
      colShop.setMinWidth(60)
      colShop.setPreferredWidth(60)
      colShop.setMaxWidth(60)
      val colPrice = peer.getColumn("Cena")
      colPrice.setMinWidth(60)
      colPrice.setPreferredWidth(60)
      colPrice.setMaxWidth(60)
      //http://stackoverflow.com/questions/9588765/using-tablerowsorter-with-scala-swing-table
      //peer.setRowSorter(new TableRowSorter(model))
    }
    val edtDesc       = new EditorPane {
      import javax.swing.border._
      border = new EtchedBorder(EtchedBorder.LOWERED)
      editable = false
    }
    val txtStatus     = new Label(" ")


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
              contents += txtSearchProd
            }
            contents += new BorderPanel {
              add(btnSearchProd, BorderPanel.Position.Center)
            }
          }

          contents += new ScrollPane(tblProds)
          contents += new BorderPanel {
            maximumSize = new Dimension(640, 30)
            add(btnSearchDesc, BorderPanel.Position.Center)
          }
        }
        contents += new ScrollPane(edtDesc)
      }
      contents += new BoxPanel(orientation = Vertical) {
        border = EmptyBorder(top = 5, left = 5, bottom = 5, right = 5)
        contents += new BorderPanel {
          add(btnStats, BorderPanel.Position.Center)
        }
      }
      contents += txtStatus
    }

    def dspStatus(s: String) = {
      txtStatus.text = s
    }

    val eventScheduler = SchedulerEx.SwingEventThreadScheduler

    val obsCboxAll: Observable[Boolean]       = cboxAllegro.stateValues.observeOn(eventScheduler)
    val obsCboxGum: Observable[Boolean]       = cboxGumtree.stateValues.observeOn(eventScheduler)
    val obsCboxOlx: Observable[Boolean]       = cboxOlx.stateValues.observeOn(eventScheduler)
    val obsBtnSearchProd: Observable[Button]  = btnSearchProd.clicks.observeOn(eventScheduler)
    val obsBtnSearchDesc: Observable[Button]  = btnSearchDesc.clicks.observeOn(eventScheduler)
    val obsBtnStats: Observable[Button]       = btnStats.clicks.observeOn(eventScheduler)

    val obsAll:   Observable[(String, Boolean)] = obsCboxAll.map(("Allegro", _))
    val obsGum:   Observable[(String, Boolean)] = obsCboxGum.map(("Gumtree", _))
    val obsOlx:   Observable[(String, Boolean)] = obsCboxOlx.map(("Olx", _))
    val cboxObs:  Observable[(String, Boolean)] = obsAll.merge(obsGum).merge(obsOlx)


    val obsSearchProd: Observable[String]
      = obsBtnSearchProd.map(_ => txtSearchProd.text)
    val obsSearchDesc: Observable[(String, String)]
      = obsBtnSearchDesc.filter(_ => tblProds.peer.getSelectedRowCount == 1)
        .map(_ => (mdlProds.getValueAt(tblProds.peer.getSelectedRow, 0).toString,
                    mdlProds.getValueAt(tblProds.peer.getSelectedRow, 1).toString))

    crawList foreach {
      crawler => createCrawler(crawler) onComplete {
        case Success(true)    => dspStatus(crawler + " has been added.")
        case Success(false)   => dspStatus(crawler + " has not been found.")
        case Failure(err)     => dspStatus("createCrawler error: " + err.getMessage)
      }
    }

    cboxObs.subscribe( _ match {
      case (name, true)   => createCrawler(name)
      case (name, false)  => removeCrawler(name)
    })

    obsSearchProd.subscribe(
      n => getPrices(n) onComplete {
        case Success(results) =>
          if(mdlProds.getRowCount > 0) mdlProds.setRowCount(0)
          results.sortBy(_._4).foreach{ res =>
            mdlProds.addRow(Array[AnyRef](res._1, res._2, res._3, res._4.toString()))}
        case Failure(err)     =>
          dspStatus("getPrices error: " + err.getMessage)
      }
    )

    obsSearchDesc.subscribe {
      n =>
        println("obsSearchDesc")
        getDescription(n._1, n._2) onComplete {
          case Success(desc) =>
            edtDesc.text = desc
          case Failure(err) =>
            edtDesc.text =
              "Error: " + err.getMessage
        }
    }

    obsBtnStats.subscribe { _ =>
      val statsFrame = new StatsFrame
      //statsFrame.pack
      statsFrame.visible = true
    }
  }
}

