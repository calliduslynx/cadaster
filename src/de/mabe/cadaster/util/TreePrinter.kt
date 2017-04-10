package de.mabe.cadaster.util

import de.mabe.cadaster.expression.*
import de.mabe.cadaster.util.Boxer.*
import de.mabe.cadaster.util.PrintChar.*
import de.mabe.cadaster.util.Side.left
import de.mabe.cadaster.util.Side.right

infix fun String.indentBy(indentation: Int) = this.lines().map { " ".repeat(indentation) + it }.joinToString("\n")
infix fun String.indentRightBy(indentation: Int) = this.lines().map { it + " ".repeat(indentation) }.joinToString("\n")
private infix fun String.toLength(length: Int) = if (this.length < length) this + " ".repeat(length - this.length) else this
private fun Int.isEven() = this / 2 * 2 == this
private fun Int.isOdd() = !this.isEven()


enum class Side {left, right }
enum class PrintChar(val left: String, val middle: String = left, val right: String = left) {
  V("V"), SLASH("/", "|", "\\"), PIPE("|");

  fun get(side: Side) = if (side == Side.left) left else right
}

/** first: top, second: bottom */
var graphEndings = Pair(SLASH, SLASH)

/** first: topLine, second: bottomLine */
var putInBoxes: Boxer? = null


enum class Boxer(val boxMethod: (String) -> String) {
  Minus({ "-".repeat(it.length + 4) + "\n- " + it + " -\n" + "-".repeat(it.length + 4) }),
  Addig({ "+" + "-".repeat(it.length + 2) + "+\n| " + it + " |\n+" + "-".repeat(it.length + 2) + "+" }),
  Extravagant({ "/" + "-".repeat(it.length + 2) + "\\\n| " + it + " |\n\\" + "-".repeat(it.length + 2) + "/" }),
  Comment({ "/" + "*".repeat(it.length + 4) + "\n * " + it + " *\n " + "*".repeat(it.length + 4) + "/" }),
  BigBox(
      {
        " " + "-".repeat(it.length + 4) + " \n" +
            "|" + " ".repeat(it.length + 4) + "|\n" +
            "|  " + it + "  |\n" +
            "|" + " ".repeat(it.length + 4) + "|\n" +
            " " + "-".repeat(it.length + 4)
      }),
  Art(
      {
        " ." + "~".repeat(it.length) + ". \n" +
            ": " + it + " :\n" +
            " `" + "~".repeat(it.length) + "´ "
      }),
}

class TextBlock {
  var width = 0
  var text = ""
  var topMiddle = 0

  constructor(str: String, topMiddle: Int) {
    init(str)
    this.topMiddle = topMiddle
  }

  constructor(str: String, side: Side) {
    init(str)
    topMiddle = getMiddle(side)
  }

  fun init(str: String) {
    width = str.lines().map { it.length }.max()!!
    text = str.lines().map { it toLength width }.joinToString("\n")
  }

  fun indent(i: Int) {
    init(text.indentBy(i))
    topMiddle += i
  }

  fun indentRight(i: Int) = init(text.indentRightBy(i))


  /**
   * 0123456   Odd?  Int:2  Left  Right
   * A       1  J      0      0     0
   * AB      2  N      1      1     0
   * ABC     3  J      1      1     1
   * ABCD    4  N      2      2     1
   * ABCDE   5  J      2      2     2
   * ABCDEF  6  N      3      3     2
   * ABCDEFG 7  J      3      3     3
   */
  fun getMiddle(side: Side) = if (width.isEven() && side == right) width / 2 - 1 else width / 2

  override fun toString() = text
}

