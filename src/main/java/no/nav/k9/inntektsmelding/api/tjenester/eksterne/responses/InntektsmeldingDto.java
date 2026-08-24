package no.nav.k9.inntektsmelding.api.tjenester.eksterne.responses;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Pattern;

import no.nav.k9.inntektsmelding.api.typer.EndringsaarsakDto;
import no.nav.k9.inntektsmelding.api.typer.InntektsmeldingStatus;
import no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto;
import no.nav.k9.inntektsmelding.api.typer.YtelseTypeDto;

public record InntektsmeldingDto(@NotNull Long loepenr,
                                 @NotNull UUID inntektsmeldingId,
                                 @NotNull @Pattern(regexp = "^\\d{11}$") String soekerFnr,
                                 @NotNull YtelseTypeDto ytelse,
                                 @NotNull InntektsmeldingArbeidsgiver arbeidsgiver,
                                 @NotNull LocalDate startdato,
                                 @NotNull Inntekt inntekt,
                                 @NotNull LocalDateTime innsendtTid,
                                 @NotNull AvsenderSystem avsender,
                                 Refusjon refusjon,
                                 List<Naturalytelse> naturalytelser,
                                 @NotNull InntektsmeldingStatus status,
                                 OmsorgspengerInfo omsorgspengerInfo) {

    public record Inntekt(@NotNull BigDecimal beloep, @NotNull LocalDate inntektsdato, @NotNull List<InntektEndringsårsaker> endringAarsaker) {
    }

    public record InntektsmeldingArbeidsgiver(@NotNull @Pattern(regexp = "^\\d{9}$") String orgnr, @NotNull Kontaktperson kontaktperson) {
    }

    public record Refusjon(@NotNull BigDecimal beloepPerMaaned, @NotNull List<RefusjonEndring> endringer) {
    }

    public record InntektEndringsårsaker(@NotNull EndringsaarsakDto aarsak, LocalDate fom, LocalDate tom, LocalDate bleKjentFom) {
    }

    public record RefusjonEndring(@NotNull BigDecimal beloepPerMaaned, @NotNull LocalDate fom) {
    }

    public record AvsenderSystem(@NotNull String systemNavn, @NotNull String systemVersjon) {
    }

    public record Kontaktperson(@NotNull String navn, @NotNull String telefonnummer) {
    }

    public record Naturalytelse(@NotNull BigDecimal verdi, @NotNull LocalDate sluttdato, @NotNull NaturalytelsetypeDto naturalytelse) {
    }

    public record OmsorgspengerInfo(@NotNull Boolean harUtbetaltPliktigeDager,
                                    List<@Valid FraværHeleDagenPeriode> fraværHeleDagenPerioder,
                                    List<@Valid FraværDelerAvDagen> fravaerDelerAvDager) {

        public record FraværHeleDagenPeriode(@NotNull LocalDate fom,
                                             @NotNull LocalDate tom) {}

        public record FraværDelerAvDagen(@NotNull LocalDate dato,
                                         @NotNull BigDecimal timer) {}
    }
}

