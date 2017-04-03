package de.mabe.cadaster.expression

import java.util.concurrent.atomic.AtomicInteger


internal fun String.braced() = "( $this )"

// ***********************************************************************************************
// ***********************************************************************************************
// ***********************************************************************************************
// ***** shortings

fun Var(name: String) = VariableExpression(name)
fun Val(value: Double) = ValueExpression(value)
fun Val(value: Int) = ValueExpression(value.toDouble())
fun Wurzel(exp1: Expression) = WurzelExpression(exp1)
fun Hoch2(exp1: Expression) = QuadratExpression(exp1)

fun Neg(exp1: Expression) = NegExpression(exp1)
fun Kehrwert(exp1: Expression) = KehrwertExpression(exp1)
fun Plus(exp1: Expression, exp2: Expression) = PlusExpression(exp1, exp2)
fun Min(exp1: Expression, exp2: Expression) = PlusExpression(exp1, Neg(exp2))
fun Mal(exp1: Expression, exp2: Expression) = MalExpression(exp1, exp2)
fun Div(exp1: Expression, exp2: Expression) = MalExpression(exp1, Kehrwert(exp2))

operator fun Expression.plus(that: Expression): Expression = Plus(this, that)
operator fun Expression.minus(that: Expression): Expression = Min(this, that)
operator fun Expression.times(that: Expression): Expression = Mal(this, that)
operator fun Expression.div(that: Expression): Expression = Div(this, that)




typealias VariableCount = HashMap<String, AtomicInteger>

class VariableNotFoundException(varName: String) : RuntimeException("Die Variable '$varName' konnte nicht gefunden werden")

// ***********************************************************************************************
// ***********************************************************************************************
// ***********************************************************************************************
sealed class Expression(val stringForGraph: String) {
  abstract fun getValue(): Result
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
  fun simplify(): Expression {
    val result = getValue()
    return when (result) {
      is ConcreteResult -> result.asExpression()
      is MissingVariableResult -> {
        val instanceToCheck = simplifedInstance()
        val newChildren = instanceToCheck.children().map { it.simplify() }
        return instanceToCheck.newInstance(newChildren)
      }
    }
  }

  protected open fun simplifedInstance() = this

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
}

class VariableExpression(val name: String) : Expression("VARIABLE: $name") {
  override fun getValue() = MissingVariableResult(name)
  override fun toString() = name
  override fun children() = emptyList<Expression>()
  override fun newInstance(children: List<Expression>) = VariableExpression(name)
  override fun shiftOver(varName: String, right: Expression) = TODO()
}

class ValueExpression(val value: Double) : Expression("VALUE: $value") {
  override fun getValue(): Result = ConcreteResult(value)
  override fun toString() = value.toString()
  override fun children() = emptyList<Expression>()
  override fun newInstance(children: List<Expression>) = ValueExpression(value)
  override fun shiftOver(varName: String, right: Expression) = throw VariableNotFoundException(varName)
}

// ***********************************************************************************************

abstract class SingleFieldExpression(stringForGraph: String, val exp1: Expression) : Expression(stringForGraph) {
  override fun getValue(): Result {
    val res1 = exp1.getValue()
    return if (res1 is ConcreteResult) ConcreteResult(calculate(res1.value)) else res1
  }

  protected abstract fun calculate(v1: Double): Double

  override fun children() = listOf(exp1)

  override fun shiftOver(varName: String, right: Expression): Pair<Expression, Expression> {
    if (!exp1.containsVariable(varName)) throw VariableNotFoundException(varName)
    return Pair(exp1, invertOperation(right))
  }

  protected abstract fun invertOperation(exp: Expression): Expression
}


abstract class TwoFieldExpression(stringForGraph: String, val exp1: Expression, val exp2: Expression) : Expression(stringForGraph) {
  override fun getValue(): Result {
    val res1 = exp1.getValue()
    val res2 = exp2.getValue()
    if (res1 is ConcreteResult && res2 is ConcreteResult) {
      return ConcreteResult(calculate(res1.value, res2.value))
    } else if (res1 is MissingVariableResult && res2 is MissingVariableResult) {
      return MissingVariableResult(res1, res2)
    } else if (res1 is MissingVariableResult) {
      return res1
    } else {
      return res2
    }
  }

  protected abstract fun calculate(v1: Double, v2: Double): Double

  override fun children() = listOf(exp1, exp2)

  override fun shiftOver(varName: String, right: Expression): Pair<Expression, Expression> {
    return when {
      exp1.containsVariable(varName) -> Pair(exp1, invertOperation(right, exp2))
      exp2.containsVariable(varName) -> Pair(exp2, invertOperation(right, exp1))
      else -> throw VariableNotFoundException(varName)
    }
  }

  protected abstract fun invertOperation(right: Expression, otherExp: Expression): Expression
}

// ***********************************************************************************************
// ***********************************************************************************************
// ***********************************************************************************************

open class WurzelExpression(exp1: Expression) : SingleFieldExpression("WURZEL", exp1) {
  override fun calculate(v1: Double) = Math.sqrt(v1)
  override fun toString() = "WURZEL $exp1".braced()
  override fun newInstance(children: List<Expression>) = WurzelExpression(children[0])
  override fun invertOperation(exp: Expression) = QuadratExpression(exp)
  override fun simplifedInstance() = if (exp1 is QuadratExpression) exp1.exp1 else this
}

open class QuadratExpression(exp1: Expression) : SingleFieldExpression("QUADRAT", exp1) {
  override fun calculate(v1: Double) = v1 * v1
  override fun toString() = "$exp1 ^ 2".braced()
  override fun newInstance(children: List<Expression>) = QuadratExpression(children[0])
  override fun invertOperation(exp: Expression) = WurzelExpression(exp)
  override fun simplifedInstance() = if (exp1 is WurzelExpression) exp1.exp1 else this
}

// ***********************************************************************************************

class PlusExpression(exp1: Expression, exp2: Expression) : TwoFieldExpression("PLUS", exp1, exp2) {
  override fun calculate(v1: Double, v2: Double) = v1 + v2
  override fun toString() = "$exp1 + $exp2".braced()
  override fun newInstance(children: List<Expression>) = PlusExpression(children[0], children[1])
  override fun invertOperation(right: Expression, otherExp: Expression) = right - otherExp
}

class NegExpression(exp1: Expression) : SingleFieldExpression("NEG", exp1) {
  override fun calculate(v1: Double) = -v1
  override fun toString() = "-$exp1"
  override fun newInstance(children: List<Expression>) = NegExpression(children[0])
  override fun invertOperation(exp: Expression) = NegExpression(exp)
  override fun simplifedInstance() = if (exp1 is NegExpression) exp1.exp1 else this
}

class MalExpression(exp1: Expression, exp2: Expression) : TwoFieldExpression("MAL", exp1, exp2) {
  override fun calculate(v1: Double, v2: Double) = v1 * v2
  override fun toString() = "$exp1 * $exp2".braced()
  override fun newInstance(children: List<Expression>) = MalExpression(children[0], children[1])
  override fun invertOperation(right: Expression, otherExp: Expression) = right / otherExp
}

class KehrwertExpression(exp1: Expression) : SingleFieldExpression("KEHRWERT", exp1) {
  override fun calculate(v1: Double) = 1 / v1
  override fun toString() = "1/$exp1".braced()
  override fun newInstance(children: List<Expression>) = KehrwertExpression(children[0])
  override fun invertOperation(exp: Expression) = KehrwertExpression(exp)
  override fun simplifedInstance() = if (exp1 is KehrwertExpression) exp1.exp1 else this
}

