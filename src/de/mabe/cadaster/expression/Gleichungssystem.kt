package de.mabe.cadaster.expression

import de.mabe.cadaster.expression.Gleichheit.IST_GLEICH
import de.mabe.cadaster.util.indentBy


class Gleichungssystem(vararg gleichungen: Gleichung) {

  val gleichungen = gleichungen.toMutableList()

  fun solve() {
    println(this)

    val vc_initial = gleichungen.fold(VariableCount(), { vc, gl -> gl.variableCount(vc) })
    println(" > var-counts: " + vc_initial)

    fun erzeugeGleichungenUmgestelltNachVar(list: List<Gleichung>) = vc_initial.map { vc ->
      val varName = vc.key

      list.map {
        val solveResult = it.loese_auf_nach(varName)
        when (solveResult) {
          is ErfolgreicheUmstellung -> solveResult.gleichung
          is VariableNichtRelevant -> {
            debug("            .. Variable '$varName' not relevant : $it")
            null
          }
          is UmstellungNichtErfolgreich -> {
            println("            !! Unable to Solve for '$varName' : $it")
            null
          }
        }
      }.filterNotNull()
    }.flatten()


    gleichungen.forEach { it.simplify() }

    val gleichungenAusWertebereich: List<Gleichung> = gleichungen.map { it.left.wertebereiche() + it.right.wertebereiche() }.flatten()
    println(" > gleichungen aus Wertebereich: ${gleichungenAusWertebereich.size}\n" + gleichungenAusWertebereich.joinToString("\n").indentBy(10))


    val alleBekanntenGleichungen = ArrayList<Gleichung>()
    alleBekanntenGleichungen.addAll(gleichungen + gleichungenAusWertebereich)

    val alleBekanntenGleichungenDieUmgestelltSind = ArrayList<Gleichung>()
    alleBekanntenGleichungenDieUmgestelltSind.addAll(erzeugeGleichungenUmgestelltNachVar(alleBekanntenGleichungen))

    alleBekanntenGleichungen.addAll(alleBekanntenGleichungenDieUmgestelltSind)

    println(" > alle bekannten Gleichungen:")
    alleBekanntenGleichungen.forEach { println("        $it") }

    println(" > gleichungen umgestellt nach Variablen:")
    alleBekanntenGleichungenDieUmgestelltSind.forEach { println("        $it") }


    var umgestellteGleichungenIndex = 0
    println(" > Substitutionen")
    while (umgestellteGleichungenIndex < alleBekanntenGleichungenDieUmgestelltSind.size) {
      val umgestellteGleichungZumEinsetzen = alleBekanntenGleichungenDieUmgestelltSind[umgestellteGleichungenIndex]
      if (umgestellteGleichungZumEinsetzen.gleichheit == IST_GLEICH) {

        println("   > Substituiere " + umgestellteGleichungZumEinsetzen)

        alleBekanntenGleichungen.forEach { bekannteGleichung ->
          debug("     > in Gleichung " + bekannteGleichung)
          val varName = (umgestellteGleichungZumEinsetzen.left as VariableExpression).name
          val gleichungMitErsetzung = bekannteGleichung.withValue(varName, umgestellteGleichungZumEinsetzen.right)

          debug("       > mit Ersetzung: " + gleichungMitErsetzung)

          val neueUmgestellteGleichungen = erzeugeGleichungenUmgestelltNachVar(listOf(gleichungMitErsetzung))
          neueUmgestellteGleichungen.forEach {
            val neuSimple = it.simplify()
            debug("         > umgestellt + simple: $neuSimple     ......")
            if (alleBekanntenGleichungenDieUmgestelltSind.contains(neuSimple)) {
              debug("                                          -> bekannt")
            } else {
              debug("                                          -> neu")
              alleBekanntenGleichungenDieUmgestelltSind.add(neuSimple)
            }
          }
        }
      }
      umgestellteGleichungenIndex++
    }

    println(" > Gleichungen nach Substitution")
    alleBekanntenGleichungenDieUmgestelltSind.forEach { println("        $it") }

    val variablenUndIhrWert = HashMap<String, Double>()
    alleBekanntenGleichungenDieUmgestelltSind.forEach {
      if (it.gleichheit == IST_GLEICH && it.right is ValueExpression) {
        val varName = (it.left as VariableExpression).name
        val value = it.right.value
        if (variablenUndIhrWert.containsKey(varName) && variablenUndIhrWert[varName] != value)
          throw IllegalStateException("VAR '$varName' ist mal '${variablenUndIhrWert[varName]}' und mal '$value'")

        variablenUndIhrWert.put(varName, value)
      }
    }

    println(" > Ermittelte konkrete Variablen:")
    variablenUndIhrWert.forEach { varName, value -> println("        $varName: $value") }

    // TODO noch nicht ermittelte Variablen anhand ihres Wertebereichs ermitteln
    // immer nur eine neue Variable und dann wieder einsetzen
    
    // TODO mit allen ermittelten Variablen alle Gleichungen testen
  }

  override fun toString() = gleichungen.mapIndexed { i, it -> "[$i] $it" }.joinToString("\n")
}



