package no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InntektInfo(@NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beloepPerMaaned,
                          @NotNull List<Endringsårsak> endringAarsaker) {
    public record Endringsårsak(@Valid EndringsårsakType aarsak,
                                LocalDate fom,
                                LocalDate tom,
                                LocalDate gjelderFra) {
        public enum EndringsårsakType {
            PERMITTERING,
            NY_STILLING,
            NY_STILLINGSPROSENT,
            SYKEFRAVÆR,
            BONUS,
            FERIETREKK_ELLER_UTBETALING_AV_FERIEPENGER,
            NYANSATT,
            MANGELFULL_RAPPORTERING_AORDNING,
            INNTEKT_IKKE_RAPPORTERT_ENDA_AORDNING,
            TARIFFENDRING,
            FERIE,
            VARIG_LØNNSENDRING,
            PERMISJON
        }
    }
}

