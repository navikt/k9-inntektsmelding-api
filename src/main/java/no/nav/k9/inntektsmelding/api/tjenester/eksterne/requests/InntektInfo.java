package no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import no.nav.k9.inntektsmelding.api.typer.EndringsaarsakDto;

public record InntektInfo(@NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beloepPerMaaned,
                          @NotNull List<@Valid Endringsaarsak> endringAarsaker) {

    public record Endringsaarsak(@Valid @NotNull EndringsaarsakDto aarsak,
                                LocalDate fom,
                                LocalDate tom,
                                LocalDate gjelderFra) {
    }
}

