package de.mabe.cadaster.expression

import de.mabe.cadaster.expression.GleichungKorrect.*
import de.mabe.cadaster.util.getAsTree


sealed class UmstellungsErgebnis

class ErfolgreicheUmstellung(val gleichung: Gleichung) : UmstellungsErgebnis() {
  override fun toString() = "SOLVED: $gleichung"
}

class UmstellungNichtErfolgreich(val message: String) : UmstellungsErgebnis() {
  override fun toString() = "UNABLE TO SOLVE: $message"
}

class VariableNichtRelevant(val varName: String, val gleichung: Gleichung) : UmstellungsErgebnis() {
  override fun toString() = "SOLVED: Variable $varName not relevant. Simplified EineGleichung: $gleichung (${gleichung.isCorrect()})"
}

// ******************************************************************************************************************
// ******************************************************************************************************************
// ******************************************************************************************************************

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

// ******************************************************************************************************************
// ******************************************************************************************************************
// ******************************************************************************************************************

enum class GleichungKorrect { KORREKT, NICHT_KORREKT, MGLWEISE }

// ******************************************************************************************************************
// ******************************************************************************************************************
// ******************************************************************************************************************

//@formatter:off
fun G(list: List<Gleichung>) : Gleichung= GleichungsMenge(list)
fun G(left: Int       , gleichheit: Gleichheit, right: Expression) = G(Val(left), gleichheit, right)
fun G(left: Double    , gleichheit: Gleichheit, right: Expression) = G(Val(left), gleichheit, right)
fun G(left: Expression, gleichheit: Gleichheit, right: Int       ) = G(left, gleichheit, Val(right))
fun G(left: Expression, gleichheit: Gleichheit, right: Double    ) = G(left, gleichheit, Val(right))
fun G(left: Expression, gleichheit: Gleichheit, right: Expression) : Gleichung = EineGleichung(left, gleichheit, right)

fun G(left: Int       , gleichheit: String    , right: Expression) = G(Val(left), gleichheit, right)
fun G(left: Double    , gleichheit: String    , right: Expression) = G(Val(left), gleichheit, right)
fun G(left: Expression, gleichheit: String    , right: Int       ) = G(left, gleichheit, Val(right))
fun G(left: Expression, gleichheit: String    , right: Double    ) = G(left, gleichheit, Val(right))
fun G(left: Expression, gleichheit: String    , right: Expression) = G(left, Gleichheit.get(gleichheit), right)
//@formatter:on

// ******************************************************************************************************************

interface Gleichung {
  fun simplify(): Gleichung
  fun loese_auf_nach(variable: VariableExpression) = loese_auf_nach(variable.name)
  fun loese_auf_nach(varName: String): UmstellungsErgebnis
  fun isCorrect(): GleichungKorrect
  fun toGraph(): String
  val variables: Set<String>
}

// ******************************************************************************************************************

private class GleichungsMenge(val gleichungen: List<Gleichung>) : Gleichung {
  override val variables by lazy { gleichungen.fold(HashSet<String>(), { set, gleichung -> set.addAll(gleichung.variables); set }) }

  override fun simplify() = GleichungsMenge(gleichungen.map { it.simplify() })

  override fun loese_auf_nach(varName: String): UmstellungsErgebnis {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun isCorrect() = gleichungen.fold(KORREKT) { statusBisher, gleichung ->
    val statusNeu = gleichung.isCorrect()
    when {
      statusNeu == NICHT_KORREKT || statusBisher == NICHT_KORREKT -> NICHT_KORREKT
      statusNeu == MGLWEISE || statusBisher == MGLWEISE -> MGLWEISE
      else -> KORREKT
    }
  }

  override fun equals(other: Any?) = (other as? GleichungsMenge)?.gleichungen == gleichungen

  override fun toGraph() = "Graph von ${gleichungen.size} Gleichungen"

  override fun toString() = gleichungen.map { it.toString() }.joinToString("  &  ")
}

// ******************************************************************************************************************

private class EineGleichung(val left: Expression, val gleichheit: Gleichheit, val right: Expression) : Gleichung {
  override fun toString() = "$left $gleichheit $right"

  override fun simplify() = EineGleichung(left.simplify(), gleichheit, right.simplify())
  override val variables by lazy { left.variables + right.variables }

