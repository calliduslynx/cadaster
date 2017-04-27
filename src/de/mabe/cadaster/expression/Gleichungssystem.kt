package de.mabe.cadaster.expression

import de.mabe.cadaster.expression.Gleichheit.IST_GLEICH
import de.mabe.cadaster.util.indentBy


class Gleichungssystem(vararg gleichungen: Gleichung) {

  val gleichungen = gleichungen.toMutableList()

  fun solve() {
    println(this)

    val vc_initial = gleichungen.fold(VariableCount(), { vc, gl -> gl.variableCount(vc) })
    println(" > var-counts: " + vc_initial)

    gleichungen.forEach { it.simplify() }

    val gleichungenAusWertebereich: List<Gleichung> = gleichungen.map { it.left.wertebereiche() + it.right.wertebereiche() }.flatten()
    println(" > gleichungen aus Wertebereich: ${gleichungenAusWertebereich.size}\n" + gleichungenAusWertebereich.joinToString("\n").indentBy(10))

    gleichungen += gleichungenAusWertebereich

    val gleichungenProVariable: Map<String, List<Gleichung>> = vc_initial.map { vc ->
      val varName = vc.key
      Pair(
          varName,
          gleichungen.map {
            val solveResult = it.loese_auf_nach(varName)
            when (solveResult) {
              is ErfolgreicheUmstellung -> solveResult.gleichung
              is VariableNichtRelevant -> {
                println("  .. Variable '$varName' not relevant : $it")
                null
              }
              is UmstellungNichtErfolgreich -> {
                println("  !! Unable to Solve for '$varName' : $it")
                null
              }
            }
          }.filterNotNull()
      )
    }.toMap()

    println(" > gleichungen pro variable:")
    gleichungenProVariable.forEach { varName, list ->
      println("   > Variable: " + varName)
      list.forEachIndexed { i, it -> println("    [$i] $it") }
    }

    println(" > Substitutionen")
    gleichungenProVariable.forEach { varName, list ->
      // ***** Durchlauf für eine Variable
      println("   > Substituiere für " + varName)

      list.filter { it.gleichheit == IST_GLEICH }
          .forEach { gleichungFuerAktuelleVar ->
            gleichungFuerAktuelleVar.simplify()
          

          }

    }
    val substitutionsGleichungenProVariable = gleichungenProVariable.map { e -> Pair(e.key, e.value.filter { it.gleichheit == IST_GLEICH }) }.toMap()


  }

  override fun toString() = gleichungen.mapIndexed { i, it -> "[$i] $it" }.joinToString("\n")
}

//fun main(args: Array<String>) {
//  println("> load data")
//  println("     using elements:\n" + project.elements().all().toListString())
//  val variables = project.elements().allVariables()
//  println("     using variables:\n" + variables.toListString())
//  println("     using rules:\n" + project.rules().all().toListString())
//
//  println("> loading gleichungen")
//  var gleichungen = project.rules().all().map { it.rule.gleichung() }
//  println("     using gleichungen:\n" + gleichungen.toListString())
//  println("> setze bekannte Variablen")
//  variables.filter { it.pValue.type == exactly }.forEach { pVar ->
//    gleichungen = gleichungen.map { gl -> gl.withValue(pVar.name, pVar.pValue.value) }
//  }
//  println("> simplify gleichungen")
//  gleichungen = gleichungen.map { it.simplify() }
//  println("     gleichungen:\n" + gleichungen.toListString())
//  println("     missing vars (Name, Anzahl):\n" + gleichungen.variableCount().toList().toListString())
//
//  // **************************
//
//  println(gleichungen.map { it.loese_auf_nach("P1.x") }.toListString())
//  val gl = Gleichung(gleichungen[0].right, gleichungen[1].right)
//  println(gl.simplify())
//  val gl2 = gl.loese_auf_nach("P1.x")
//  println(gl2)
//
//
//  // Substitution / Gleichsetzung
//
//
//  // TODO Wertebereich
//  //  -> Div durch null
//  //  -> Wurzel nicht los
//}



