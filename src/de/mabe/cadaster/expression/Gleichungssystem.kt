package de.mabe.cadaster.expression

// ******************************************************************************************************************
// ******************************************************************************************************************
// ******************************************************************************************************************
private fun List<Gleichung>.print() = this.mapIndexed { i, it -> "  [$i] $it" }.joinToString("\n")

private fun List<Gleichung>.`enthalten mindestens eine gelöste, konkrete Variable`(): Boolean =
    this.any { it.getErgebnis()?.ist_konkret ?: false }

private fun List<*>.printIndexed(index: Int) = this.map { it.toString() }.mapIndexed { i, it -> if (i == index) "[$it]" else it }.joinToString(",")

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
  fun l(a: Any, d: Int): Unit = if (a.toString().lines().size > 1) a.toString().lines().forEach { l(it, d) } else println("| ".repeat(d) + a)

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
    fun l(a: Any) = l(a, depth)

    val gleichungen = gleichungenParam
    val variablen: HashSet<String> = gleichungen.map { it.variables }.fold(HashSet<String>(), { set, v -> set.addAll(v);set })

    if (depth > 0) l("+-" + "-".repeat(20), depth - 1)

    l(gleichungen.print())
    l("> aktuelles Result:\n   $result\n")
    if (result.fertig) return `logge Resultate`(result, this.gleichungen)
    if (gleichungen.isEmpty()) {
      l("> keine Gleichungen vorhanden, gehe Ebene zurück")
      return result
    }

    l("> Vereinfache Gleichungen")
    val vereinfachte_gleichungen = gleichungen.map { it.simplify() }
    l(vereinfachte_gleichungen.print())

    l("> Stelle Gleichungen um")
    val (mindestens_eine_Umstellung_war_erfolgreich: Boolean, umgestellte_gleichungen: List<Gleichung>)
        = `erzeuge Gleichungen, umgestellt für jede Variable`(gleichungen, variablen)
    l(umgestellte_gleichungen.print())

    l("> Überrpüfe, ob mindestens ein Gleichung umgestellt werden konnte: " + (if (mindestens_eine_Umstellung_war_erfolgreich) "JA" else "NEIN"))
    if (!mindestens_eine_Umstellung_war_erfolgreich) {
      l("> ABBRUCH: Keine der Gleichungen kann umgestellt werden")
      return Gleichungssystem_nicht_gelöst
    }

    l("> Überpüfe, ob Variable bereits gelöst wurde: " + (if (umgestellte_gleichungen.`enthalten mindestens eine gelöste, konkrete Variable`()) "JA" else "NEIN"))
    if (umgestellte_gleichungen.`enthalten mindestens eine gelöste, konkrete Variable`()) {
      l("> Es wurden gelöste Variablen gefunden, diese werden eingesetzt.")
      val eingesetzte_gleichungen = `nimm fertige Gleichungen und setze Variablen`(umgestellte_gleichungen, result)
      return solve(eingesetzte_gleichungen, result, depth + 1)
    }

    l("> Substituiere Variablen")
    val erfolgreich: Boolean = `Versuche per Substitution die Variablen zu reduzieren`(umgestellte_gleichungen, result, depth)
    if (erfolgreich) return result

    l("> Versuche Variablen zu erraten")
    TODO()
  }


  private fun `setze Variablen aus Result ein und mach weiter`(gleichungen: List<Gleichung>, result: GleichungssytemResult, depth: Int): GleichungssytemResult {
    TODO()

    solve(gleichungen, result, depth + 1)
  }

  /*********************************************************************************************************************
   * Versucht die Variablen zu reduzieren, indem sie substitutiert werden
   * Ist die Substitution erfolgreich, wird true zurück geliefert
   *********************************************************************************************************************/
  private fun `Versuche per Substitution die Variablen zu reduzieren`(gleichungen: List<Gleichung>, result: GleichungssytemResult, depth: Int): Boolean {
    fun l(a: Any) = l(a, depth)
    val substituierbare_Gleichungen_pro_Variable: List<Pair<String, List<Gleichung>>> =
        gleichungen.groupBy { it.linkeVariable ?: "DELETE" }.filterKeys { it != "DELETE" }
            .map { Pair(it.key, it.value) }

    for (index_aktuelle_Sub_Gleichung in 0..substituierbare_Gleichungen_pro_Variable.size - 1) {
      l("> Variablen: " + substituierbare_Gleichungen_pro_Variable.map { it.first }.printIndexed(index_aktuelle_Sub_Gleichung))
      val substituierbare_Gleichungen = substituierbare_Gleichungen_pro_Variable[index_aktuelle_Sub_Gleichung].second
      l(substituierbare_Gleichungen.print())

      val neue_Gleichungen = substituierbare_Gleichungen.map { substituierbare_Gleichung ->
        gleichungen.map { gleichung ->
          if (gleichung != substituierbare_Gleichung)
            gleichung.mitExpressionFuerVariable(substituierbare_Gleichung)
          else
            null
        }.filterNotNull()
      }.flatten()

      // TODO results werden hier in der Tiefe kaputt gemacht und nicht zurück gesetzt
      val firstResult = solve(neue_Gleichungen, result, depth + 1)
      if (firstResult !is Gleichungssystem_nicht_gelöst) {
        val secondResult = `setze Variablen aus Result ein und mach weiter`(gleichungen, result, depth)
        if (secondResult !is Gleichungssystem_nicht_gelöst) return true
      }
    }

    l("> Substitution fehlgeschlagen")
    return false
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
   * hier werden alle gleichungen genommen, es wird geschaut, welche der Gleichungen konkrete Werte für Variablen
   * liefern. Diese konkreten Werte werden
   * - in das Result abgelegt
   * - in die Gleichungen eingesetzt
   *
   * @return alle Gleichungen (mit bereits eingesetzten Variablen)
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
   * Testet noch einmal, ob alle Variablen korrekt sind, loggt dann die Ergebnisse und gibt sie zurück
   *********************************************************************************************************************/
  private fun `logge Resultate`(result: GleichungssytemResult, gleichungen: List<Gleichung>): GleichungssytemResult {
    // TODO mal checken, ob alle Gleichungen mit den Variablen laufen
    println()
    println("######## LÖSUNG ########")
    println("# Gleichungen:")
    gleichungen.print().lines().forEach { println("# $it") }
    println("# ")
    println("# Lösungen:")
    result.foundVariables.forEach { name, details ->
      print("#  $name -> ${details.second} (")
      print(if (details.first) "100%ig" else "u.A.")
      println(")")
    }
    println("########################")

    return result
  }

  override fun toString() = gleichungen.print()
}
