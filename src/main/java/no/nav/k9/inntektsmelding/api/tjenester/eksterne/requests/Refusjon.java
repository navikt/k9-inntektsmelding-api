package no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record Refusjon(@NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beloepPerMaaned,
                       @NotNull @Valid List<RefusjonEndring> endringer) {
    public record RefusjonEndring(@NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beloepPerMaaned, @NotNull LocalDate stardato) {}
}

