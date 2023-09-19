package arenaoppslag.fellesordning

import java.time.LocalDate

data class FellesordningRequest(
    val personId:String,
    val datoForOnsketUttakForAFP:LocalDate,
)