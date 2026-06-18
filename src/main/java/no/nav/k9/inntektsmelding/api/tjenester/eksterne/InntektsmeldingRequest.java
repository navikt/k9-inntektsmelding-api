package no.nav.k9.inntektsmelding.api.tjenester.eksterne;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
                                     @NotNull Kontaktinformasjon kontaktinformasjon,
                                     @NotNull @Valid Avsender avsender,
                                     @Valid Omsorgspenger omsorgspenger) {


    public record InntektInfo(@NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beloepPerMaaned, @NotNull List<Endringsårsak> endringAarsaker) {
        public record Endringsårsak(@Valid EndringsårsakType aarsak,
                                    LocalDate fom,
                                    LocalDate tom,
                                    LocalDate gjelderFra) {
            public enum EndringsårsakType {
                PERMITTERING,
                NY_STILLING,
                NY_STILLINGSPROSENT,
                SYKEFRAVÆR,
                BONUS,
                FERIETREKK_ELLER_UTBETALING_AV_FERIEPENGER,
                NYANSATT,
                MANGELFULL_RAPPORTERING_AORDNING,
                INNTEKT_IKKE_RAPPORTERT_ENDA_AORDNING,
                TARIFFENDRING,
                FERIE,
                VARIG_LØNNSENDRING,
                PERMISJON
            }
        }
    }
    public record Refusjon(@NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beloepPerMaaned,
                           @NotNull @Valid List<RefusjonEndring> endringer) {
        public record RefusjonEndring(@NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beloepPerMaaned, @NotNull LocalDate stardato) {}

    }

    public record Kontaktinformasjon(@NotNull String arbeidsgiverNavn,  @NotNull String arbeidsgiverTlf) {}

    public record Naturalytelse(@NotNull Naturalytelsetype naturalytelse,
                                @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beloepPerMaaned,
                                @NotNull LocalDate bortfallerFra,
                                LocalDate bortfallerTil) {
        public enum Naturalytelsetype {
            ELEKTRISK_KOMMUNIKASJON,
            AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS,
            LOSJI,
            KOST_DOEGN,
            BESØKSREISER_HJEMMET_ANNET,
            KOSTBESPARELSE_I_HJEMMET,
            RENTEFORDEL_LÅN,
            BIL,
            KOST_DAGER,
            BOLIG,
            SKATTEPLIKTIG_DEL_FORSIKRINGER,
            FRI_TRANSPORT,
            OPSJONER,
            TILSKUDD_BARNEHAGEPLASS,
            ANNET,
            BEDRIFTSBARNEHAGEPLASS,
            YRKEBIL_TJENESTLIGBEHOV_KILOMETER,
            YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS,
            INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING
            }

    }

    public record Avsender(@NotNull @Size(max = 200) String systemNavn, @NotNull @Size(max = 100) String systemVersjon) {

    }

    public record Omsorgspenger(@NotNull Boolean harUtbetaltPliktigeDager,
                                List<@Valid FraværHeleDagenPeriode> fraværHeleDagenPerioder,
                                List<@Valid FraværDelerAvDagen> fraværDelerAvDager) {

        public record FraværHeleDagenPeriode(@NotNull LocalDate fom,
                                             @NotNull LocalDate tom) {}

        public record FraværDelerAvDagen(@NotNull LocalDate dato,
                                         @NotNull @Min(0) @Max(24) @Digits(integer = 2, fraction = 2) BigDecimal timer) {}
    }
}