fun <T> getAsTree(element: T, getName: (T) -> String, getChildren: (T) -> List<T>): String {
  fun buildTree(element: T, side: Side): TextBlock {
    val children = getChildren.invoke(element)
    val name = getName.invoke(element)
    val txt = putInBoxes?.boxMethod?.invoke(name) ?: name

    val thisBlock = TextBlock(txt, side)

    return when (children.size) {
      0 -> thisBlock
      1 -> {
        val childBlock = buildTree(children[0], side)

        val thisMiddle = thisBlock.getMiddle(side)
        val childMiddle = childBlock.topMiddle
        val diff = thisMiddle - childMiddle
        val targetMiddle = maxOf(thisMiddle, childMiddle)
        if (diff < 0) thisBlock.indent(-diff)
        if (diff > 0) childBlock.indent(diff)


        TextBlock(thisBlock.text + "\n" +
            " ".repeat(targetMiddle) + graphEndings.first.middle + "\n" +
            " ".repeat(targetMiddle) + graphEndings.second.middle + "\n" +
            childBlock.text,
            targetMiddle
        )
      }
      2 -> {
        val spacer = if (thisBlock.width.isOdd()) "   " else "    "

        val leftBlock = buildTree(children[0], left)
        val rightBlock = buildTree(children[1], right)

        if (leftBlock.width < 3) leftBlock.indentRight(1)
        if (rightBlock.width < 3) rightBlock.indent(1)

        val leftBlockLines = leftBlock.text.lines()
        val rightBlockLines = rightBlock.text.lines()

        val noOfLines = maxOf(leftBlockLines.size, rightBlockLines.size)

        var str = ""
        for (lineNr in 0..noOfLines - 1) {
          val leftPart = if (leftBlockLines.size > lineNr) leftBlockLines[lineNr] else " ".repeat(leftBlock.width)
          val rightPart = if (rightBlockLines.size > lineNr) rightBlockLines[lineNr] else ""

          str += leftPart + spacer + rightPart + "\n"
        }


        val middleOfOver = thisBlock.getMiddle(side)

        val lowLeft = leftBlock.topMiddle
        val lowRight = leftBlock.width + spacer.length + rightBlock.topMiddle
        val topLeft = leftBlock.width - 1
        val topRight = topLeft + spacer.length + 1
        val zwischenwert = lowLeft + 1 + (topLeft - 1 - lowLeft) + 1
        val indentForOver = if (thisBlock.width.isEven())
          (leftBlock.width + spacer.length / 2) - (thisBlock.width / 2)
        else
          leftBlock.width - thisBlock.width / 2

        thisBlock.indent(indentForOver)
        val over = " ".repeat(lowLeft + 1) + "_".repeat(topLeft - 1 - lowLeft) + graphEndings.first.left +
            " ".repeat(topRight - zwischenwert) + graphEndings.first.right + "_".repeat(lowRight - topRight - 1)
        val under = " ".repeat(lowLeft) + graphEndings.second.left + " ".repeat(lowRight - lowLeft - 1) + graphEndings.second.right

        TextBlock(
            thisBlock.text + "\n" +
                over + "\n" +
                under + "\n" +
                str,
            middleOfOver + indentForOver
        )
      }
      else -> throw IllegalStateException("mehr als 2 Kinder")
    }
  }

  return buildTree(element, left).toString()
}


fun main(args: Array<String>) {

  val expressions = listOf(
      Wurzel(Var("a")), Wurzel(Var("ab")), Wurzel(Var("abc")), Wurzel(Var("abcd")),
      -Var("a"), -Var("ab"), -Var("abc"), -Var("abcd"),
      -y,
      x - y,
      Wurzel(x),
      x + (x + x),
      x * -1231233,
      (x * -3) + (x * 4),
      Val(2) + x + y - z,
      (x + y) - z,
      x + x, Var("xyz") + Var("xyza"), Var("xyza") + Var("xyz"), Var("12345") + Var("123456"),
      x * y,
      Wurzel(x + 23456789),
      x + y - z + 1234567 + 123456 + 9876543 + 2345678 + (y * 43234567 + Wurzel(x + 234567))

  )

  expressions.forEach {
    for (i in 1..6) {
      graphEndings = when (i) {
        1 -> Pair(SLASH, SLASH)
        2 -> Pair(PIPE, PIPE)
        3 -> Pair(SLASH, PIPE)
        4 -> Pair(PIPE, SLASH)
        5 -> Pair(SLASH, V)
        else -> Pair(PIPE, V)
      }

      for (j in 0..6) {
        putInBoxes = when (j) {
          1 -> Minus
          2 -> Addig
          3 -> BigBox
          4 -> Comment
          5 -> Extravagant
          6 -> Art
          else -> null
        }

        println("= $graphEndings $putInBoxes=\n")
        println(getAsTree(it, { it.stringForGraph }, { it.children() }))
      }
    }
    println("\n----------------------\n----------------------\n----------------------\n")
  }

}
