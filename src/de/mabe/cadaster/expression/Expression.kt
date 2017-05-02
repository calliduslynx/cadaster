package de.mabe.cadaster.expression

import de.mabe.cadaster.util.getAsTree
import java.util.HashSet
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.set

typealias ignore = Unit
internal fun String.braced() = "( $this )"

// ***********************************************************************************************
// ***********************************************************************************************
// ***********************************************************************************************
// ***** shortings @formatter:off

fun      Var( name: String)                       = VariableExpression(name)
fun      Val(value: Double)                       = ValueExpression(value)
fun      Val(value: Int)                          = ValueExpression(value.toDouble())
fun   Wurzel( exp1: Expression)                   = WurzelExpression(exp1)
fun  Quadrat( exp1: Expression)                   = QuadratExpression(exp1)
fun  Quadrat(value: Double)                       = QuadratExpression(Val(value))
fun  Quadrat(value: Int)                          = QuadratExpression(Val(value))

fun      Neg( exp1: Expression)                   = NegExpression(exp1)
fun Kehrwert( exp1: Expression)                   = KehrwertExpression(exp1)
fun     Plus( exp1: Expression, exp2: Expression) = PlusExpression(exp1, exp2)
fun      Min( exp1: Expression, exp2: Expression) = PlusExpression(exp1, Neg(exp2))
fun      Mal( exp1: Expression, exp2: Expression) = MalExpression(exp1, exp2)
fun      Div( exp1: Expression, exp2: Expression) = MalExpression(exp1, Kehrwert(exp2))

operator fun Expression.plus (that: Expression): Expression = Plus(this, that)
operator fun Expression.plus (that: Double)    : Expression = Plus(this, Val(that))
operator fun Expression.plus (that: Int)       : Expression = Plus(this, Val(that))
operator fun Expression.minus(that: Expression): Expression =  Min(this, that)
operator fun Expression.minus(that: Double)    : Expression =  Min(this, Val(that))
operator fun Expression.minus(that: Int)       : Expression =  Min(this, Val(that))
operator fun Expression.times(that: Expression): Expression =  Mal(this, that)
operator fun Expression.times(that: Double)    : Expression =  Mal(this, Val(that))
operator fun Expression.times(that: Int)       : Expression =  Mal(this, Val(that))
operator fun Expression.div  (that: Expression): Expression =  Div(this, that)
operator fun Expression.div  (that: Double)    : Expression =  Div(this, Val(that))
operator fun Expression.div  (that: Int)       : Expression =  Div(this, Val(that))
operator fun Expression.unaryMinus()           : Expression =  Neg(this)
//@formatter:on


class VariableNotFoundException(varName: String) : RuntimeException("Die Variable '$varName' konnte nicht gefunden werden")

class AenderungDerRechtenSeite(
    val flipGleichheit: Boolean,
    val rightSideManipulationMethod: (Expression) -> Expression
)

class AufloesungsErgebnis(
    val newLeft: Expression,
    val aenderungDerRechtenSeiteList: List<AenderungDerRechtenSeite>
)


// ***********************************************************************************************
// ***********************************************************************************************
// ***********************************************************************************************
sealed class Expression(val stringForGraph: String) {
  /** creates a new instance of the current Expression using given children as new children */
  abstract fun newInstance(children: List<Expression>): Expression

  abstract val children: List<Expression>

  fun containsVariable(variable: String) = variables.contains(variable)
  val variables: HashSet<String>  by lazy {
    val variables: HashSet<String> = HashSet()
    traverseTree { if (it is VariableExpression) variables.add(it.name) }
    variables
  }

  /** returns this expression as a Graph */
  fun toGraph() = getAsTree(this, { it.stringForGraph }, { it.children })

  /** returns a new instance with a simplified expression */
  abstract fun simplify(): Expression                         // TODO boolean if it was already simplified

  fun withValue(varName: String, value: Int) = withValue(varName, value.toDouble())
  fun withValue(varName: String, value: Double) = withValue(varName, Val(value))
  fun withValue(varName: String, value: Expression): Expression {
    if (this is VariableExpression && this.name == varName)
      return value
    else
      return newInstance(children.map { it.withValue(varName, value) })
  }

