package no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import no.nav.k9.inntektsmelding.api.typer.YtelseType;

public record InntektsmeldingRequest(@NotNull @Valid UUID forespoerselId,
                                     @Pattern(
                                         regexp = "^\\d{11}$",
                                         message = "Fødselsnummer må bestå av 11 siffer"
                                     ) @NotNull String soekerFnr,
                                     @NotNull LocalDate startdato,
                                     @NotNull YtelseType ytelse,
                                     @NotNull @Valid InntektInfo inntekt,
                                     @Valid Refusjon refusjon,
                                     @NotNull List<@Valid Naturalytelse> naturalytelser,
                                     @NotNull @Valid Kontaktinformasjon kontaktinformasjon,
                                     @NotNull @Valid Avsender avsender) {
}