  fun withValue(varName: String, value: Int) = withValue(varName, value.toDouble())
  fun withValue(varName: String, value: Double) = withValue(varName, Val(value))
  fun withValue(varName: String, value: Expression) = EineGleichung(left.withValue(varName, value), gleichheit, right.withValue(varName, value))

  override fun isCorrect(): GleichungKorrect {
    val leftValue = (left.simplify() as? ValueExpression)?.value
    val rightValue = (right.simplify() as? ValueExpression)?.value
    return when {
      leftValue == null || rightValue == null -> MGLWEISE
      leftValue == rightValue -> KORREKT
      else -> NICHT_KORREKT
    }
  }

  override fun loese_auf_nach(varName: String): UmstellungsErgebnis {
    // **** schauen ob beides gleich ist
    if (left.simplify() == right.simplify()) return VariableNichtRelevant(varName, EineGleichung(left.simplify(), gleichheit, right.simplify()))

    // ***** bring var to left
    val leftContainsVar = left.containsVariable(varName)
    val rightContainsVar = right.containsVariable(varName)
    debug("loese_auf_nach: '$varName' links: $leftContainsVar rechts:$rightContainsVar")

    // var only on left side
    var tmpGleichheit = this.gleichheit
    var (tmpLeft, tmpRight) = when {
      !leftContainsVar && !rightContainsVar -> return VariableNichtRelevant(varName, EineGleichung(left, gleichheit, right))
      leftContainsVar && !rightContainsVar -> Pair(this.left.simplify(), this.right.simplify())
      !leftContainsVar && rightContainsVar -> {
        tmpGleichheit = tmpGleichheit.flip
        Pair(this.right.simplify(), this.left.simplify())
      }
      else /*leftContainsVar && rightContainsVar*/ -> Pair((left - right).simplify(), Val(0))
    }
    debug("umgestellt (simple): $tmpLeft $tmpGleichheit $tmpRight")


    if (!tmpLeft.variables.contains(varName)) {
      return VariableNichtRelevant(varName, EineGleichung(tmpLeft, gleichheit, tmpRight))
    }

    /** Pair( flip Zeichen | right Expression ) */
    var tmpRightList = listOf(Pair(false, tmpRight))
    try {
      while (tmpLeft !is VariableExpression) {
        val aufloesungsErgebnis = tmpLeft.loese_auf(varName)
        tmpLeft = aufloesungsErgebnis.newLeft

        val newTmpRightList = mutableListOf<Pair<Boolean, Expression>>()

        aufloesungsErgebnis.aenderungDerRechtenSeiteList.forEach { aenderungDerRechtenSeite ->
          tmpRightList.forEach { (prevFlip, prevRight) ->
            newTmpRightList.add(
                Pair(
                    if (aenderungDerRechtenSeite.flipGleichheit) !prevFlip else prevFlip,
                    aenderungDerRechtenSeite.rightSideManipulationMethod.invoke(prevRight)
                )
            )
          }
        }

        tmpRightList = newTmpRightList.distinct()
      }

      return when (tmpRightList.size) {
        1 -> ErfolgreicheUmstellung(EineGleichung(tmpLeft, if (tmpRightList[0].first) tmpGleichheit.flip else tmpGleichheit, tmpRightList[0].second.simplify()))
        else -> ErfolgreicheUmstellung(GleichungsMenge(
            tmpRightList.map {
              EineGleichung(tmpLeft,
                  if (it.first) tmpGleichheit.flip else tmpGleichheit,
                  it.second.simplify())
            }
        ))
      }
    } catch (e: Exception) {
      return UmstellungNichtErfolgreich(e.message ?: "Keine Nachricht")
    }
  }

  override fun toGraph() = getAsTree(this as Any,
      {
        when (it) {
          is EineGleichung -> gleichheit.toString()
          is Expression -> it.stringForGraph
          else -> throw IllegalStateException()
        }
      },
      {
        when (it) {
          is EineGleichung -> listOf(left, right)
          is Expression -> it.children
          else -> throw IllegalStateException()
        }
      }
  )

  override fun equals(other: Any?): Boolean {
    if (other == null) return false
    if (other !is EineGleichung) return false
    if (javaClass != other.javaClass) return false

    if (left == other.left && gleichheit == other.gleichheit && right == other.right) return true
    if (left == other.right && gleichheit == other.gleichheit.flip && right == other.left) return true

    return false
  }
}
