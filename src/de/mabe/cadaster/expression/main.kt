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
  }
}


val gleichungen = listOf(
    Gleichung(
        left = Val(12) + Var("x"),
        right = Val(13) * Var("x")
    ),
    Gleichung(
        left = (Val(12) + Val(23)) * Val(12),
        right = (Val(13) * Var("x")) - (Val(12) / Val(4))
    ),
    Gleichung(
        left = (Var("y") + Val(23)) * Val(12),
        right = (Val(13) * Var("x")) - (Val(12) / Val(4))
    )
)

val list = listOf(
    Min(
        Plus(
            Val(1.0),
            Val(2.0)
        ),
        Val(1.5)
    ),
    Min(
        Plus(
            Val(1.0),
            Val(2.0)
        ),
        Var("x")
    ),
    Min(
        Plus(
            Var("x"),
            Var("y")
        ),
        Plus(
            Plus(
                Var("x"),
                Var("z")
            ),
            Plus(
                Var("x"),
                Var("y")
            )
        )
    ),
    Wurzel(
        Plus(
            QuadratExpression(
                Min(
                    Var("X2"),
                    Var("X1")
                )
            ),
            QuadratExpression(
                Min(
                    Var("Y2"),
                    Var("Y1")
                )
            )
        )
    ),
    Wurzel(
        Hoch2(Var("X2") - Var("X1")) +
            Hoch2(Var("Y2") - Var("Y1"))
    ),
    (Val(13) * Var("x")) - (Val(12) / Val(4)),
    Var("x") - (Val(12) / Val(4)),
    Var("x") + Var("x")
)
