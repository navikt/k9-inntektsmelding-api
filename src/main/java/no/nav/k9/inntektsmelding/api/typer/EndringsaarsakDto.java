package no.nav.k9.inntektsmelding.api.typer;

/**
 * Hvorfor inntekt i inntektsmeldingen er endret fra snittet de siste tre måneder
 */
public enum EndringsaarsakDto {
    Permittering,
    NyStilling,
    NyStillingsprosent,
    Sykefravaer,
    Bonus,
    Ferietrekk,
    Nyansatt,
    MangelfullRapporteringAordning,
    InntektIkkeRapportertEndaAordning,
    Tariffendring,
    Ferie,
    VarigLoennsendring,
    Permisjon
}
