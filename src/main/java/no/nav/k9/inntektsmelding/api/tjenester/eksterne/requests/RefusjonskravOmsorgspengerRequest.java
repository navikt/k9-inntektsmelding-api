package no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RefusjonskravOmsorgspengerRequest(@Pattern(
                                                     regexp = "^\\d{11}$",
                                                     message = "Fødselsnummer må bestå av 11 siffer"
                                                 ) @NotNull String soekerFnr,
                                                @Pattern(
                                                     regexp = "^\\d{9}$",
                                                     message = "Organisasjonsnummer må bestå av 9 siffer"
                                                 ) @NotNull String orgnr,
                                                @NotNull LocalDate startdato,
                                                @NotNull @Valid InntektInfo refusjon,
                                                @NotNull @Valid Kontaktinformasjon kontaktinformasjon,
                                                @NotNull @Valid Avsender avsender,
                                                @NotNull @Valid OmsorgspengerInfo omsorgspengerInfo) {
}
