package no.nav.k9.inntektsmelding.api.tjenester.eksterne;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.inntektsmelding.api.forespørsel.Forespørsel;
import no.nav.k9.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.k9.inntektsmelding.api.typer.ForespørselStatus;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public class InntektsmeldingValidererUtil {
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingValidererUtil.class);

    private InntektsmeldingValidererUtil() {
        //skal ikke initialiseres
    }


    public static Optional<EksponertFeilmelding> validerInntektsmelding(InntektsmeldingRequest inntektsmeldingRequest, Forespørsel forespørsel) {
        var feilmeldingForespørsel = validerInntektsmeldingMotForespørsel(inntektsmeldingRequest, forespørsel);
        if (feilmeldingForespørsel.isPresent()) {
            return feilmeldingForespørsel;
        }

        var feilmeldingRefusjon = validerRefusjon(inntektsmeldingRequest.refusjon(), inntektsmeldingRequest.startdato());
        if (feilmeldingRefusjon.isPresent()) {
            return feilmeldingRefusjon;
        }

        var feilmeldingNaturalytelse = validerNaturalytelse(inntektsmeldingRequest.naturalytelser());
        if (feilmeldingNaturalytelse.isPresent()) {
            return feilmeldingNaturalytelse;
        }

        return validerEndringsårsaker(inntektsmeldingRequest.inntekt().endringAarsaker(), inntektsmeldingRequest.startdato());
    }

    public static Optional<EksponertFeilmelding> validerInntektsmeldingMotForespørsel(InntektsmeldingRequest inntektsmeldingRequest,
                                                                                      Forespørsel forespørsel) {
        if (forespørsel.status() == ForespørselStatus.UTGÅTT) {
            LOG.warn("Forespørsel med uuid {} har status UTGÅTT, og kan ikke motta inntektsmelding.", forespørsel.forespørselUuid());
            return Optional.of(EksponertFeilmelding.UGYLDIG_FORESPOERSEL);
        }
        if (!inntektsmeldingRequest.startdato().equals(forespørsel.skjæringstidspunkt())) {
            LOG.warn("Startdato fra inntektsmelding {} og skjæringstidspunkt fra forespørsel {} matcher ikke.",
                inntektsmeldingRequest.startdato(),
                forespørsel.skjæringstidspunkt());
            return Optional.of(EksponertFeilmelding.MISMATCH_SKJAERINGSTIDSPUNKT);
        }
        if (!inntektsmeldingRequest.ytelse().equals(forespørsel.ytelseType())) {
            LOG.warn("Ytelsetype fra inntektsmelding {} og ytelsetype fra forespørsel {} matcher ikke.",
                inntektsmeldingRequest.ytelse(),
                forespørsel.ytelseType());
            return Optional.of(EksponertFeilmelding.MISMATCH_YTELSE);
        }
        return Optional.empty();
    }

    public static Optional<EksponertFeilmelding> validerRefusjon(InntektsmeldingRequest.Refusjon refusjon, LocalDate startdato) {
        if (refusjon == null) {
            return Optional.empty();
        }
        //Todo Avklare om vi skal sjekke på at refusjonsbeløp ikke kan være større enn inntekt dersom inntekt ikke er 0. Sykepenger gjør dette, men hos oss er det tillatt i portalen i dag hvis endringsårsak oppgis.
        // Det fins tilfeller hvor arbeidsgiver ønsker å gjøre dette.

        var endringsListe = refusjon.endringer().stream()
            .map(InntektsmeldingRequest.Refusjon.RefusjonEndring::stardato)
            .toList();
        if (endringsListe.stream().anyMatch(stardato -> stardato.equals(startdato))) {
            LOG.info("Refusjon har en endring som starter på startdato for permisjonen, dette er ikke tillatt");
            return Optional.of(EksponertFeilmelding.REFUSJON_ENDRING_LIK_STARTDATO);
        }
        var harDuplikateStardatoer = endringsListe.size() > 1 && endringsListe.size() != new java.util.HashSet<>(endringsListe).size();
        if (harDuplikateStardatoer) {
            LOG.info("Refusjon har duplikate start-datoer: {}", endringsListe);
            return Optional.of(EksponertFeilmelding.LIK_START_DATO_REFUSJONSENDRINGER);
        }
        return Optional.empty();
    }

    public static Optional<EksponertFeilmelding> validerNaturalytelse(List<InntektsmeldingRequest.Naturalytelse> naturalytelsePerioder) {
        if (naturalytelsePerioder == null) {
            return Optional.empty();
        }

        var perioderMedFomOgTomDato = naturalytelsePerioder.stream()
            .filter(periode -> periode.bortfallerFra() != null && periode.bortfallerTil() != null)
            .toList();
        if (finnesOverlapp(perioderMedFomOgTomDato,
            InntektsmeldingRequest.Naturalytelse::bortfallerFra,
            InntektsmeldingRequest.Naturalytelse::bortfallerTil)) {
            LOG.info("Bortfalt naturalytelse har overlappende perioder");
            return Optional.of(EksponertFeilmelding.OVERLAPP_I_PERIODER);
        }

        if (perioderMedFomOgTomDato.stream().anyMatch(periode -> fraDatoEtterTom(periode.bortfallerFra(), periode.bortfallerTil()))) {
            LOG.info("Bortfalt naturalytelse har ugyldig periode. Fra dato er etter til dato for en eller flere perioder");
            return Optional.of(EksponertFeilmelding.FRA_DATO_ETTER_TOM);
        }

        var perioderMedKunFomDato = naturalytelsePerioder.stream()
            .filter(periode -> periode.bortfallerFra() != null && periode.bortfallerTil() == null)
            .toList();

        boolean harDuplikater = perioderMedKunFomDato.size() != new HashSet<>(perioderMedKunFomDato).size();

        if (harDuplikater) {
            LOG.info("Bortfalt naturalytelse har duplikate fom-datoer for perioder uten tom-dato: {}",
                perioderMedKunFomDato.stream().map(InntektsmeldingRequest.Naturalytelse::bortfallerFra).toList());
            return Optional.of(EksponertFeilmelding.LIK_FOM_NATURALYTELSER);
        }
        return Optional.empty();
    }


    public static Optional<EksponertFeilmelding> validerEndringsårsaker(List<InntektsmeldingRequest.InntektInfo.Endringsårsak> endringsårsaker,
                                                                        LocalDate startdato) {
        // Todo Tariffendring skal kun være tilgjengelig dersom man endrer en IM, ikke for førstegangs-innsendelse
        if (endringsårsaker == null) {
            return Optional.empty();
        }

        var unikeÅrsakerListe = endringsårsaker.stream().map(InntektsmeldingRequest.InntektInfo.Endringsårsak::aarsak)
            .filter(InntektsmeldingValidererUtil::skalÅrsakVæreUnik)
            .toList();

        boolean harDuplikater = unikeÅrsakerListe.size() != new HashSet<>(unikeÅrsakerListe).size();
        if (harDuplikater) {
            LOG.info("Det er oppgitt flere endringsårsaker av samme type: {}", unikeÅrsakerListe);
            return Optional.of(EksponertFeilmelding.DUPLIKATER_IKKE_TILATT);
        }

        var feilmeldingTariffendring = endringsårsaker.stream()
            .filter(årsak -> årsak.aarsak() == InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.TARIFFENDRING)
            .findFirst()
            .flatMap(InntektsmeldingValidererUtil::valideringTariffendring);
        if (feilmeldingTariffendring.isPresent()) {
            return feilmeldingTariffendring;
        }

        if (endringsårsaker.stream().anyMatch(årsak -> kreverFomDato(årsak.aarsak()) && årsak.fom() == null)) {
            LOG.info("Endringsårsak mangler fra dato");
            return Optional.of(EksponertFeilmelding.AARSAK_KREVER_FRA_DATO);
        }

        var varigLønnsendringFraDato = endringsårsaker.stream()
            .filter(årsak -> årsak.aarsak() == InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.VARIG_LØNNSENDRING)
            .findFirst()
            .map(InntektsmeldingRequest.InntektInfo.Endringsårsak::fom);

        if (varigLønnsendringFraDato.isPresent() && !varigLønnsendringFraDato.get().isBefore(startdato)) {
            LOG.info("Endringsårsak varig lønnsendring har ugyldig dato. Fra dato {} må være før fraværsdato {}",
                varigLønnsendringFraDato.get(),
                startdato);
            return Optional.of(EksponertFeilmelding.FRA_DATO_FOER_STARTDATO);
        }

        var årsakerSomKreverFomOgTomDato = endringsårsaker.stream()
            .filter(årsak -> kreverFomOgTomDato(årsak.aarsak()))
            .toList();

        if (!årsakerSomKreverFomOgTomDato.isEmpty()) {
            if (årsakerSomKreverFomOgTomDato.stream().anyMatch(årsak -> årsak.fom() == null || årsak.tom() == null)) {
                LOG.info("Endringsårsak mangler fra eller til dato");
                return Optional.of(EksponertFeilmelding.AARSAK_KREVER_FRA_OG_TIL_DATO);
            }

            if (årsakerSomKreverFomOgTomDato.stream().anyMatch( årsak -> fraDatoEtterTom(årsak.fom(), årsak.tom()))) {
                LOG.info("Endringsårsak har ugyldig periode. Fra dato er etter til dato for en eller flere endringsårsaker");
                return Optional.of(EksponertFeilmelding.FRA_DATO_ETTER_TOM);
            }

            if (finnesOverlapp(årsakerSomKreverFomOgTomDato,
                InntektsmeldingRequest.InntektInfo.Endringsårsak::fom,
                InntektsmeldingRequest.InntektInfo.Endringsårsak::tom)) {
                LOG.info("Endringsårsak har overlappende perioder");
                return Optional.of(EksponertFeilmelding.OVERLAPP_I_PERIODER);
            }
        }
        return Optional.empty();
    }

    private static Optional<EksponertFeilmelding> valideringTariffendring(InntektsmeldingRequest.InntektInfo.Endringsårsak endringsårsak) {
        if (endringsårsak != null) {
            if (endringsårsak.fom() == null || endringsårsak.gjelderFra() == null) {
                LOG.info("Endringsårsak tariffendring mangler fra dato eller ble gjelder fra dato");
                return Optional.of(EksponertFeilmelding.KREVER_FRA_OG_BLE_KJENT_DATO);
            }
            if (endringsårsak.gjelderFra().isBefore(endringsårsak.fom())) {
                LOG.info("Endringsårsak tariffendring har ugyldig dato. Gjelder fra dato {} er før fra dato {}",
                    endringsårsak.gjelderFra(),
                    endringsårsak.fom());
                return Optional.of(EksponertFeilmelding.KREVER_FRA_OG_BLE_KJENT_DATO);
            }
        }
        return Optional.empty();
    }

    private static boolean fraDatoEtterTom(LocalDate fom, LocalDate tom) {
        if (fom == null || tom == null) {
            return false;
        }
        return fom.isAfter(tom);
    }

    /**
     * Generisk metode for å sjekke om perioder overlapper i en liste.
     * Bruker LocalDateInterval fra tidsserie-biblioteket for å sjekke overlapp.
     * Fungerer med alle typer som har datoperiode (fra og til dato).
     *
     * @param periods           listen av perioder som skal valideres
     * @param fromDateExtractor funksjon for å hente 'fra'-dato fra en periode
     * @param toDateExtractor   funksjon for å hente 'til'-dato fra en periode
     * @param <T>               typen periodeobjekt
     * @return true hvis perioder overlapper, false hvis ingen overlapp finnes
     */
    private static <T> boolean finnesOverlapp(List<T> periods,
                                              Function<T, LocalDate> fromDateExtractor,
                                              Function<T, LocalDate> toDateExtractor) {
        if (periods == null || periods.size() < 2) {
            return false;
        }

        // Konverter alle perioder til LocalDateInterval objekter
        var intervals = periods.stream()
            .map(p -> new LocalDateInterval(fromDateExtractor.apply(p), toDateExtractor.apply(p)))
            .toList();

        // Sjekk om noen interval overlapper med en annen
        for (int i = 0; i < intervals.size(); i++) {
            for (int j = i + 1; j < intervals.size(); j++) {
                if (intervals.get(i).overlaps(intervals.get(j))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean kreverFomDato(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType årsakType) {
        return InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.NY_STILLING == årsakType
            || InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.NY_STILLINGSPROSENT == årsakType
            || InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.VARIG_LØNNSENDRING == årsakType;
    }

    private static boolean kreverFomOgTomDato(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType årsakType) {
        return InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.FERIE == årsakType
            || InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.PERMISJON == årsakType
            || InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.PERMITTERING == årsakType
            || InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.SYKEFRAVÆR == årsakType;
    }

    private static boolean skalÅrsakVæreUnik(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType årsakType) {
        return !(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.FERIE == årsakType
            || InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.PERMISJON == årsakType
            || InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.PERMITTERING == årsakType
            || InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.SYKEFRAVÆR == årsakType);
    }
}
