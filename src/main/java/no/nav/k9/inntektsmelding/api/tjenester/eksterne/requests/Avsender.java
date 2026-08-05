package no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record Avsender(@NotNull @Size(max = 200) String systemNavn, @NotNull @Size(max = 100) String systemVersjon) {}

