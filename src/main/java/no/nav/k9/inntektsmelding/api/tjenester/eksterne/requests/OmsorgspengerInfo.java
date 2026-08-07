package no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OmsorgspengerInfo(@NotNull Boolean harUtbetaltPliktigeDager,
                                List<@Valid Periode> fraværHeleDagenPerioder,
                                List<@Valid FraværDelerAvDagen> fraværDelerAvDager,
                                List<@Valid Periode> trukketPerioder) {

    public record Periode(@NotNull LocalDate fom,
                          @NotNull LocalDate tom) {}

    public record FraværDelerAvDagen(@NotNull LocalDate dato,
                                     @NotNull @Min(0) @Max(24) @Digits(integer = 2, fraction = 2) BigDecimal timer) {}
}