  abstract fun loese_auf(varName: String): AufloesungsErgebnis

  /** Order: top, left, right */
  private fun traverseTree(apply: (Expression) -> Unit) {
    apply(this)
    children.forEach { it.traverseTree(apply) }
  }

  override fun toString(): String {
    var str = innerToString()
    str = str.replace("+ -", "- ")
    str = str.replace(".0 ", " ")
    if (str.startsWith("( ") && str.endsWith(" )"))
      str = str.substring(2, str.length - 2)
    return str
  }

  abstract fun innerToString(): String

  override fun equals(other: Any?): Boolean {
    if (other == null) return false
    if (other !is Expression) return false
    if (javaClass != other.javaClass) return false

    when (this) {
      is VariableExpression -> return this.name == (other as VariableExpression).name
      is ValueExpression -> {
        fun Double.round(digits: Int): Double {
          val multiplier = Math.pow(10.0, digits.toDouble())
          return Math.round(this * multiplier) / multiplier
        }

        val val1 = value.round(8)
        val val2 = (other as ValueExpression).value.round(8)

        return val1 == val2
      }
      else -> {
        val tC = children
        val oC = other.children

        if (tC.size != oC.size) return false

        if (tC.size == 1) {
          return tC[0] == oC[0]
        } else {
          return (tC[0] == oC[0] && tC[1] == oC[1]) ||
              (tC[0] == oC[1] && tC[1] == oC[0])
        }
      }
    }
  }
}

// ******************************************************************************************************************
// ******************************************************************************************************************
// ******************************************************************************************************************

class VariableExpression(val name: String) : Expression(name) {
  override val children = emptyList<Expression>()
  override fun innerToString() = name
  override fun newInstance(children: List<Expression>) = VariableExpression(name)
  override fun simplify() = this
  override fun loese_auf(varName: String) = throw IllegalStateException("Eine VariableExpression kann nicht aufgelöst werden")
}

// ******************************************************************************************************************
// ******************************************************************************************************************
// ******************************************************************************************************************


class ValueExpression(val value: Double) : Expression("$value") {
  override val children = emptyList<Expression>()
  override fun innerToString() = value.toString()
  override fun newInstance(children: List<Expression>) = ValueExpression(value)
  override fun simplify() = this
  override fun loese_auf(varName: String) = throw VariableNotFoundException(varName)
}

// ******************************************************************************************************************
// ******************************************************************************************************************
// ******************************************************************************************************************

abstract class SingleFieldExpression(stringForGraph: String, val exp1: Expression) : Expression(stringForGraph) {
  override val children = listOf(exp1)

  override fun loese_auf(varName: String): AufloesungsErgebnis {
    if (!exp1.containsVariable(varName)) throw VariableNotFoundException(varName)
    return AufloesungsErgebnis(
        newLeft = exp1,
        aenderungDerRechtenSeiteList = aenderungenDerRechtenSeiteBeimAufloesen()
    )
  }

  protected abstract fun aenderungenDerRechtenSeiteBeimAufloesen(): List<AenderungDerRechtenSeite>
}


// ******************************************************************************************************************
// ******************************************************************************************************************
// ******************************************************************************************************************

open class WurzelExpression(exp1: Expression) : SingleFieldExpression("WURZEL", exp1) {
  override fun innerToString() = "WURZEL ${exp1.innerToString()}".braced()
  override fun newInstance(children: List<Expression>) = WurzelExpression(children[0])
  override fun simplify(): Expression {
    val simpleChild = exp1.simplify()
    return when (simpleChild) {
      is ValueExpression -> ValueExpression(Math.sqrt(simpleChild.value))
      is QuadratExpression -> simpleChild.exp1
      else -> this
    }
  }

  override fun aenderungenDerRechtenSeiteBeimAufloesen() = listOf(
      AenderungDerRechtenSeite(false) { Quadrat(it) }
  )
}

// ******************************************************************************************************************
// ******************************************************************************************************************
// ******************************************************************************************************************

