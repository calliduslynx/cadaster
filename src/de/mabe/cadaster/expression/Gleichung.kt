package de.mabe.cadaster.expression

import de.mabe.cadaster.expression.GleichungKorrect.*

enum class GleichungKorrect { KORREKT, NICHT_KORREKT, MGLWEISE }

class NotSolvableGleichung(val varName: String, val reason: String) : Gleichung(VariableExpression(varName), VariableExpression(varName)) {
  override fun toString() = "Gleichung kann nicht nach $varName aufgelöst werden: $reason"
  override fun simplify() = this
  override fun withValue(varName: String, value: Int) = this
  override fun withValue(varName: String, value: Double) = this
  override fun solveFor(varName: String) = this
  override fun isCorrect() = NICHT_KORREKT
}

open class Gleichung(val left: Expression, val right: Expression) {
  override fun toString() = "$left = $right"
  open fun simplify() = Gleichung(left.simplify(), right.simplify())
  open fun variableCount(): VariableCount = left.variableCount(right.variableCount())
  open fun withValue(varName: String, value: Int) = withValue(varName, value.toDouble())
  open fun withValue(varName: String, value: Double) = Gleichung(left.withValue(varName, value), right.withValue(varName, value))
  open fun isCorrect(): GleichungKorrect {
    val leftResult = left.getValue()
    val rightResult = right.getValue()
    if (leftResult is MissingVariableResult || rightResult is MissingVariableResult) return MGLWEISE
    return if ((leftResult as ConcreteResult).value == (rightResult as ConcreteResult).value) KORREKT else NICHT_KORREKT
  }

  open fun solveFor(varName: String): Gleichung {
    if (variableCount()[varName]?.get() ?: 0 > 1){
      return NotSolvableGleichung(varName, "Die Variable kommt mehrfach vor!")
    }

    // ***** bring var to left --- make deep copy
    var tmpLeft = (if (this.left.containsVariable(varName)) this.left else this.right).copy()
    var tmpRight = (if (this.left.containsVariable(varName)) this.right else this.left).copy()

    while (tmpLeft !is VariableExpression) {
      val (newLeft, newRight) = tmpLeft.shiftOver(varName, tmpRight)
      tmpLeft = newLeft
      tmpRight = newRight
    }

    return Gleichung(tmpLeft, tmpRight)
  }
}
