package de.mabe.cadaster.expression

import de.mabe.cadaster.expression.GleichungKorrect.*

enum class GleichungKorrect { KORREKT, NICHT_KORREKT, MGLWEISE }

sealed class SolveResult

class Solved(val gleichung: Gleichung) : SolveResult() {
  override fun toString() = "SOLVED: $gleichung"
}

class UnableToSolve(val message: String) : SolveResult() {
  override fun toString() = "UNABLE TO SOLVE: $message"
}

class VariableNotRelevant(val varName: String, val gleichung: Gleichung) : SolveResult() {
  override fun toString() = "SOLVED: Variable $varName not relevant. Simplified Gleichung: $gleichung (${gleichung.isCorrect()})"
}


class Gleichung(val left: Expression, val right: Expression) {
  override fun toString() = "$left = $right"
  fun simplify() = Gleichung(left.simplify(), right.simplify())
  fun variableCount(variableCount: VariableCount = VariableCount()): VariableCount = left.variableCount(right.variableCount(variableCount))
  fun withValue(varName: String, value: Int) = withValue(varName, value.toDouble())
  fun withValue(varName: String, value: Double) = Gleichung(left.withValue(varName, value), right.withValue(varName, value))
  fun isCorrect(): GleichungKorrect {
    val leftValue = (left.simplify() as? ValueExpression)?.value
    val rightValue = (right.simplify() as? ValueExpression)?.value
    return when {
      leftValue == null || rightValue == null -> MGLWEISE
      leftValue == rightValue -> KORREKT
      else -> NICHT_KORREKT
    }
  }


  fun solveFor(varName: String): SolveResult {
    // ***** bring var to left --- make deep copy
    val leftContainsVar = left.containsVariable(varName)
    val rightContainsVar = right.containsVariable(varName)

    // var only on left side
    var (tmpLeft, tmpRight) = when {
      !leftContainsVar && !rightContainsVar -> return VariableNotRelevant(varName, Gleichung(left, right))
      leftContainsVar && !rightContainsVar -> Pair(this.left.simplify(), this.right.simplify())
      !leftContainsVar && rightContainsVar -> Pair(this.right.simplify(), this.left.simplify())
      else /*leftContainsVar && rightContainsVar*/ -> Pair((left - right).simplify(), Val(0))
    }

    val varCount = tmpLeft.variableCount()[varName]?.get() ?: 0

    if (varCount == 0) {
      return VariableNotRelevant(varName, Gleichung(tmpLeft, tmpRight))
    }

    try {
      while (tmpLeft !is VariableExpression) {
        val (newLeft, newRight) = tmpLeft.shiftOver(varName, tmpRight)
        tmpLeft = newLeft
        tmpRight = newRight
      }
      tmpRight = tmpRight.simplify()
      return Solved(Gleichung(tmpLeft, tmpRight))
    } catch (e: Exception) {
      return UnableToSolve(e.message ?: "Keine Nachricht")
    }
  }
}
