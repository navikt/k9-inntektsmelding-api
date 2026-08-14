package no.nav.k9.inntektsmelding.api.integrasjoner;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Pattern;

import no.nav.k9.inntektsmelding.api.typer.EndringsaarsakDto;
import no.nav.k9.inntektsmelding.api.typer.KildesystemDto;
import no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto;
import no.nav.k9.inntektsmelding.api.typer.Organisasjonsnummer;
import no.nav.k9.inntektsmelding.api.typer.YtelseTypeDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

record InntektsmeldingResponse(
    @NotNull UUID inntektsmeldingUuid,
    @Pattern(regexp = "^\\d{11}$") @NotNull String fnr,
    @NotNull @Valid YtelseTypeDto ytelse,
    @NotNull @Valid Organisasjonsnummer orgnr,
    @NotNull @Valid Kontaktperson kontaktperson,
    @NotNull LocalDate startdato,
    @NotNull LocalDate skjæringstidspunkt,
    @NotNull BigDecimal månedInntekt,
    @NotNull LocalDateTime innsendtTidspunkt,
    @NotNull @Valid KildesystemDto kildesystem,
    @Valid AvsenderSystem avsenderSystem,
    @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal månedRefusjon,
    @NotNull LocalDate opphørsdatoRefusjon,
    @NotNull List<@Valid RefusjonEndring> refusjonsendringer,
    @NotNull List<@Valid BortfaltNaturalytelse> bortfaltNaturalytelsePerioder,
    @NotNull List<@Valid Endringsårsaker> endringAvInntektÅrsaker) {

    record RefusjonEndring(@NotNull LocalDate fom,
                           @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beløp) {
    }

    record BortfaltNaturalytelse(@NotNull LocalDate fom,
                                 LocalDate tom,
                                 @NotNull NaturalytelsetypeDto naturalytelsetype,
                                 @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beløp) {
    }

    record Endringsårsaker(@NotNull EndringsaarsakDto årsak,
                           LocalDate fom,
                           LocalDate tom,
                           LocalDate bleKjentFom) {
    }

    record Kontaktperson(
        @NotNull String telefonnummer,
        @NotNull String navn
    ) {
    }

    record AvsenderSystem(
        @NotNull String navn,
        @NotNull String versjon
    ) {
    }
}
