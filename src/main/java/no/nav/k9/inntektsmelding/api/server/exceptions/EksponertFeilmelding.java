package no.nav.k9.inntektsmelding.api.server.exceptions;

public enum EksponertFeilmelding {

    // Tilgangsfeil
    MANGLER_TOKEN("Mangler token i header"),
    UTGAATT_TOKEN("Oppgitt token er utgått"),
    UGYLDIG_TOKEN("Oppgitt token er ugyldig"),
    FEIL_SCOPE("Token inneholder ikke riktig scope for denne operasjonen"),
    IKKE_TILGANG_ALTINN("Systemet har ikke registrert tilgang til organisasjonen i Altinn"),
    FEIL_OPPSLAG_ALTINN("Klarte ikke slå opp rettigheter i Altinn"),

    // Valideringsfeil
    SERIALISERINGSFEIL("Serialiseringsfeil"),
    VALIDERINGSFEIL("Valideringsfeil"),
    TOM_FORESPOERSEL("Finner ikke forespørsel"),
    TOM_INNTEKTSMELDING("Finner ikke inntektsmelding"),
    UGYLDIG_PERIODE("Oppgitt periode er ugyldig, fom kan ikke være etter tom"),
    MISMATCH_ORGNR("Organisasjonsnummer fra token og organisasjonsnummer fra etterspurt forespørsel matcher ikke"),
    MISMATCH_SKJAERINGSTIDSPUNKT("Skjæringstidspunkt fra inntektsmelding og skjæringstidspunkt fra etterspurt forespørsel matcher ikke"),
    MISMATCH_YTELSE("Ytelse fra inntektsmelding og ytelse fra etterspurt forespørsel matcher ikke"),
    MISMATCH_FOEDSELSNUMMER("Fødselsnummer fra inntektsmelding og forespørsel matcher ikke"),
    UGYLDIG_FORESPOERSEL("Det er ikke tillatt å sende inn en inntektsmelding på en forkastet forespørsel."),
    LIK_START_DATO_REFUSJONSENDRINGER("Endringer i refusjon kan ikke starte på samme dato"),
    LIK_FOM_NATURALYTELSER("Flere naturalytelsesperioder kan ikke starte på samme dato"),
    REFUSJON_ENDRING_LIK_STARTDATO("Endring i refusjon kan ikke starte på samme dato som startdato for permisjonen"),
    UGYLDIG_FRA_DATO_LISTE("Fra datoene i listen må være sammenhengende"),
    FRA_DATO_ETTER_TOM("Fra dato kan ikke være etter til dato"),
    OVERLAPP_I_PERIODER("Perioder kan ikke overlappe"),
    AARSAK_KREVER_FRA_DATO("Endringsårskene NY_STILLING, NY_STILLINGSPROSENT og VARIG_LØNNSENDRING krever at det oppgis en fra dato"),
    AARSAK_KREVER_FRA_OG_TIL_DATO("Endringsårskene FERIE, PERMISJON, PERMITTERING og SYKEFRAVÆR krever at det oppgis en fra dato og en til dato"),
    KREVER_FRA_OG_BLE_KJENT_DATO("Endringsårsaken Tariffendring krever at fra dato og gjelder fra dato er oppgitt"),
    OMSORGSPENGER_KREVER_OMSORGSPENGER_INFO("Omsorgspenger krever at omsorgspengerInfo er oppgitt"),
    OMSORGSPENGER_OVERLAPP_I_HELE_DAGER("Omsorgspenger kan ikke ha overlappende perioder for hele dager"),
    OMSORGSPENGER_MANGLER_FRAVÆRSPERIODER("Omsorgspenger krever minst én fraværsperiode (hel dag eller del av dag)"),
    OMSORGSPENGER_DUPLIKAT_FRAVAR_DELER_AV_DAGEN("Fraværsdato kan ikke forekomme mer enn én gang i fraværDelerAvDager"),
    OMSORGSPENGER_FRAVAR_DELER_AV_DAGEN_OVERLAPPER_HEL_DAG("Fraværsdag (del av dag) kan ikke falle innenfor en fraværsperiode for hel dag"),
    FRA_DATO_FOER_STARTDATO("Dato for varig lønnsendring må være før fraværsdato"),
    DUPLIKATER_IKKE_TILATT(
        "Duplikate endringsårsker er ikke tillatt for årsakene: NY_STILLING, NY_STILLINGSPROSENT, VARIG_LØNNSENDRING, BONUS, TARIFF_ENDRING, FERIETREKK_ELLER_UTBETALING_AV_FERIEPENGER, NYANSATT, MANGELFULL_RAPPORTERING_A-ORDNING, INNTEKT_IKKE_RAPPRTERT_ENDA_A-ORDNING"),
    // Default
    STANDARD_FEIL("Noe feilet.");

    private final String tekst;

    EksponertFeilmelding(String tekst) {
        this.tekst = tekst;
    }

    public String getTekst() {
        return tekst;
    }
}