open class QuadratExpression(exp1: Expression) : SingleFieldExpression("QUADRAT", exp1) {
  override fun innerToString() = "${exp1.innerToString()} ^ 2".braced()
  override fun newInstance(children: List<Expression>) = QuadratExpression(children[0])
  override fun simplify(): Expression {
    val simpleChild = exp1.simplify()
    return when (simpleChild) {
      is ValueExpression -> ValueExpression(simpleChild.value * simpleChild.value)
      is WurzelExpression -> simpleChild.exp1
      else -> this
    }
  }

  override fun aenderungenDerRechtenSeiteBeimAufloesen() = listOf(
      AenderungDerRechtenSeite(false) { Wurzel(it) },
      AenderungDerRechtenSeite(true) { -Wurzel(it) }
  )
}

// ******************************************************************************************************************
// ******************************************************************************************************************
// ******************************************************************************************************************


class NegExpression(exp1: Expression) : SingleFieldExpression("NEG", exp1) {
  override fun innerToString() = "-${exp1.innerToString()}"
  override fun newInstance(children: List<Expression>) = NegExpression(children[0])
  override fun simplify(): Expression {
    val simpleChild = exp1.simplify()

    return when (simpleChild) {
      is ValueExpression -> ValueExpression(-simpleChild.value)
      is NegExpression -> simpleChild.exp1
      is MalExpression -> (-simpleChild.exp1 * simpleChild.exp2).simplify()
      is PlusExpression -> (-simpleChild.exp1 + -simpleChild.exp2).simplify()
      else -> NegExpression(simpleChild)
    }
  }

  override fun aenderungenDerRechtenSeiteBeimAufloesen() = listOf(
      AenderungDerRechtenSeite(true) { -it }
  )
}

// ******************************************************************************************************************
// ******************************************************************************************************************
// ******************************************************************************************************************

class KehrwertExpression(exp1: Expression) : SingleFieldExpression("KEHRWERT", exp1) {
  override fun innerToString() = "1/${exp1.innerToString()}".braced()
  override fun newInstance(children: List<Expression>) = KehrwertExpression(children[0])
  override fun simplify(): Expression {
    val simpleChild = exp1.simplify()
    return when (simpleChild) {
      is ValueExpression -> ValueExpression(1 / simpleChild.value)
      is KehrwertExpression -> simpleChild.exp1.simplify()
      else -> KehrwertExpression(exp1.simplify())
    }
  }

  override fun aenderungenDerRechtenSeiteBeimAufloesen() = listOf(
      AenderungDerRechtenSeite(false, { KehrwertExpression(it) })
  )
}

// ******************************************************************************************************************
// ******************************************************************************************************************
// ******************************************************************************************************************

infix fun <A, B> A.und(b: B) = Pair(this, b)


abstract class TwoFieldExpression(stringForGraph: String, val exp1: Expression, val exp2: Expression) : Expression(stringForGraph) {
  override val children = listOf(exp1, exp2)
  override fun loese_auf(varName: String): AufloesungsErgebnis {
    val (expToKeep, expToMoveToRight) = when (exp1.containsVariable(varName) und exp2.containsVariable(varName)) {
      true und false -> exp1 und exp2
      false und true -> exp2 und exp1
      true und true -> throw RuntimeException("Variable $varName existiert rechts und links im Baum")
      false und false -> throw VariableNotFoundException(varName)
      else -> throw RuntimeException("geht gar nicht")
    }

    return AufloesungsErgebnis(expToKeep, listOf(
        AenderungDerRechtenSeite(false, invertOperation(expToMoveToRight))
    ))
  }

  protected abstract fun invertOperation(expressionToMoveToRight: Expression): (Expression) -> Expression
}

// ******************************************************************************************************************
// ******************************************************************************************************************
// ******************************************************************************************************************

