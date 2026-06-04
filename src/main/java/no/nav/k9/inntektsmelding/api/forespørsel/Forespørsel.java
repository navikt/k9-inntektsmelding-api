package no.nav.k9.inntektsmelding.api.forespørsel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import no.nav.k9.inntektsmelding.api.typer.ForespørselStatus;
import no.nav.k9.inntektsmelding.api.typer.Organisasjonsnummer;
import no.nav.k9.inntektsmelding.api.typer.YtelseType;

public record Forespørsel(UUID forespørselUuid,
                          Organisasjonsnummer orgnummer,
                          String fødselsnummer,
                          LocalDate skjæringstidspunkt,
                          ForespørselStatus status,
                          YtelseType ytelseType,
                          LocalDateTime opprettetTid) {

}
