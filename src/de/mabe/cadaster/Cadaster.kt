package de.mabe.cadaster


sealed class SolveResult

class UnableToSolve() : SolveResult()
class Solvede(vals: Map<String, Double>) : SolveResult()

private fun List<*>.toListString() = this.map { "          # $it" }.joinToString("\n")

class Cadaster {
  fun solve(project: Project): SolveResult {
//    println("> load data")
//    println("     using elements:\n" + project.elements().all().toListString())
//    val variables = project.elements().allVariables()
//    println("     using variables:\n" + variables.toListString())
//    println("     using rules:\n" + project.rules().all().toListString())
//
//    println("> loading gleichungen")
//    var gleichungen = project.rules().all().map { it.rule.gleichung() }
//    println("     using gleichungen:\n" + gleichungen.toListString())
//    println("> setze bekannte Variablen")
//    variables.filter { it.pValue.type == exactly }.forEach { pVar ->
//      gleichungen = gleichungen.map { gl -> gl.withValue(pVar.name, pVar.pValue.value) }
//    }
//    println("> simplify gleichungen")
//    gleichungen = gleichungen.map { it.simplify() }
//    println("     gleichungen:\n" + gleichungen.toListString())
//
//    // **************************
//
//    println(gleichungen.map { it.loese_auf_nach("P1.x") }.toListString())
//    val gl = EineGleichung(gleichungen[0].right, IST_GLEICH, gleichungen[1].right)
//    println(gl.simplify())
//    val gl2 = gl.loese_auf_nach("P1.x")
//    println(gl2)
//
//
//    // Substitution / Gleichsetzung
//
//
//    // TODO Wertebereich
//    //  -> Div durch null
//    //  -> Wurzel nicht los
    return UnableToSolve()
  }
}
