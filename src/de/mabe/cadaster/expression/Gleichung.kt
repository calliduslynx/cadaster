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

fun G(left: Expression, gleichheit: Gleichheit, right: Expression) = Gleichung(left, gleichheit, right)
fun G(left: Expression, gleichheit: String, right: Expression) = Gleichung(left, Gleichheit.get(gleichheit), right)

class Gleichung(val left: Expression, val gleichheit: Gleichheit, val right: Expression) {
  override fun toString() = "$left $gleichheit $right"
  fun simplify() = Gleichung(left.simplify(), gleichheit, right.simplify())
  fun variableCount(variableCount: VariableCount = VariableCount()): VariableCount = left.variableCount(right.variableCount(variableCount))
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

  fun loese_auf_nach(varName: String): UmstellungsErgebnis {
    // ***** bring var to left --- make deep copy
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

    val varCount = tmpLeft.variableCount()[varName]?.get() ?: 0

    if (varCount == 0) {
      return VariableNichtRelevant(varName, Gleichung(tmpLeft, gleichheit, tmpRight))
    }

    try {
      while (tmpLeft !is VariableExpression) {
        val (newLeft, newRight, flip) = tmpLeft.shiftOver(varName, tmpRight)
        debug(" shift over '$varName' : $tmpLeft $tmpGleichheit $tmpRight --> $newLeft $tmpGleichheit? $newRight flip: $flip")
        if (flip) tmpGleichheit = tmpGleichheit.flip
        tmpLeft = newLeft
        tmpRight = newRight
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
          is Expression -> it.children()
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
