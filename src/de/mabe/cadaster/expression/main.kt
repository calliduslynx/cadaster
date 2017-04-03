package de.mabe.cadaster.expression


// ***********************************************************************************************
// ***********************************************************************************************
// ***********************************************************************************************
fun main(args: Array<String>) {

  println("-------------------------------------")
  println("------------ GLEICHUNGEN ------------")
  gleichungen.forEach {
    println("-------------------------------------")
    println(it)
    println()
    println("              simple: " + it.simplify())
    println("      variable-count: " + it.variableCount())
    println("     with x=5 simple: " + it.withValue("x", 5).simplify())
    println("  with x=5 isCorrect: " + it.withValue("x", 5).isCorrect())
    println("         solve for x: " + it.solveFor("x"))
    println("  solve for x simple: " + it.solveFor("x").simplify())
  }

  println()
  println("-------------------------------------")
  println("------------ EXPRESSIONS ------------")
  list.forEach {
    println("-------------------------------------")
    println(it)
    println()
    println("              value: " + it.getValue())
    println("             simple: " + it.simplify())
    println("      variable-count: " + it.variableCount())
    println("            with x=5: " + it.withValue("x", 5))
    println("     with x=5 simple: " + it.withValue("x", 5).simplify())
    val s1 = try {
      Gleichung(it, Val(0)).solveFor("x")
    } catch (e: Exception) {
      "NOT SOLVEABLE"
    }
    println("         solve for x: " + s1)
    val s2 = try {
      Gleichung(it, Val(0)).solveFor("x").simplify()
    } catch (e: Exception) {
      "NOT SOLVEABLE"
    }
    println("  solve for x simple: " + s2)
  }
}


val gleichungen = listOf(
    Gleichung(
        left = Val(12) + x,
        right = Val(13) * x
    ),
    Gleichung(
        left = (Val(12) + Val(23)) * Val(12),
        right = (Val(13) * x) - (Val(12) / Val(4))
    ),
    Gleichung(
        left = (Var("y") + Val(23)) * Val(12),
        right = (Val(13) * x) - (Val(12) / Val(4))
    )
)
val x2 = Var("x2")
val x1 = Var("x1")
val y2 = Var("y2")
val y1 = Var("y1")

val list = listOf(
    (Val(1.0) + Val(2.0)) - Val(1.5),
    (Val(1.0) + Val(2.0)) - x,
    (x + Var("y")) - ((x + Var("z")) + (x + Var("y"))),
    Wurzel(Hoch2(x2 - x1) + Hoch2(y2 - y1)),
    (x * 13) - (Val(12) / 4),
    x - (Val(12) / 4),
    x + x,
    -x,
    Neg(Neg(x)),
    Neg(Neg(x)) + 1,
    Kehrwert(Kehrwert(x)) + 1,
    Hoch2(Wurzel(x)) + 1,
    Wurzel(Hoch2(x)) + 1,
    (x * 2) + x,
    (x * 3) + (x * 4),
    (Val(3) * x) + (Val(3) * x),
    x * x,
    x + x,
    x * (Val(3) + 5),
    x * ((x + x - 3) * 0 )

)
