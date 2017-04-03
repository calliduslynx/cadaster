package de.mabe.cadaster.expression

import java.util.concurrent.atomic.AtomicInteger


internal fun String.braced() = "( $this )"

// ***********************************************************************************************
// ***********************************************************************************************
// ***********************************************************************************************
// ***** shortings @formatter:off

val x = Var("x")
val y = Var("y")
val z = Var("z")

fun      Var( name: String)                       = VariableExpression(name)
fun      Val(value: Double)                       = ValueExpression(value)
fun      Val(value: Int)                          = ValueExpression(value.toDouble())
fun   Wurzel( exp1: Expression)                   = WurzelExpression(exp1)
fun    Hoch2( exp1: Expression)                   = QuadratExpression(exp1)

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
  fun toGraph(): String {
    operator fun String.times(x: Int): String {
      var res = ""
      for (i in 1..x) {
        res += this
      }
      if (res == "") return ""
      return res.take(res.length - 1) + "->"
    }

    fun printExpr(sb: StringBuilder, exp: Expression, indent: Int) {
      sb.append("  " + "| " * indent + exp.stringForGraph + "\n")
      exp.children().forEach { printExpr(sb, it, indent + 1) }
    }

    val sb = StringBuilder()
    printExpr(sb, this, 0)
    return sb.toString()
  }

  /** returns a new instance with a simplified expression */
  abstract fun simplify(): Expression

  fun withValue(varName: String, value: Int) = withValue(varName, value.toDouble())

  fun withValue(varName: String, value: Double): Expression {
    if (this is VariableExpression && this.name == varName)
      return ValueExpression(value)
    else
      return newInstance(children().map { it.withValue(varName, value) })
  }

  fun variableCount(variableCount: VariableCount = VariableCount()): VariableCount {
    traverseTree { if (it is VariableExpression) variableCount.getOrPut(it.name, { AtomicInteger() }).incrementAndGet() }
    return variableCount
  }


  /** entfernt die aktuelle Expression, simuliert ein Shiften auf die andere Seite */
  abstract fun shiftOver(varName: String, right: Expression): Pair<Expression, Expression>

  /** Order: top, left, right */
  private fun traverseTree(apply: (Expression) -> Unit) {
    apply(this)
    children().forEach { it.traverseTree(apply) }
  }

  override fun equals(other: Any?): Boolean {
    return other != null &&
        other is Expression &&
        other.javaClass == javaClass &&
        children().size == other.children().size &&
        children().mapIndexed { index, child -> child == other.children()[index] }.all { it }
  }
}

class VariableExpression(val name: String) : Expression("VARIABLE: $name") {
  override fun toString() = name
  override fun children() = emptyList<Expression>()
  override fun newInstance(children: List<Expression>) = VariableExpression(name)
  override fun shiftOver(varName: String, right: Expression) = TODO()
  override fun simplify() = this
}

class ValueExpression(val value: Double) : Expression("VALUE: $value") {
  override fun toString() = value.toString()
  override fun children() = emptyList<Expression>()
  override fun newInstance(children: List<Expression>) = ValueExpression(value)
  override fun shiftOver(varName: String, right: Expression) = throw VariableNotFoundException(varName)
  override fun simplify() = this
}

// ***********************************************************************************************

abstract class SingleFieldExpression(stringForGraph: String, val exp1: Expression) : Expression(stringForGraph) {
  override fun children() = listOf(exp1)

  override fun shiftOver(varName: String, right: Expression): Pair<Expression, Expression> {
    if (!exp1.containsVariable(varName)) throw VariableNotFoundException(varName)
    return Pair(exp1, invertOperation(right))
  }

  protected abstract fun invertOperation(exp: Expression): Expression
}


abstract class TwoFieldExpression(stringForGraph: String, val exp1: Expression, val exp2: Expression) : Expression(stringForGraph) {
  override fun children() = listOf(exp1, exp2)
  override fun shiftOver(varName: String, right: Expression): Pair<Expression, Expression> {
    val exp1ContainsVariable = exp1.containsVariable(varName)
    val exp2ContainsVariable = exp2.containsVariable(varName)
    return when {
      exp1ContainsVariable && !exp2ContainsVariable -> Pair(exp1, invertOperation(right, exp2))
      !exp1ContainsVariable && exp2ContainsVariable -> Pair(exp2, invertOperation(right, exp1))
      exp1ContainsVariable && exp2ContainsVariable -> throw RuntimeException("Variable $varName existiert rechts und links im Baum")
      else -> throw VariableNotFoundException(varName)
    }
  }

  protected abstract fun invertOperation(right: Expression, otherExp: Expression): Expression
  protected abstract fun invertOperationVarInBothSides(otherExp: Expression): Expression
}

// ***********************************************************************************************
// ***********************************************************************************************
// ***********************************************************************************************

open class WurzelExpression(exp1: Expression) : SingleFieldExpression("WURZEL", exp1) {
  override fun toString() = "WURZEL $exp1".braced()
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
}

open class QuadratExpression(exp1: Expression) : SingleFieldExpression("QUADRAT", exp1) {
  override fun toString() = "$exp1 ^ 2".braced()
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
}

// ***********************************************************************************************


class NegExpression(exp1: Expression) : SingleFieldExpression("NEG", exp1) {
  override fun toString() = "-$exp1"
  override fun newInstance(children: List<Expression>) = NegExpression(children[0])
  override fun simplify(): Expression {
    val simpleChild = exp1.simplify()
    return when (simpleChild) {
      is ValueExpression -> ValueExpression(-simpleChild.value)
      is NegExpression -> simpleChild.exp1
      else -> NegExpression(exp1.simplify())
    }
  }

  override fun invertOperation(exp: Expression) = NegExpression(exp)
}

class KehrwertExpression(exp1: Expression) : SingleFieldExpression("KEHRWERT", exp1) {
  override fun toString() = "1/$exp1".braced()
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
}

class PlusExpression(exp1: Expression, exp2: Expression) : TwoFieldExpression("PLUS", exp1, exp2) {
  override fun toString() = "$exp1 + $exp2".braced()
  override fun newInstance(children: List<Expression>) = PlusExpression(children[0], children[1])
  override fun simplify(): Expression {
    val simpleChild1 = exp1.simplify()
    val simpleChild2 = exp2.simplify()
    return when {
      simpleChild1 is ValueExpression && simpleChild2 is ValueExpression -> Val(simpleChild1.value + simpleChild2.value)
      simpleChild1 is ValueExpression && simpleChild1.value == 0.0 -> simpleChild2
      simpleChild2 is ValueExpression && simpleChild2.value == 0.0 -> simpleChild1
      simpleChild1 is NegExpression && simpleChild1.exp1 == simpleChild2 -> Val(0)
      simpleChild2 is NegExpression && simpleChild2.exp1 == simpleChild1 -> Val(0)
      else -> PlusExpression(simpleChild1, simpleChild2)
    }
  }

  override fun invertOperation(right: Expression, otherExp: Expression) = right - otherExp
  override fun invertOperationVarInBothSides(otherExp: Expression): Expression = TODO()
}


class MalExpression(exp1: Expression, exp2: Expression) : TwoFieldExpression("MAL", exp1, exp2) {
  override fun toString() = "$exp1 * $exp2".braced()
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
      simpleChild2 is KehrwertExpression && simpleChild2.exp1 == simpleChild1 -> Val(1)
      else -> MalExpression(simpleChild1, simpleChild2)
    }
  }

  override fun invertOperation(right: Expression, otherExp: Expression) = right / otherExp
  override fun invertOperationVarInBothSides(otherExp: Expression): Expression = TODO()
}


