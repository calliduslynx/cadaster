package de.mabe.cadaster.expression


private fun List<Gleichung>.print() = this.mapIndexed { i, it -> "  [$i] $it" }.joinToString("\n")

class GleichungssytemResult {
  val map = HashMap<String, Double>()

  val numberOfSolvedVariables: Int get() = map.size

  fun setVariable(varName: String, value: Double) = map.put(varName, value)
  fun contains(varName: String) = map.containsKey(varName)

  override fun toString() = map.toString()
}

class Gleichungssystem(vararg gleichungen: Gleichung) {
  val gleichungen = gleichungen.toList()
  val variablen: HashSet<String> = gleichungen.fold(HashSet<String>(), { set, gleichung -> set.addAll(gleichung.variables); set })

  /** nimmt die Gleichung und versucht sie umzustellen, für alle be*/
  private fun erzeugeGleichungenUmgestelltNachVar(gleichung: Gleichung) = variablen.map { varName ->
    val solveResult = gleichung.loese_auf_nach(varName)
    when (solveResult) {
      is ErfolgreicheUmstellung -> solveResult.gleichung
      is VariableNichtRelevant -> {
        debug("            .. Variable '$varName' not relevant : $gleichung")
        null
      }
      is UmstellungNichtErfolgreich -> {
        println("            !! Unable to Solve for '$varName' : $gleichung")
        null
      }
    }
  }.filterNotNull()

  /** Wenn die Gleichung fertig gelöst ist, wird Pair(VarName, Value) zurück geliefert, sonst null */
  private fun Gleichung.getLoesung() = if (this.left is VariableExpression && this.right is ValueExpression)
    Pair(this.left.name, this.right.value) else null

  fun solve(result: GleichungssytemResult = GleichungssytemResult()) {
    println(this)
    println("  Variablen: $variablen\n")


    println("> Vereinfache Gleichungen")
    val gleichungen = gleichungen.map { it.simplify() }
    println(gleichungen.print())

    println("> Umgestellte Gleichungen")
    val umgestellteGleichungen = gleichungen.map { erzeugeGleichungenUmgestelltNachVar(it) }.flatten().toMutableList()

    println("> Versuche einfache Umstellung")
    while (true) {
      val numberOfFoundVariables = result.numberOfSolvedVariables
      println(umgestellteGleichungen.print())

      println(">> identifiziere gelöse Variablen")
      umgestellteGleichungen.map { it.getLoesung() }.filterNotNull().forEach { result.setVariable(it.first, it.second) }
      println("  " + result)
      if (numberOfFoundVariables == result.numberOfSolvedVariables) break

      println(">> entferne unscharfte Gleichungen")
      umgestellteGleichungen.removeIf { result.contains((it.left as VariableExpression).name) }
      println(">> setze bekannte Variablen")
      umgestellteGleichungen.replaceAll { gleichung ->
        var gl = gleichung
        result.map.forEach { varName, value -> gl = gleichung.withValue(varName, value) }
        gl.simplify()
      }
    }

  }
//    val gleichungenAusWertebereich: List<Gleichung> = gleichungen.map { it.left.wertebereiche() + it.right.wertebereiche() }.flatten()
//    println(" > gleichungen aus Wertebereich: ${gleichungenAusWertebereich.size}\n" + gleichungenAusWertebereich.joinToString("\n").indentBy(10))
//
//
//    val alleBekanntenGleichungen = ArrayList<Gleichung>()
//    alleBekanntenGleichungen.addAll(gleichungen + gleichungenAusWertebereich)
//
//    val alleBekanntenGleichungenDieUmgestelltSind = ArrayList<Gleichung>()
//    alleBekanntenGleichungenDieUmgestelltSind.addAll(erzeugeGleichungenUmgestelltNachVar(alleBekanntenGleichungen))
//
//    alleBekanntenGleichungen.addAll(alleBekanntenGleichungenDieUmgestelltSind)
//
//    println(" > alle bekannten Gleichungen:")
//    alleBekanntenGleichungen.forEach { println("        $it") }
//
//    if (DEBUG) {
//      println(" > gleichungen umgestellt nach Variablen:")
//      alleBekanntenGleichungenDieUmgestelltSind.forEach { println("        $it") }
//    }
//
//
//    var umgestellteGleichungenIndex = 0
//    println(" > Substitutionen")
//    while (umgestellteGleichungenIndex < alleBekanntenGleichungenDieUmgestelltSind.size) {
//      val umgestellteGleichungZumEinsetzen = alleBekanntenGleichungenDieUmgestelltSind[umgestellteGleichungenIndex]
//      if (umgestellteGleichungZumEinsetzen.gleichheit == IST_GLEICH) {
//
//        debug("   > Substituiere " + umgestellteGleichungZumEinsetzen)
//
//        alleBekanntenGleichungen.forEach { bekannteGleichung ->
//          debug("     > in Gleichung " + bekannteGleichung)
//          val varName = (umgestellteGleichungZumEinsetzen.left as VariableExpression).name
//          val gleichungMitErsetzung = bekannteGleichung.withValue(varName, umgestellteGleichungZumEinsetzen.right)
//
//          debug("       > mit Ersetzung: " + gleichungMitErsetzung)
//
//          val neueUmgestellteGleichungen = erzeugeGleichungenUmgestelltNachVar(listOf(gleichungMitErsetzung))
//          neueUmgestellteGleichungen.forEach {
//            val neuSimple = it.simplify()
//            debug("         > umgestellt + simple: $neuSimple     ......")
//            if (alleBekanntenGleichungenDieUmgestelltSind.contains(neuSimple)) {
//              debug("                                          -> bekannt")
//            } else {
//              debug("                                          -> neu")
//              alleBekanntenGleichungenDieUmgestelltSind.add(neuSimple)
//            }
//          }
//        }
//      }
//      umgestellteGleichungenIndex++
//    }
//
//    println(" > Gleichungen nach Substitution")
//    alleBekanntenGleichungenDieUmgestelltSind.forEach { println("        $it") }
//
//    val variablenUndIhrWert = HashMap<String, Double>()
//    alleBekanntenGleichungenDieUmgestelltSind.forEach {
//      if (it.gleichheit == IST_GLEICH && it.right is ValueExpression) {
//        val varName = (it.left as VariableExpression).name
//        val value = it.right.value
//        if (variablenUndIhrWert.containsKey(varName) && variablenUndIhrWert[varName] != value)
//          throw IllegalStateException("VAR '$varName' ist mal '${variablenUndIhrWert[varName]}' und mal '$value'")
//
//        variablenUndIhrWert.put(varName, value)
//      }
//    }
//
//    println(" > Ermittelte konkrete Variablen:")
//    variablenUndIhrWert.forEach { varName, value -> println("        $varName: $value") }
//
//    // TODO noch nicht ermittelte Variablen anhand ihres Wertebereichs ermitteln
//    // immer nur eine neue Variable und dann wieder einsetzen
//
//    // TODO mit allen ermittelten Variablen alle Gleichungen testen
//}

  override fun toString() = gleichungen.print()
}



