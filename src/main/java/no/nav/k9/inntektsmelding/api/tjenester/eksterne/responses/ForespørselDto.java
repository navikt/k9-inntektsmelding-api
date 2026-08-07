package no.nav.k9.inntektsmelding.api.tjenester.eksterne.responses;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import no.nav.k9.inntektsmelding.api.typer.StatusDto;
import no.nav.k9.inntektsmelding.api.typer.YtelseTypeDto;

public record ForespørselDto(@NotNull UUID forespoerselId,
                             @NotNull @Pattern(regexp = "^\\d{9}$") String orgnr,
                             @NotNull @Pattern(regexp = "^\\d{11}$") String soekerFnr,
                             @NotNull LocalDate startdato,
                             @NotNull StatusDto status,
                             @NotNull YtelseTypeDto ytelseType,
                             List<@Valid Periode> etterspurtePerioder,
                             @NotNull LocalDateTime opprettetTid) {

    public record Periode(@NotNull LocalDate fom,
                          @NotNull LocalDate tom) {
    }

}
