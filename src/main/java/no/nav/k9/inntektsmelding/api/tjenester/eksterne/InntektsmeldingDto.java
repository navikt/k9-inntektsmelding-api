package no.nav.k9.inntektsmelding.api.tjenester.eksterne;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Pattern;

import no.nav.k9.inntektsmelding.api.typer.EndringsårsakDto;
import no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto;
import no.nav.k9.inntektsmelding.api.typer.YtelseTypeDto;

public record InntektsmeldingDto(@NotNull UUID inntektsmeldingId,
                                 @NotNull @Pattern(regexp = "^\\d{11}$") String soekerFnr,
                                 @NotNull YtelseTypeDto ytelse,
                                 @NotNull InntektsmeldingArbeidsgiver arbeidsgiver,
                                 @NotNull LocalDate startdato,
                                 @NotNull Inntekt inntekt,
                                 @NotNull LocalDateTime innsendtTid,
                                 @NotNull AvsenderSystem avsender,
                                 Refusjon refusjon,
                                 List<Naturalytelse> naturalytelser,
                                 Omsorgspenger omsorgspenger) {

    public record Inntekt(@NotNull BigDecimal beloep, @NotNull LocalDate inntektsdato, @NotNull List<InntektEndringsårsaker> endringAarsaker) {
    }

    public record InntektsmeldingArbeidsgiver(@NotNull @Pattern(regexp = "^\\d{9}$") String orgnr, @NotNull Kontaktperson kontaktperson) {
    }

    public record Refusjon(@NotNull BigDecimal beloepPerMaaned, @NotNull List<RefusjonEndring> endringer) {
    }

    public record InntektEndringsårsaker(@NotNull EndringsårsakDto aarsak, LocalDate fom, LocalDate tom, LocalDate bleKjentFom) {
    }

    public record RefusjonEndring(@NotNull BigDecimal beloepPerMaaned, @NotNull LocalDate fom) {
    }

    public record AvsenderSystem(@NotNull String systemNavn, @NotNull String systemVersjon) {
    }

    public record Kontaktperson(@NotNull String navn, @NotNull String telefonnummer) {
    }

    public record Naturalytelse(@NotNull BigDecimal verdi, @NotNull LocalDate sluttdato, @NotNull NaturalytelsetypeDto naturalytelse) {
    }

    public record Omsorgspenger(Boolean harUtbetaltPliktigeDager,
                                @NotNull List<FraværHeleDager> fravaerHeleDager,
                                @NotNull List<FraværDelerAvDagen> fravaerDelerAvDagen) {
        public record FraværHeleDager(@NotNull LocalDate fom, @NotNull LocalDate tom) {}
        public record FraværDelerAvDagen(@NotNull LocalDate dato, @NotNull BigDecimal timer) {}
    }
}

