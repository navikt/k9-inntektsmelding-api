package no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests;

import jakarta.validation.constraints.NotNull;

public record Kontaktinformasjon(@NotNull String arbeidsgiverNavn,
                                 @NotNull String arbeidsgiverTlf) {}

