package no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record Naturalytelse(@NotNull Naturalytelsetype naturalytelse,
                            @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beloepPerMaaned,
                            @NotNull LocalDate bortfallerFra,
                            LocalDate bortfallerTil) {

    public enum Naturalytelsetype {
        ELEKTRISK_KOMMUNIKASJON,
        AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS,
        LOSJI,
        KOST_DOEGN,
        BESØKSREISER_HJEMMET_ANNET,
        KOSTBESPARELSE_I_HJEMMET,
        RENTEFORDEL_LÅN,
        BIL,
        KOST_DAGER,
        BOLIG,
        SKATTEPLIKTIG_DEL_FORSIKRINGER,
        FRI_TRANSPORT,
        OPSJONER,
        TILSKUDD_BARNEHAGEPLASS,
        ANNET,
        BEDRIFTSBARNEHAGEPLASS,
        YRKEBIL_TJENESTLIGBEHOV_KILOMETER,
        YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS,
        INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING
    }
}

