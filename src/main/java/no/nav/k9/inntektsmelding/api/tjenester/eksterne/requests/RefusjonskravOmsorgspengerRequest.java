package no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
                                                @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beloepPerMaaned,
                                                List<InntektInfo.Endringsårsak> endringAarsaker,
                                                @NotNull @Valid Kontaktinformasjon kontaktinformasjon,
                                                @NotNull @Valid Avsender avsender,
                                                @NotNull @Valid OmsorgspengerInfo omsorgspengerInfo) {
}

