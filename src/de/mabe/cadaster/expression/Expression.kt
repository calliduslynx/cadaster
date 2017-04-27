package de.mabe.cadaster.expression

import de.mabe.cadaster.expression.Gleichheit.IST_GROESSER_GLEICH
import de.mabe.cadaster.expression.Gleichheit.IST_UNGLEICH
import de.mabe.cadaster.util.getAsTree
import java.util.concurrent.atomic.AtomicInteger


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

typealias VariableCount = HashMap<String, AtomicInteger>

class VariableNotFoundException(varName: String) : RuntimeException("Die Variable '$varName' konnte nicht gefunden werden")

//@formatter:on
// ***********************************************************************************************
// ***********************************************************************************************
// ***********************************************************************************************
sealed class Expression(val stringForGraph: String) {
  /** creates a new instance of the current Expression using given children as new children */
  abstract fun newInstance(children: List<Expression>): Expression

  /** performs a deep copy */
  fun copy(): Expression = newInstance(children().map { it.copy() })

  abstract fun children(): List<Expression>

  fun containsVariable(variable: String): Boolean = (this is VariableExpression && this.name == variable) || children().any { it.containsVariable(variable) }

  /** returns this expression as a Graph */
  fun toGraph() = getAsTree(this, { it.stringForGraph }, { it.children() })

  /** returns a new instance with a simplified expression */
  abstract fun simplify(): Expression

  fun withValue(varName: String, value: Int) = withValue(varName, value.toDouble())
  fun withValue(varName: String, value: Double) = withValue(varName, Val(value))
  fun withValue(varName: String, value: Expression): Expression {
    if (this is VariableExpression && this.name == varName)
      return value
    else
      return newInstance(children().map { it.withValue(varName, value) })
  }

  fun variableCount(variableCount: VariableCount = VariableCount()): VariableCount {
    traverseTree { if (it is VariableExpression) variableCount.getOrPut(it.name, { AtomicInteger() }).incrementAndGet() }
    return variableCount
  }

  fun wertebereiche(): List<Gleichung> {
    val list = ArrayList<Gleichung?>()
    traverseTree { list.add(it.wertebereich()) }
    return list.filterNotNull()
  }

  open protected fun wertebereich(): Gleichung? = null

  /** entfernt die aktuelle Expression, simuliert ein Shiften auf die andere Seite */
  abstract fun shiftOver(varName: String, right: Expression): Triple<Expression, Expression, Boolean>

  /** Order: top, left, right */
  private fun traverseTree(apply: (Expression) -> Unit) {
    apply(this)
    children().forEach { it.traverseTree(apply) }
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

    if (this is VariableExpression) return this.name == (other as VariableExpression).name
    if (this is ValueExpression) return this.value == (other as ValueExpression).value

    val tC = children()
    val oC = other.children()

    if (tC.size != oC.size) return false

    if (tC.size == 1) {
      return tC[0] == oC[0]
    } else {
      return (tC[0] == oC[0] && tC[1] == oC[1]) ||
          (tC[0] == oC[1] && tC[1] == oC[0])
    }
  }
}

class VariableExpression(val name: String) : Expression(name) {
  override fun innerToString() = name
  override fun children() = emptyList<Expression>()
  override fun newInstance(children: List<Expression>) = VariableExpression(name)
  override fun shiftOver(varName: String, right: Expression) = TODO()
  override fun simplify() = this
}

class ValueExpression(val value: Double) : Expression("$value") {
  override fun innerToString() = value.toString()
  override fun children() = emptyList<Expression>()
  override fun newInstance(children: List<Expression>) = ValueExpression(value)
  override fun shiftOver(varName: String, right: Expression) = throw VariableNotFoundException(varName)
  override fun simplify() = this
}

// ***********************************************************************************************

abstract class SingleFieldExpression(stringForGraph: String, val exp1: Expression) : Expression(stringForGraph) {
  override fun children() = listOf(exp1)

  override fun shiftOver(varName: String, right: Expression): Triple<Expression, Expression, Boolean> {
    if (!exp1.containsVariable(varName)) throw VariableNotFoundException(varName)
    debug("> part to shift: $this .. inverted: " + invertOperation(right))
    return Triple(exp1, invertOperation(right), flip())
  }

  protected abstract fun flip(): Boolean
  protected abstract fun invertOperation(exp: Expression): Expression
}


abstract class TwoFieldExpression(stringForGraph: String, val exp1: Expression, val exp2: Expression) : Expression(stringForGraph) {
  override fun children() = listOf(exp1, exp2)
  override fun shiftOver(varName: String, right: Expression): Triple<Expression, Expression, Boolean> {
    val exp1ContainsVariable = exp1.containsVariable(varName)
    val exp2ContainsVariable = exp2.containsVariable(varName)
    return when {
      exp1ContainsVariable && !exp2ContainsVariable -> Triple(exp1, invertOperation(right, exp2), false)
      !exp1ContainsVariable && exp2ContainsVariable -> Triple(exp2, invertOperation(right, exp1), false)
      exp1ContainsVariable && exp2ContainsVariable -> throw RuntimeException("Variable $varName existiert rechts und links im Baum")
      else -> throw VariableNotFoundException(varName)
    }
  }