class PlusExpression(exp1: Expression, exp2: Expression) : TwoFieldExpression("PLUS", exp1, exp2) {
  override fun innerToString() = "${exp1.innerToString()} + ${exp2.innerToString()}".braced()
  override fun newInstance(children: List<Expression>) = PlusExpression(children[0], children[1])
  override fun simplify(): Expression = simplify(true)
  private fun simplify(useAssoziativ: Boolean): Expression {
    val simpleChild1 = exp1.simplify()
    val simpleChild2 = exp2.simplify()
    return when {
      simpleChild1 is ValueExpression && simpleChild2 is ValueExpression -> Val(simpleChild1.value + simpleChild2.value)
      simpleChild1 is ValueExpression && simpleChild1.value == 0.0 -> simpleChild2
      simpleChild2 is ValueExpression && simpleChild2.value == 0.0 -> simpleChild1
      simpleChild1 is NegExpression && simpleChild1.exp1 == simpleChild2 -> Val(0)
      simpleChild2 is NegExpression && simpleChild2.exp1 == simpleChild1 -> Val(0)
      else -> {
        if (useAssoziativ) {
          val expressions = wendeAssoziativGesetzAufPlusAn(this).expressions()
          var curr = expressions[0]
          expressions.forEachIndexed { i, exp -> if (i > 0) curr += exp }
          return if (curr is PlusExpression) curr.simplify(false) else curr.simplify()
        } else {
          simpleChild1 + simpleChild2
        }
      }
    }
  }

  override fun invertOperation(expressionToMoveToRight: Expression) = { it: Expression -> it - expressionToMoveToRight }
}

private val VALUES = "###VALS###"

private class PlusCounts {
  private val internal = HashMap<String, Double>()
  private var others = ArrayList<Expression>()

  fun increment(valName: String, value: Double, invert: Boolean = false) {
    val valueTo = if (invert) -value else value
    internal[valName] = internal.getOrPut(valName, { 0.0 }) + valueTo
  }

  fun addOther(exp: Expression) = others.add(exp)

  fun add(counts: PlusCounts): Unit {
    counts.others.forEach { addOther(it) }
    counts.internal.forEach { increment(it.key, it.value) }
  }

  fun expressions(): List<Expression> {
    val list = ArrayList<Expression>()
    list.addAll(others)
    internal.forEach { varName, value -> list.add(if (varName == VALUES) Val(value) else Var(varName) * value) }
    return list
  }

  fun invert(): PlusCounts {
    internal.forEach { varName, value -> internal[varName] = -value }
    others = ArrayList(others.map { -it })
    return this
  }
}

private fun wendeAssoziativGesetzAufPlusAn(exp: Expression): PlusCounts {
  val counts = PlusCounts()

  when (exp) {
    is ValueExpression -> counts.increment(VALUES, exp.value)
    is VariableExpression -> counts.increment(exp.name, 1.0)
    is NegExpression -> counts.add(wendeAssoziativGesetzAufPlusAn(exp.exp1).invert())
    is PlusExpression -> {
      counts.add(wendeAssoziativGesetzAufPlusAn(exp.exp1))
      counts.add(wendeAssoziativGesetzAufPlusAn(exp.exp2))
    }
    is MalExpression -> {
      var e1 = exp.exp1.simplify()
      var e2 = exp.exp2.simplify()
      val invert = (e1 is NegExpression) xor (e2 is NegExpression)

      if (e1 is NegExpression) e1 = e1.exp1
      if (e2 is NegExpression) e2 = e2.exp1

      if (e1 is VariableExpression && e2 is ValueExpression)
        counts.increment(e1.name, e2.value, invert)
      else if (e2 is VariableExpression && e1 is ValueExpression)
        counts.increment(e2.name, e1.value, invert)
      else
        counts.addOther(exp)
    }
    else -> counts.addOther(exp)
  }
  return counts
}

// ******************************************************************************************************************
// ******************************************************************************************************************
// ******************************************************************************************************************

