package no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import no.nav.k9.inntektsmelding.api.typer.StatusDto;
import no.nav.k9.inntektsmelding.api.typer.YtelseType;

public record ForespørselFilter(@NotNull @Pattern(regexp = "^\\d{9}$") String orgnr,
                                @Pattern(regexp = "^\\d{11}$") String soekerFnr,
                                @Valid UUID forespoerselId,
                                @Valid StatusDto status,
                                @Valid YtelseType ytelseType,
                                LocalDate fom,
                                LocalDate tom,
                                Long fraLoepenr) {}
