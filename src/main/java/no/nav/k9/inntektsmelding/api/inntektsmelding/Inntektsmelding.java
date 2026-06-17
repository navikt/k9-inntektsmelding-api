package no.nav.k9.inntektsmelding.api.inntektsmelding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import no.nav.k9.inntektsmelding.api.typer.EndringsårsakDto;
import no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto;
import no.nav.k9.inntektsmelding.api.typer.Organisasjonsnummer;
import no.nav.k9.inntektsmelding.api.typer.YtelseTypeDto;

public record Inntektsmelding(
    UUID inntektsmeldingUuid,
    String fnr,
    YtelseTypeDto ytelse,
    Organisasjonsnummer orgnr,
    Kontaktperson kontaktperson,
    LocalDate startdato,
    BigDecimal månedInntekt,
    LocalDate skjæringstidspunkt,
    LocalDateTime innsendtTidspunkt,
    AvsenderSystem avsenderSystem,
    BigDecimal månedRefusjon,
    LocalDate opphørsdatoRefusjon,
    List<Refusjon> refusjon,
    List<BortfaltNaturalytelse> bortfaltNaturalytelsePerioder,
    List<Endringsårsaker> endringAvInntektÅrsaker,
    Omsorgspenger omsorgspenger) {

    public record Refusjon(LocalDate fom,
                           BigDecimal beløp) {
    }

    public record BortfaltNaturalytelse(LocalDate fom,
                                        LocalDate tom,
                                        NaturalytelsetypeDto naturalytelsetype,
                                        BigDecimal beløp) {
    }

    public record Endringsårsaker(EndringsårsakDto årsak,
                                  LocalDate fom,
                                  LocalDate tom,
                                  LocalDate bleKjentFom) {
    }

    public record Kontaktperson(
        String navn,
        String telefonnummer
        ) {
    }

    public record AvsenderSystem(
        String navn,
        String versjon
    ) {
    }

    public record Omsorgspenger(Boolean harUtbetaltPliktigeDager,
                                List<FraværHeleDager> fraværHeleDager,
                                List<FraværDelerAvDagen> fraværDelerAvDagen) {
        public record FraværHeleDager(LocalDate fom, LocalDate tom) {}
        public record FraværDelerAvDagen(LocalDate dato, BigDecimal timer) {}
    }
}
