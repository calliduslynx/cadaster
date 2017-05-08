package de.mabe.cadaster.expression

// ******************************************************************************************************************
// ******************************************************************************************************************
// ******************************************************************************************************************
private fun List<Gleichung>.print() = this.mapIndexed { i, it -> "  [$i] $it" }.joinToString("\n")

private fun List<Gleichung>.`enthalten mindestens eine gelöste, konkrete Variable`(): Boolean =
    this.any { it.getErgebnis()?.ist_konkret ?: false }

// ******************************************************************************************************************
// ******************************************************************************************************************
// ******************************************************************************************************************

open class GleichungssytemResult(val variables: HashSet<String>) {
  val foundVariables = HashMap<String, Pair<Boolean, List<Double>>>()
  val openVariables: List<String> get() = variables.filter { !foundVariables.keys.contains(it) }
  val fertig: Boolean get() = openVariables.isEmpty()

  fun setzeFestenWert(variable: String, values: List<Double>) {
    foundVariables.put(variable, Pair(true, values))
  }

  override fun toString() = "Fertig:$fertig  $foundVariables  - open: $openVariables"
}

object Gleichungssystem_nicht_gelöst : GleichungssytemResult(HashSet())

// ******************************************************************************************************************
// ******************************************************************************************************************
// ******************************************************************************************************************

class Gleichungssystem(vararg gleichungen: Gleichung) {
  val gleichungen = gleichungen.toList()
  val variablen: HashSet<String> = gleichungen.fold(HashSet<String>(), { set, gleichung -> set.addAll(gleichung.variables); set })

  /*********************************************************************************************************************
   *                                                                                                                   *
   *   M A I N - M E T H O D                                                                                           *
   *                                                                                                                   *
   *********************************************************************************************************************/
  fun solve(
      gleichungenParam: List<Gleichung> = this.gleichungen,
      result: GleichungssytemResult = GleichungssytemResult(this.variablen),
      depth: Int = 0
  ): GleichungssytemResult {
    fun l(a: Any, d: Int = depth): Unit = if (a.toString().lines().size > 1) a.toString().lines().forEach { l(it) } else println("| ".repeat(d) + a)
    val gleichungen = gleichungenParam
    val variablen: HashSet<String> = gleichungen.map { it.variables }.fold(HashSet<String>(), { set, v -> set.addAll(v);set })

    if (depth > 0) l("+-" + "-".repeat(20), depth - 1)

    l(gleichungen.print())
    l("> aktuelles Result: $result\n")
    if (result.fertig) return `logge Resultate`(result, this.gleichungen)

    l("> Vereinfache Gleichungen")
    val vereinfachte_gleichungen = gleichungen.map { it.simplify() }
    l(vereinfachte_gleichungen.print())

    l("> Stelle Gleichungen um")
    val (mindestens_eine_Umstellung_war_erfolgreich: Boolean, umgestellte_gleichungen: List<Gleichung>)
        = `erzeuge Gleichungen, umgestellt für jede Variable`(gleichungen, variablen)
    l(umgestellte_gleichungen.print())

    l("> Überrpüfe, ob mindestens ein Gleichung umgestellt werden konnte")
    if (!mindestens_eine_Umstellung_war_erfolgreich) {
      l("> ABBRUCH: Keine der Gleichungen kann umgestellt werden")
      return Gleichungssystem_nicht_gelöst
    }

    l("> Überpüfe, ob Variable bereits gelöst wurde")
    if (umgestellte_gleichungen.`enthalten mindestens eine gelöste, konkrete Variable`()) {
      l("> Es wurden gelöste Variablen gefunden, diese werden eingesetzt.")
      val eingesetzte_gleichungen = `nimm fertige Gleichungen und setze Variablen`(umgestellte_gleichungen, result)
      return solve(eingesetzte_gleichungen, result, depth + 1)
    }

    l("> Substituiere Variablen")

    TODO()
  }


