package de.mabe.cadaster.expression

import com.sun.javafx.fxml.expression.VariableExpression


internal fun String.braced() = " ( $this ) "

// ***********************************************************************************************
// ***********************************************************************************************
// ***********************************************************************************************
// ***** shortings

fun Var(name: String) = VariableExpression(name)
fun Val(value: Double) = ValueExpression(value)
fun Wurzel(exp1: Expression) = WurzelExpression(exp1)
fun Hoch2(exp1: Expression) = QuadratExpression(exp1)
operator fun Expression.plus(exp1: Expression): Expression = PlusExpression(this, exp1)
operator fun Expression.minus(exp1: Expression): Expression = MinusExpression(this, exp1)
operator fun Expression.times(exp1: Expression): Expression = MalExpression(this, exp1)
operator fun Expression.div(exp1: Expression): Expression = GeteiltExpression(this, exp1)

// ***********************************************************************************************
// ***********************************************************************************************
// ***********************************************************************************************
sealed class Expression(val expName: String, vararg children: Expression) {
  val children = mutableListOf(*children)

  abstract fun getValue(): Result
  abstract fun copy(): Expression

  fun containsVariable(variable: String): Boolean {
    return (this is VariableExpression && (this as de.mabe.cadaster.expression.VariableExpression).name == variable)
        || children.any { it.containsVariable(variable) }
  }

  /**
   * returns this expression as a Graph
   */
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
      sb.append("  " + "| " * indent + exp.expName + "\n")
      exp.children.forEach { printExpr(sb, it, indent + 1) }
    }

    val sb = StringBuilder()
    printExpr(sb, this, 0)
    return sb.toString()
  }

  fun simplify(): Expression {
    val result = getValue()
    return when (result) {
      is ConcreteResult -> result.asExpression()
      is MissingVariableResult -> {
        val copy = copy()
        for ((index, child) in copy.children.withIndex())
          copy.children[index] = child.simplify()
        return copy
      }
    }
  }
}

class VariableExpression(val name: String) : Expression("VARIABLE: $name") {
  override fun getValue() = MissingVariableResult(name)
  override fun toString() = " $name "
  override fun copy() = VariableExpression(name)
}

class ValueExpression(val value: Double) : Expression("VALUE: $value") {
  override fun getValue(): Result = ConcreteResult(value)
  override fun toString() = " $value "
  override fun copy() = ValueExpression(value)
}

// ***********************************************************************************************

abstract class SingleFieldExpression(name: String, val exp1: Expression) : Expression(name, exp1) {
  override fun getValue(): Result {
    val res1 = exp1.getValue()
    return if (res1 is ConcreteResult) ConcreteResult(calculate(res1.value)) else res1
  }

  protected abstract fun calculate(v1: Double): Double
}


abstract class TwoFieldExpression(name: String, val exp1: Expression, val exp2: Expression) : Expression(name, exp1, exp2) {
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
}

// ***********************************************************************************************
// ***********************************************************************************************
// ***********************************************************************************************

open class WurzelExpression(exp1: Expression) : SingleFieldExpression("WURZEL", exp1) {
  override fun calculate(v1: Double) = Math.sqrt(v1)
  override fun toString() = "WURZEL $exp1".braced()
  override fun copy() = WurzelExpression(exp1.copy())
}

open class QuadratExpression(exp1: Expression) : SingleFieldExpression("QUADRAT", exp1) {
  override fun calculate(v1: Double) = v1 * v1
  override fun toString() = "$exp1 ^ 2".braced()
  override fun copy() = QuadratExpression(exp1.copy())
}

// ***********************************************************************************************

class PlusExpression(exp1: Expression, exp2: Expression) : TwoFieldExpression("PLUS", exp1, exp2) {
  override fun calculate(v1: Double, v2: Double) = v1 + v2
  override fun toString() = "$exp1 + $exp2".braced()
  override fun copy() = PlusExpression(exp1.copy(), exp2.copy())
}

class MinusExpression(exp1: Expression, exp2: Expression) : TwoFieldExpression("MINUS", exp1, exp2) {
  override fun calculate(v1: Double, v2: Double) = v1 - v2
  override fun toString() = "$exp1 - $exp2".braced()
  override fun copy() = MinusExpression(exp1.copy(), exp2.copy())
}

class MalExpression(exp1: Expression, exp2: Expression) : TwoFieldExpression("MAL", exp1, exp2) {
  override fun calculate(v1: Double, v2: Double) = v1 * v2
  override fun toString() = "$exp1 * $exp2".braced()
  override fun copy() = MalExpression(exp1.copy(), exp2.copy())
}

class GeteiltExpression(exp1: Expression, exp2: Expression) : TwoFieldExpression("GETEILT", exp1, exp2) {
  override fun calculate(v1: Double, v2: Double) = v1 / v2
  override fun toString() = "$exp1 / $exp2".braced()
  override fun copy() = GeteiltExpression(exp1.copy(), exp2.copy())
}

