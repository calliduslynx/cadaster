package de.mabe.cadaster.expression

import de.mabe.cadaster.expression.GleichungKorrect.*
import de.mabe.cadaster.util.getAsTree

// TODO Gleichungen bei MAL muss - * - = +  .... a*b > 0 ... a > 0 und b > 0 oder a < und b < 0

enum class GleichungKorrect { KORREKT, NICHT_KORREKT, MGLWEISE }

sealed class UmstellungsErgebnis

class ErfolgreicheUmstellung(val gleichung: Gleichung) : UmstellungsErgebnis() {
  override fun toString() = "SOLVED: $gleichung"
}

class UmstellungNichtErfolgreich(val message: String) : UmstellungsErgebnis() {
  override fun toString() = "UNABLE TO SOLVE: $message"
}

class VariableNichtRelevant(val varName: String, val gleichung: Gleichung) : UmstellungsErgebnis() {
  override fun toString() = "SOLVED: Variable $varName not relevant. Simplified Gleichung: $gleichung (${gleichung.isCorrect()})"
}

enum class Gleichheit(val look: String, flip: () -> Gleichheit) {
  IST_GLEICH("=", { IST_GLEICH }),
  IST_UNGLEICH("<>", { IST_UNGLEICH }),
  IST_KLEINER("<", { IST_GROESSER }),
  IST_GROESSER(">", { IST_KLEINER }),
  IST_KLEINER_GLEICH("<=", { IST_GROESSER_GLEICH }),
  IST_GROESSER_GLEICH(">=", { IST_KLEINER_GLEICH });

  val flip by lazy { flip.invoke() }
  override fun toString() = look

  companion object {
    fun get(str: String) = Gleichheit.values().find { it.look == str } ?: throw IllegalStateException("keine Gleichheit für '$str'")
  }
}


//@formatter:off
fun G(left: Int       , gleichheit: Gleichheit, right: Expression) = G(Val(left), gleichheit, right)
fun G(left: Double    , gleichheit: Gleichheit, right: Expression) = G(Val(left), gleichheit, right)
fun G(left: Expression, gleichheit: Gleichheit, right: Int       ) = G(left, gleichheit, Val(right))
fun G(left: Expression, gleichheit: Gleichheit, right: Double    ) = G(left, gleichheit, Val(right))
fun G(left: Expression, gleichheit: Gleichheit, right: Expression) = Gleichung(left, gleichheit, right)

fun G(left: Int       , gleichheit: String    , right: Expression) = G(Val(left), gleichheit, right)
fun G(left: Double    , gleichheit: String    , right: Expression) = G(Val(left), gleichheit, right)
fun G(left: Expression, gleichheit: String    , right: Int       ) = G(left, gleichheit, Val(right))
fun G(left: Expression, gleichheit: String    , right: Double    ) = G(left, gleichheit, Val(right))
fun G(left: Expression, gleichheit: String    , right: Expression) = G(left, Gleichheit.get(gleichheit), right)
//@formatter:on


class Gleichung(val left: Expression, val gleichheit: Gleichheit, val right: Expression) {
  override fun toString() = "$left $gleichheit $right"

  fun simplify() = Gleichung(left.simplify(), gleichheit, right.simplify())
  val variables by lazy { left.variables + right.variables }

  fun withValue(varName: String, value: Int) = withValue(varName, value.toDouble())
  fun withValue(varName: String, value: Double) = withValue(varName, Val(value))
  fun withValue(varName: String, value: Expression) = Gleichung(left.withValue(varName, value), gleichheit, right.withValue(varName, value))

  fun isCorrect(): GleichungKorrect {
    val leftValue = (left.simplify() as? ValueExpression)?.value
    val rightValue = (right.simplify() as? ValueExpression)?.value
    return when {
      leftValue == null || rightValue == null -> MGLWEISE
      leftValue == rightValue -> KORREKT
      else -> NICHT_KORREKT
    }
  }

  fun loese_auf_nach(variable: VariableExpression) = loese_auf_nach(variable.name)
  fun loese_auf_nach(varName: String): UmstellungsErgebnis {
    // **** schauen ob beides gleich ist
    if (left.simplify() == right.simplify()) return VariableNichtRelevant(varName, Gleichung(left.simplify(), gleichheit, right.simplify()))

    // ***** bring var to left
    val leftContainsVar = left.containsVariable(varName)
    val rightContainsVar = right.containsVariable(varName)
    debug("loese_auf_nach: '$varName' links: $leftContainsVar rechts:$rightContainsVar")

    // var only on left side
    var tmpGleichheit = this.gleichheit
    var (tmpLeft, tmpRight) = when {
      !leftContainsVar && !rightContainsVar -> return VariableNichtRelevant(varName, Gleichung(left, gleichheit, right))
      leftContainsVar && !rightContainsVar -> Pair(this.left.simplify(), this.right.simplify())
      !leftContainsVar && rightContainsVar -> {
        tmpGleichheit = tmpGleichheit.flip
        Pair(this.right.simplify(), this.left.simplify())
      }
      else /*leftContainsVar && rightContainsVar*/ -> Pair((left - right).simplify(), Val(0))
    }
    debug("umgestellt (simple): $tmpLeft $tmpGleichheit $tmpRight")


    if (!tmpLeft.variables.contains(varName)) {
      return VariableNichtRelevant(varName, Gleichung(tmpLeft, gleichheit, tmpRight))
    }

    try {
      while (tmpLeft !is VariableExpression) {
        val aufloesungsErgebnis = tmpLeft.loese_auf(varName)
        tmpLeft = aufloesungsErgebnis.newLeft
        tmpRight = aufloesungsErgebnis.aenderungDerRechtenSeiteList[0].rightSideManipulationMethod.invoke(tmpRight)
        if (aufloesungsErgebnis.aenderungDerRechtenSeiteList[0].flipGleichheit) tmpGleichheit = tmpGleichheit.flip
        if (aufloesungsErgebnis.aenderungDerRechtenSeiteList.size > 1) println("ERROR --> zweites Ergebnis ignoriert")
      }
      tmpRight = tmpRight.simplify()
      return ErfolgreicheUmstellung(Gleichung(tmpLeft, tmpGleichheit, tmpRight))
    } catch (e: Exception) {
      return UmstellungNichtErfolgreich(e.message ?: "Keine Nachricht")
    }
  }

  fun toGraph() = getAsTree(this as Any,
      {
        when (it) {
          is Gleichung -> gleichheit.toString()
          is Expression -> it.stringForGraph
          else -> throw IllegalStateException()
        }
      },
      {
        when (it) {
          is Gleichung -> listOf(left, right)
          is Expression -> it.children
          else -> throw IllegalStateException()
        }
      }
  )

  override fun equals(other: Any?): Boolean {
    if (other == null) return false
    if (other !is Gleichung) return false
    if (javaClass != other.javaClass) return false

    if (left == other.left && gleichheit == other.gleichheit && right == other.right) return true
    if (left == other.right && gleichheit == other.gleichheit.flip && right == other.left) return true

    return false
  }
}