  protected abstract fun invertOperation(right: Expression, otherExp: Expression): Expression
}

// ***********************************************************************************************
// ***********************************************************************************************
// ***********************************************************************************************

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

  override fun invertOperation(exp: Expression) = QuadratExpression(exp)
  override fun flip() = false

  override fun wertebereich() = Gleichung(exp1, IST_GROESSER_GLEICH, Val(0))
}

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


  override fun invertOperation(exp: Expression) = WurzelExpression(exp)
  override fun flip() = false
}

// ***********************************************************************************************


class NegExpression(exp1: Expression) : SingleFieldExpression("NEG", exp1) {
  override fun innerToString() = "-${exp1.innerToString()}"
  override fun newInstance(children: List<Expression>) = NegExpression(children[0])
  override fun simplify(): Expression {
    val simpleChild = exp1.simplify()

    if (simpleChild is ValueExpression && simpleChild.value == 0.0) return Val(0) // FIXME: ... 

    return when (simpleChild) {
      is ValueExpression -> ValueExpression(-simpleChild.value)
      is NegExpression -> simpleChild.exp1
      else -> NegExpression(exp1.simplify())
    }
  }

  override fun invertOperation(exp: Expression) = NegExpression(exp)
  override fun flip() = true
}

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

  override fun invertOperation(exp: Expression) = KehrwertExpression(exp)
  override fun flip() = false

  override fun wertebereich() = Gleichung(exp1, IST_UNGLEICH, Val(0))
}

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
      else ->
        if (useAssoziativ) {
          // Assoziativ-Gesetz
          val variableCount = HashMap<String, Double>()
          fun HashMap<String, Double>.increment(valName: String, value: Double) {
            this[valName] = this.getOrPut(valName, { 0.0 }) + value
          }

          val otherExp = mutableListOf<Expression>()
          var sum = 0.0


          fun traverse(exp1: Expression, exp2: Expression? = null) {
            val isNeg = exp1 is NegExpression
            val e = if (isNeg) (exp1 as NegExpression).exp1 else exp1

            when (e) {
              is ValueExpression -> sum += if (isNeg) -e.value else e.value
              is VariableExpression -> variableCount.increment(e.name, if (isNeg) -1.0 else 1.0)
              is PlusExpression -> if (!isNeg) traverse((exp1 as PlusExpression).exp1, exp1.exp2) else otherExp.add(exp1)
              is MalExpression -> {
                if (e.exp1 is VariableExpression && e.exp2 is ValueExpression)
                  variableCount.increment(e.exp1.name, if (isNeg) -e.exp2.value else e.exp2.value)
                else if (e.exp2 is VariableExpression && e.exp1 is ValueExpression)
                  variableCount.increment(e.exp2.name, if (isNeg) -e.exp1.value else e.exp1.value)
                else
                  otherExp.add(exp1)
              }
              else -> otherExp.add(exp1)
            }
            if (exp2 != null) traverse(exp2)
          }

          traverse(this)

          var curr: Expression = Val(sum)
          variableCount.forEach { varName, count ->
            curr = curr + (Var(varName) * count)
          }
          otherExp.forEach { curr = curr + it }
          return if (curr is PlusExpression) (curr as PlusExpression).simplify(false) else curr.simplify()
        } else {
          Plus(simpleChild1, simpleChild2)
        }
    }
  }

  override fun invertOperation(right: Expression, otherExp: Expression) = right - otherExp
}


class MalExpression(exp1: Expression, exp2: Expression) : TwoFieldExpression("MAL", exp1, exp2) {
  override fun innerToString() = "${exp1.innerToString()} * ${exp2.innerToString()}".braced()
  override fun newInstance(children: List<Expression>) = children[0] * children[1]
  override fun simplify(): Expression {
    val simpleChild1 = exp1.simplify()
    val simpleChild2 = exp2.simplify()
    return when {
      simpleChild1 is ValueExpression && simpleChild2 is ValueExpression -> Val(simpleChild1.value * simpleChild2.value)
      simpleChild1 is ValueExpression && simpleChild1.value == 0.0 -> Val(0.0)
      simpleChild2 is ValueExpression && simpleChild2.value == 0.0 -> Val(0.0)
      simpleChild1 is ValueExpression && simpleChild1.value == 1.0 -> simpleChild2
      simpleChild2 is ValueExpression && simpleChild2.value == 1.0 -> simpleChild1
      simpleChild1 is ValueExpression && simpleChild1.value == -1.0 -> -simpleChild2
      simpleChild2 is ValueExpression && simpleChild2.value == -1.0 -> -simpleChild1
      simpleChild2 is KehrwertExpression && simpleChild2.exp1 == simpleChild1 -> Val(1)
      simpleChild1 is VariableExpression && simpleChild2 is VariableExpression && simpleChild1.name == simpleChild2.name -> Quadrat(simpleChild1)
      else -> MalExpression(simpleChild1, simpleChild2)
    }
  }

  override fun invertOperation(right: Expression, otherExp: Expression) = right / otherExp
}