  /*********************************************************************************************************************
   * nimmt die Gleichungen und versucht sie zu jeder bekannten Variable umzustellen                                    *
   * - wenn eine Gleichung nach allen bekannten Variablen umgestellt werden konnte, wird sie entfernt, andernfalls     *
   *   existiert sie auch in der Return-Liste                                                                          *
   * @return Boolean:     ob mindestens eine Gleichung erfolgreich umgestellt wurde                                    *
   *         Gleichungen: alle weiter zu verwendenden Gleichungen                                                      *
   *********************************************************************************************************************/
  private fun `erzeuge Gleichungen, umgestellt für jede Variable`(gleichungen: List<Gleichung>, variablen: HashSet<String>): Pair<Boolean, List<Gleichung>> {
    var mindestens_eine_Umstellung_war_erfolgreich = false

    val list = gleichungen.map { gleichung ->
      var gleichung_fuer_alle_Variablen_aufgeloest = true

      val umgestellte_Gleichungen = variablen.map { varName ->
        val solveResult = gleichung.loese_auf_nach(varName)
        when (solveResult) {
          is ErfolgreicheUmstellung -> {
            mindestens_eine_Umstellung_war_erfolgreich = true
            solveResult.gleichung
          }
          is VariableNichtRelevant -> {
            debug("            .. Variable '$varName' not relevant : $gleichung")
            null
          }
          is UmstellungNichtErfolgreich -> {
            println("            !! Unable to Solve for '$varName' : $gleichung")
            gleichung_fuer_alle_Variablen_aufgeloest = false
            null
          }
        }
      }.filterNotNull()

      if (gleichung_fuer_alle_Variablen_aufgeloest) umgestellte_Gleichungen else umgestellte_Gleichungen + gleichung
    }.flatten()

    return Pair(mindestens_eine_Umstellung_war_erfolgreich, list)
  }

  /*********************************************************************************************************************
   *
   *********************************************************************************************************************/
  private fun `nimm fertige Gleichungen und setze Variablen`(gleichungen: List<Gleichung>, result: GleichungssytemResult): List<Gleichung> {
    val relevanteErgebnisse = gleichungen.map { it.getErgebnis() }.filterNotNull().filter { it.ist_konkret }
    // TODO man könnte checken, dass Variablen immer gleiche Werte haben

    var gleichungenTmp = gleichungen

    relevanteErgebnisse.forEach { ergebnis ->
      result.setzeFestenWert(ergebnis.variable, ergebnis.konkrete_Werte)

      gleichungenTmp = gleichungenTmp
          .map { gleichung -> gleichung.mitWertFuerVariable(ergebnis) }
          .filter { it.variables.isNotEmpty() }
    }

    return gleichungenTmp
  }

  /*********************************************************************************************************************
   *
   *********************************************************************************************************************/
  private fun `logge Resultate`(result: GleichungssytemResult, gleichungen: List<Gleichung>): GleichungssytemResult {
    // TODO mal checken, ob alle Gleichungen mit den Variablen laufen
    println()
    println("======== LÖSUNG ========")
    println("= Gleichungen:")
    gleichungen.print().lines().forEach { println("= $it") }
    println("= ")
    println("= Lösungen:")
    result.foundVariables.forEach { name, details ->
      print("=  $name -> ")
      print(if (details.first) "100%ig" else "u.A.")
      println("  " + details.second)
    }
    println("========================")

    return result
  }

//    while (true) {
////      val numberOfFoundVariables = result.numberOfSolvedVariables
////      println(umgestellteGleichungen.print())
////
////      println(">> identifiziere gelöse Variablen")
////      umgestellteGleichungen.map { it.getLoesung() }.filterNotNull().forEach { result.setVariable(it.first, it.second) }
////      println("  " + result)
////      if (numberOfFoundVariables == result.numberOfSolvedVariables) break
////
////      println(">> entferne unscharfte Gleichungen")
////      umgestellteGleichungen.removeIf { result.contains((it.left as VariableExpression).name) }
////      println(">> setze bekannte Variablen")
////      umgestellteGleichungen.replaceAll { gleichung ->
////        var gl = gleichung
////        result.map.forEach { varName, value -> gl = gleichung.withValue(varName, value) }
////        gl.simplify()
////      }
//    }

//    val gleichungenAusWertebereich: List<EineGleichung> = gleichungen.map { it.left.wertebereiche() + it.right.wertebereiche() }.flatten()
//    println(" > gleichungen aus Grenzwert: ${gleichungenAusWertebereich.size}\n" + gleichungenAusWertebereich.joinToString("\n").indentBy(10))
//
//
//    val alleBekanntenGleichungen = ArrayList<EineGleichung>()
//    alleBekanntenGleichungen.addAll(gleichungen + gleichungenAusWertebereich)
//
//    val alleBekanntenGleichungenDieUmgestelltSind = ArrayList<EineGleichung>()
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
//          debug("     > in EineGleichung " + bekannteGleichung)
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



