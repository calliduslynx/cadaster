package de.mabe.cadaster.util

import de.mabe.cadaster.expression.*

infix fun String.indentBy(indentation: Int) = this.lines().map { " ".repeat(indentation) + it }.joinToString("\n")
private infix fun String.toLength(length: Int) = if (this.length < length) this + " ".repeat(length - this.length) else this

private

class TextBlock(str: String, middle: Int? = null) {
  val width: Int = str.lines().map { it.length }.max()!!
  val text: String = str.lines().map { it toLength width }.joinToString("\n")
  val middle = middle ?: width / 2


  infix fun over(under: TextBlock): TextBlock {
    val over = this

    var overText = over.text
    var underText = under.text
    val overMiddle = over.middle
    val underMiddle = under.middle

    val middleDiff = overMiddle - underMiddle
    when {
      middleDiff > 0 -> underText = underText indentBy middleDiff
      middleDiff < 0 -> overText = overText indentBy -middleDiff
    }

    return TextBlock(overText + "\n" + underText, maxOf(overMiddle, underMiddle))

  }

  infix fun overWithPipe(under: TextBlock): TextBlock {
    val over = this

    var overText = over.text
    var underText = under.text
    val overMiddle = over.middle
    val underMiddle = under.middle

    val middleDiff = overMiddle - underMiddle
    when {
      middleDiff > 0 -> underText = underText indentBy middleDiff
      middleDiff < 0 -> overText = overText indentBy -middleDiff
    }

    val pipe = ("|" indentBy (maxOf(overMiddle, underMiddle))) + "\n"

    return TextBlock(overText + "\n" + pipe + underText)
  }

  infix fun leftTo(right: TextBlock): TextBlock {
    val SPACER = "  "

    val left = this
    val leftWithLine = TextBlock("""/""".indentBy(left.middle) + "\n" + left.text)
    val rightWithLine = TextBlock("""\""".indentBy(right.middle) + "\n" + right.text)

    val leftLines = leftWithLine.text.lines()
    val rightLines = rightWithLine.text.lines()
    val noOfLines = maxOf(leftLines.size, rightLines.size)

    var str = ""
    for (lineNr in 0..noOfLines - 1) {
      val leftPart = if (leftLines.size > lineNr) leftLines[lineNr] else " ".repeat(leftWithLine.width)
      val rightPart = if (rightLines.size > lineNr) rightLines[lineNr] else ""

      str += leftPart + SPACER + rightPart + "\n"
    }

    val leftMiddle = leftWithLine.middle
    val rightMiddle = rightWithLine.middle + leftWithLine.width + SPACER.length
    val targetMiddle = 1 + (leftMiddle + rightMiddle) / 2

    return TextBlock(str, targetMiddle)
  }

  override fun toString() = text
}

fun <T> getAsTree(element: T, getName: (T) -> String, getChildren: (T) -> List<T>): String {
  fun buildTree(element: T): TextBlock {
    val children = getChildren.invoke(element)
    return when (children.size) {
      0 -> TextBlock(getName.invoke(element))
      1 -> TextBlock(getName.invoke(element)) overWithPipe buildTree(children[0])
      2 -> TextBlock(getName.invoke(element)) over
          (buildTree(children[0]) leftTo buildTree(children[1]))
      else -> throw IllegalStateException("mehr als 2 Kinder")
    }
  }

  return buildTree(element).toString()
}

fun main(args: Array<String>) {
  println(getAsTree(-y, { it.stringForGraph }, { it.children() }))
  println("\n----------------------\n")
  println(getAsTree(x - y, { it.stringForGraph }, { it.children() }))
  println("\n----------------------\n")
  println(getAsTree(Wurzel(x) as Expression, { it.stringForGraph }, { it.children() }))
  println("\n----------------------\n")
  println(getAsTree(x + (x + x), { it.stringForGraph }, { it.children() }))
  println("\n----------------------\n")
  println(getAsTree(x * -1231233, { it.stringForGraph }, { it.children() }))
  println("\n----------------------\n")
  println(getAsTree((x * -3) + (x * 4), { it.stringForGraph }, { it.children() }))
  println("\n----------------------\n")
  println(getAsTree(Val(2) + x + y - z, { it.stringForGraph }, { it.children() }))
  println("\n----------------------\n")
  println(getAsTree((x + y) - z, { it.stringForGraph }, { it.children() }))
  println("\n----------------------\n")
  println(getAsTree(x + y - z + 1234567 + 123456 + 9876543 + 2345678 + (y * 43234567 + Wurzel(x + 234567)), { it.stringForGraph }, { it.children() }))
}
