package no.nav.k9.inntektsmelding.api.forespørsel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import no.nav.k9.inntektsmelding.api.typer.ForespørselStatus;
import no.nav.k9.inntektsmelding.api.typer.Organisasjonsnummer;
import no.nav.k9.inntektsmelding.api.typer.Periode;
import no.nav.k9.inntektsmelding.api.typer.YtelseType;

public record Forespørsel(Long loepenr,
                          UUID forespørselUuid,
                          Organisasjonsnummer orgnummer,
                          String fødselsnummer,
                          LocalDate skjæringstidspunkt,
                          ForespørselStatus status,
                          YtelseType ytelseType,
                          List<Periode> etterspurtePerioder,
                          LocalDateTime opprettetTid) {
}