class MalExpression(exp1: Expression, exp2: Expression) : TwoFieldExpression("MAL", exp1, exp2) {
  override fun innerToString() = "${exp1.innerToString()} * ${exp2.innerToString()}".braced()
  override fun newInstance(children: List<Expression>) = children[0] * children[1]
  override fun simplify(): Expression = simplify(true)
  private fun simplify(useAssoziativ: Boolean): Expression {
    val sc1 = exp1.simplify()
    val sc2 = exp2.simplify()
    return when {
      sc1 == sc2 -> Quadrat(sc1)
      sc1 is ValueExpression && sc2 is ValueExpression -> Val(sc1.value * sc2.value)
      sc1 is ValueExpression && sc1.value == 0.0 -> Val(0.0)
      sc2 is ValueExpression && sc2.value == 0.0 -> Val(0.0)
      sc1 is ValueExpression && sc1.value == 1.0 -> sc2
      sc2 is ValueExpression && sc2.value == 1.0 -> sc1
      sc1 is ValueExpression && sc1.value == -1.0 -> -sc2
      sc2 is ValueExpression && sc2.value == -1.0 -> -sc1
      sc2 is KehrwertExpression && sc2.exp1 == sc1 -> Val(1)
      sc1 is VariableExpression && sc2 is VariableExpression && sc1.name == sc2.name -> Quadrat(sc1)
      sc1 is ValueExpression && sc2 is VariableExpression -> sc1 * sc2
      sc1 is VariableExpression && sc2 is ValueExpression -> sc2 * sc1
      sc1 is PlusExpression -> ((sc1.exp1 * sc2) + (sc1.exp2 * sc2)).simplify()
      sc2 is PlusExpression -> ((sc1 * sc2.exp1) + (sc1 * sc2.exp2)).simplify()
      else -> {
        if (useAssoziativ) {
          val expressions = wendeAssoziativGesetzAufMalAn(this).expressions()
          var curr = expressions[0]
          expressions.forEachIndexed { i, exp -> if (i > 0) curr *= exp }
          return if (curr is MalExpression) curr else curr.simplify()
        } else {
          MalExpression(sc1, sc2).simplify(false)
        }
      }
    }
  }

  override fun invertOperation(expressionToMoveToRight: Expression) = { it: Expression -> it / expressionToMoveToRight }
}

private class MalCounts {
  private var inverted: Boolean = false
  private var value: Double = 1.0
  private val variableAndPotenz = HashMap<String, Int>()
  private var others = ArrayList<Expression>()


  fun addOther(exp: Expression) = others.add(exp)
  fun multiplyValue(value: Double) {
    this.value *= value
  }

  fun incVarPotenz(varName: String, value: Int = 1) {
    variableAndPotenz[varName] = variableAndPotenz.getOrPut(varName, { 0 }) + value
  }

  fun invert() {
    inverted = !inverted
  }

  fun expressions(): List<Expression> {
    val list = ArrayList<Expression>()
    if (value < 0) {
      invert()
      value = -value
    }
    if (value != 1.0) list.add(Val(value))

    list.addAll(others)

    variableAndPotenz.forEach { varName, potenz ->
      when (potenz) {
        0 -> ignore
        2 -> list.add(Quadrat(Var(varName)))
        else -> {
          val isNeg = potenz < 0
          val absPotenz = if (isNeg) -potenz else potenz

          var curr: Expression = Var(varName)
          repeat(absPotenz - 1) { curr *= Var(varName) }

          list.add(if (isNeg) Kehrwert(curr) else curr)
        }
      }
    }

    if (inverted) list[0] = Neg(list[0]).simplify()
    return list
  }

  fun flip() {
    value = 1 / value
    others = ArrayList(others.map { Kehrwert(it) })
    variableAndPotenz.forEach { varName, value -> variableAndPotenz[varName] = -value }
  }

  fun add(newCounts: MalCounts) {
    value *= newCounts.value
    others.addAll(newCounts.others)
    if (newCounts.inverted) invert()
    newCounts.variableAndPotenz.forEach { varName, value ->
      incVarPotenz(varName, value)
    }
  }
}

private fun wendeAssoziativGesetzAufMalAn(exp: Expression, counts: MalCounts = MalCounts()): MalCounts {
  when (exp) {
    is ValueExpression -> counts.multiplyValue(exp.value)
    is VariableExpression -> counts.incVarPotenz(exp.name)
    is NegExpression -> {
      counts.invert()
      wendeAssoziativGesetzAufMalAn(exp.exp1, counts)
    }
    is KehrwertExpression -> {
      val newCounts = wendeAssoziativGesetzAufMalAn(exp.exp1)
      newCounts.flip()
      counts.add(newCounts)
    }
    is MalExpression -> {
      wendeAssoziativGesetzAufMalAn(exp.exp1.simplify(), counts)
      wendeAssoziativGesetzAufMalAn(exp.exp2.simplify(), counts)
    }
    else -> counts.addOther(exp)
  }
  return counts
}


