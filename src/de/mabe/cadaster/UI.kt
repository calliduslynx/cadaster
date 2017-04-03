package de.mabe.cadaster

import javafx.application.Application
import javafx.beans.binding.Bindings
import javafx.geometry.Insets
import javafx.geometry.Side
import javafx.geometry.Side.BOTTOM
import javafx.geometry.Side.LEFT
import javafx.scene.Scene
import javafx.scene.chart.NumberAxis
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.*
import javafx.stage.Stage
import java.util.function.Function


fun main(args: Array<String>) {
  Application.launch(UI::class.java, *args)
}

class UI : Application() {
  override fun start(stage: Stage) {
    val axes = Axes(
        width = 400, height = 300,
        xLow = -8.0, xHi = 8.0, xTickUnit = 1.0,
        yLow = -6.0, yHi = 6.0, yTickUnit = 1.0
    )

    val plot = Plot(Function { x -> .25 * (x + 4) * (x + 1) * (x - 2) }, -8.0, 8.0, 1.0, axes)
    val layout = StackPane(plot)
    layout.padding = Insets(20.0)

    stage.title = "Plotter"
    stage.scene = Scene(layout, Color.rgb(35, 39, 50))
    stage.show()
  }


}

class Axes(
    width: Int, height: Int,
    xLow: Double, xHi: Double, xTickUnit: Double,
    yLow: Double, yHi: Double, yTickUnit: Double
) : Pane() {
  val xAxis: NumberAxis
  val yAxis: NumberAxis

  init {
    setMinSize(USE_PREF_SIZE, USE_PREF_SIZE)
    setPrefSize(width.toDouble(), height.toDouble())
    setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE)

    xAxis = NumberAxis(xLow, xHi, xTickUnit)
    xAxis.side = BOTTOM
    xAxis.isMinorTickVisible = false
    xAxis.prefWidth = width.toDouble()
    xAxis.layoutY = (height / 2).toDouble()

    yAxis = NumberAxis(yLow, yHi, yTickUnit)
    yAxis.side = LEFT
    yAxis.isMinorTickVisible = false
    yAxis.prefHeight = height.toDouble()
    yAxis.layoutXProperty().bind(
        Bindings.subtract(
            width / 2 + 1,
            yAxis.widthProperty()
        )
    )

    children.setAll(xAxis, yAxis)
  }
}

class Plot(
    f: Function<Double, Double>,
    xMin: Double, xMax: Double, xInc: Double, axes: Axes
) : Pane() {
  init {
    val path = Path()
    path.stroke = Color.ORANGE.deriveColor(0.0, 1.0, 1.0, 0.6)
    path.strokeWidth = 2.0

    path.clip = Rectangle(// maximaler Rahmen
        0.0, 0.0,
        axes.prefWidth,
        axes.prefHeight
    )


    path.elements.add(MoveTo(mapX(1.0, axes), mapY(1.0, axes)))
    path.elements.add(LineTo(mapX(2.0, axes), mapY(2.0, axes)))
    path.elements.add(LineTo(mapX(2.0, axes), mapY(-2.0, axes)))

    val arcTo = ArcTo()
    arcTo.x = 1.0
    arcTo.y = 1.0
    arcTo.radiusX = 1.0
    arcTo.radiusY = 1.0
    path.elements.add(arcTo)


    var x = xMin
    var y = f.apply(x)

    path.elements.add(
        MoveTo(
            mapX(x, axes), mapY(y, axes)
        )
    )

    x += xInc
    while (x < xMax) {
      y = f.apply(x)

      path.elements.add(
          LineTo(
              mapX(x, axes), mapY(y, axes)
          )
      )

      x += xInc
    }

    setMinSize(USE_PREF_SIZE, USE_PREF_SIZE)
    setPrefSize(axes.prefWidth, axes.prefHeight)
    setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE)

    val circle = Circle()
    circle.centerX = mapX(2.0, axes)
    circle.centerY = mapY(2.0, axes)
    circle.radius = 3.0
    children.setAll(axes, path, circle)

  }

  private fun mapX(x: Double, axes: Axes): Double {
    val tx = axes.prefWidth / 2
    val sx = axes.prefWidth / (axes.xAxis.upperBound - axes.xAxis.lowerBound)

    return x * sx + tx
  }

  private fun mapY(y: Double, axes: Axes): Double {
    val ty = axes.prefHeight / 2
    val sy = axes.prefHeight / (axes.yAxis.upperBound - axes.yAxis.lowerBound)

    return -y * sy + ty
  }
}
