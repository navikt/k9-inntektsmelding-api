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
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.InntektInfo;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.InntektsmeldingRequest;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.Naturalytelse;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.OmsorgspengerInfo;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.Refusjon;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.RefusjonskravOmsorgspengerRequest;
import no.nav.k9.inntektsmelding.api.typer.EndringsårsakDto;
import no.nav.k9.inntektsmelding.api.typer.ForespørselStatus;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.inntektsmelding.api.typer.YtelseType;

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

        var feilmeldingOmsorgspengerInfo = validerOmsorgspengerInfo(inntektsmeldingRequest.omsorgspengerInfo(), inntektsmeldingRequest.ytelse());
        if (feilmeldingOmsorgspengerInfo.isPresent()) {
            return feilmeldingOmsorgspengerInfo;
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

    /**
     * Validerer et refusjonskrav for omsorgspenger. Krever ingen forespørsel siden refusjonskrav for
     * omsorgspenger ikke er knyttet til en forespørsel.
     */
    public static Optional<EksponertFeilmelding> validerRefusjonskravOmsorgspenger(RefusjonskravOmsorgspengerRequest refusjonskravRequest) {
        var feilmeldingOmsorgspengerInfo = validerOmsorgspengerInfo(refusjonskravRequest.omsorgspengerInfo(), YtelseType.OMSORGSPENGER);
        if (feilmeldingOmsorgspengerInfo.isPresent()) {
            return feilmeldingOmsorgspengerInfo;
        }
        return validerEndringsårsaker(refusjonskravRequest.refusjon().endringAarsaker(), refusjonskravRequest.startdato());
    }

    public static Optional<EksponertFeilmelding> validerOmsorgspengerInfo(OmsorgspengerInfo omsorgspengerInfo,
                                                                          YtelseType ytelseType) {
        if (!YtelseType.OMSORGSPENGER.equals(ytelseType)) {
            if (omsorgspengerInfo != null) {
                LOG.warn("Ytelsetype er {}, og skal ikke ha omsorgspenger-informasjon", ytelseType);
                return Optional.of(EksponertFeilmelding.OMSORGSPENGER_INFO_UGYLDIG_FOR_YTELSE);
            }
            return Optional.empty();
        }

        if (omsorgspengerInfo == null) {
            LOG.warn("Inntektsmelding for Omsorgspenger mangler omsorgspenger-informasjon");
            return Optional.of(EksponertFeilmelding.OMSORGSPENGER_KREVER_OMSORGSPENGER_INFO);
        }

        var heleDagenPerioder = omsorgspengerInfo.fraværHeleDagenPerioder();
        var delerAvDager = omsorgspengerInfo.fraværDelerAvDager();
        var trukketPerioder = omsorgspengerInfo.trukketPerioder();

        // Må ha minst én fraværsperiode
        if ((heleDagenPerioder == null || heleDagenPerioder.isEmpty()) &&
            (delerAvDager == null || delerAvDager.isEmpty()) &&
            (trukketPerioder == null || trukketPerioder.isEmpty())) {
            LOG.warn("Omsorgspenger mangler fraværsperioder");
            return Optional.of(EksponertFeilmelding.OMSORGSPENGER_MANGLER_FRAVÆRSPERIODER);
        }

        if (heleDagenPerioder != null && !heleDagenPerioder.isEmpty()) {
            // fom kan ikke være etter tom
            for (var periode : heleDagenPerioder) {
                if (periode.fom().isAfter(periode.tom())) {
                    LOG.warn("FraværHeleDagenPeriode har fom etter tom: {} > {}", periode.fom(), periode.tom());
                    return Optional.of(EksponertFeilmelding.FRA_DATO_ETTER_TOM);
                }
            }
            // Ingen overlappende perioder
            if (finnesOverlapp(heleDagenPerioder,
                OmsorgspengerInfo.Periode::fom,
                OmsorgspengerInfo.Periode::tom)) {
                LOG.warn("FraværHeleDagenPerioder har overlappende perioder");
                return Optional.of(EksponertFeilmelding.OMSORGSPENGER_OVERLAPP_I_HELE_DAGER);
            }
        }

        if (delerAvDager != null && !delerAvDager.isEmpty()) {
            // Ingen duplikate datoer
            var datoer = delerAvDager.stream().map(OmsorgspengerInfo.FraværDelerAvDagen::dato).toList();
            if (datoer.size() != new HashSet<>(datoer).size()) {
                LOG.warn("FraværDelerAvDager har duplikate datoer");
                return Optional.of(EksponertFeilmelding.OMSORGSPENGER_DUPLIKAT_FRAVAR_DELER_AV_DAGEN);
            }

            // Ingen fraværDelerAvDager-dato innenfor en fraværHeleDagenPeriode
            if (heleDagenPerioder != null) {
                for (var periode : heleDagenPerioder) {
                    for (var delAvDag : delerAvDager) {
                        var dato = delAvDag.dato();
                        if (!dato.isBefore(periode.fom()) && !dato.isAfter(periode.tom())) {
                            LOG.warn("FraværDelerAvDagen-dato {} faller innenfor fraværHeleDagenPeriode {} - {}",
                                dato, periode.fom(), periode.tom());
                            return Optional.of(EksponertFeilmelding.OMSORGSPENGER_FRAVAR_DELER_AV_DAGEN_OVERLAPPER_HEL_DAG);
                        }
                    }
                }
            }

            // Ingen fraværDelerAvDagen-timer er 0 eller mer enn 24
            if (delerAvDager.stream().anyMatch(delAvDag -> delAvDag.timer().compareTo(java.math.BigDecimal.ZERO) <= 0
                    || delAvDag.timer().compareTo(java.math.BigDecimal.valueOf(24)) >= 0)) {
                LOG.warn("FraværDelerAvDagen har timer som er 0 eller mer enn 24");
                return Optional.of(EksponertFeilmelding.OMSORGSPENGER_FRAVAR_DELER_AV_DAGEN_UGYLDIG_ANTALL_TIMER);
            }
        }

        if (trukketPerioder != null && !trukketPerioder.isEmpty()) {
            // fom kan ikke være etter tom
            for (var periode : trukketPerioder) {
                if (periode.fom().isAfter(periode.tom())) {
                    LOG.warn("Trukket periode har fom etter tom: {} > {}", periode.fom(), periode.tom());
                    return Optional.of(EksponertFeilmelding.FRA_DATO_ETTER_TOM);
                }
            }

            // Ingen overlapp internt i trukketPerioder
            if (finnesOverlapp(trukketPerioder,
                OmsorgspengerInfo.Periode::fom,
                OmsorgspengerInfo.Periode::tom)) {
                LOG.warn("Trukket perioder har overlappende perioder");
                return Optional.of(EksponertFeilmelding.OMSORGSPENGER_TRUKKET_PERIODE_OVERLAPPER);
            }

            // Ingen overlapp mot fraværHeleDagenPerioder
            if (heleDagenPerioder != null && !heleDagenPerioder.isEmpty()
                && finnesOverlappMellomTolister(trukketPerioder, heleDagenPerioder,
                OmsorgspengerInfo.Periode::fom, OmsorgspengerInfo.Periode::tom)) {
                LOG.warn("Trukket periode overlapper med fraværHeleDagenPerioder");
                return Optional.of(EksponertFeilmelding.OMSORGSPENGER_TRUKKET_PERIODE_OVERLAPPER);
            }

            // Ingen fraværDelerAvDager-dato innenfor en trukket periode
            if (delerAvDager != null) {
                for (var periode : trukketPerioder) {
                    for (var delAvDag : delerAvDager) {
                        var dato = delAvDag.dato();
                        if (!dato.isBefore(periode.fom()) && !dato.isAfter(periode.tom())) {
                            LOG.warn("FraværDelerAvDagen-dato {} faller innenfor trukket periode {} - {}",
                                dato, periode.fom(), periode.tom());
                            return Optional.of(EksponertFeilmelding.OMSORGSPENGER_TRUKKET_PERIODE_OVERLAPPER);
                        }
                    }
                }
            }
        }

        return Optional.empty();
    }

    public static Optional<EksponertFeilmelding> validerRefusjon(Refusjon refusjon, LocalDate startdato) {
        if (refusjon == null) {
            return Optional.empty();
        }
        //Todo Avklare om vi skal sjekke på at refusjonsbeløp ikke kan være større enn inntekt dersom inntekt ikke er 0. Sykepenger gjør dette, men hos oss er det tillatt i portalen i dag hvis endringsårsak oppgis.
        // Det fins tilfeller hvor arbeidsgiver ønsker å gjøre dette.

        var endringsListe = refusjon.endringer().stream()
            .map(Refusjon.RefusjonEndring::stardato)
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

    public static Optional<EksponertFeilmelding> validerNaturalytelse(List<Naturalytelse> naturalytelsePerioder) {
        if (naturalytelsePerioder == null) {
            return Optional.empty();
        }

        var perioderMedFomOgTomDato = naturalytelsePerioder.stream()
            .filter(periode -> periode.bortfallerFra() != null && periode.bortfallerTil() != null)
            .toList();
        if (finnesOverlapp(perioderMedFomOgTomDato,
            Naturalytelse::bortfallerFra,
            Naturalytelse::bortfallerTil)) {
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
                perioderMedKunFomDato.stream().map(Naturalytelse::bortfallerFra).toList());
            return Optional.of(EksponertFeilmelding.LIK_FOM_NATURALYTELSER);
        }
        return Optional.empty();
    }


    public static Optional<EksponertFeilmelding> validerEndringsårsaker(List<InntektInfo.Endringsårsak> endringsårsaker,
                                                                        LocalDate startdato) {
        // Todo Tariffendring skal kun være tilgjengelig dersom man endrer en IM, ikke for førstegangs-innsendelse
        if (endringsårsaker == null) {
            return Optional.empty();
        }

        var unikeÅrsakerListe = endringsårsaker.stream().map(InntektInfo.Endringsårsak::aarsak)
            .filter(InntektsmeldingValidererUtil::skalÅrsakVæreUnik)
            .toList();

        boolean harDuplikater = unikeÅrsakerListe.size() != new HashSet<>(unikeÅrsakerListe).size();
        if (harDuplikater) {
            LOG.info("Det er oppgitt flere endringsårsaker av samme type: {}", unikeÅrsakerListe);
            return Optional.of(EksponertFeilmelding.DUPLIKATER_IKKE_TILATT);
        }

        var feilmeldingTariffendring = endringsårsaker.stream()
            .filter(årsak -> årsak.aarsak() == EndringsårsakDto.TARIFFENDRING)
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
            .filter(årsak -> årsak.aarsak() == EndringsårsakDto.VARIG_LØNNSENDRING)
            .findFirst()
            .map(InntektInfo.Endringsårsak::fom);

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
                InntektInfo.Endringsårsak::fom,
                InntektInfo.Endringsårsak::tom)) {
                LOG.info("Endringsårsak har overlappende perioder");
                return Optional.of(EksponertFeilmelding.OVERLAPP_I_PERIODER);
            }
        }
        return Optional.empty();
    }

    private static Optional<EksponertFeilmelding> valideringTariffendring(InntektInfo.Endringsårsak endringsårsak) {
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

    /**
     * Sjekker om noen periode i den ene listen overlapper med noen periode i den andre listen.
     *
     * @param periodsA          første liste med perioder
     * @param periodsB          andre liste med perioder
     * @param fromDateExtractor funksjon for å hente 'fra'-dato fra en periode
     * @param toDateExtractor   funksjon for å hente 'til'-dato fra en periode
     * @param <T>               typen periodeobjekt (samme type for begge lister)
     * @return true hvis det finnes overlapp mellom listene, false ellers
     */
    private static <T> boolean finnesOverlappMellomTolister(List<T> periodsA,
                                                            List<T> periodsB,
                                                            Function<T, LocalDate> fromDateExtractor,
                                                            Function<T, LocalDate> toDateExtractor) {
        if (periodsA == null || periodsB == null || periodsA.isEmpty() || periodsB.isEmpty()) {
            return false;
        }
        var intervalsA = periodsA.stream()
            .map(p -> new LocalDateInterval(fromDateExtractor.apply(p), toDateExtractor.apply(p)))
            .toList();
        var intervalsB = periodsB.stream()
            .map(p -> new LocalDateInterval(fromDateExtractor.apply(p), toDateExtractor.apply(p)))
            .toList();

        for (var a : intervalsA) {
            for (var b : intervalsB) {
                if (a.overlaps(b)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean kreverFomDato(EndringsårsakDto årsakType) {
        return EndringsårsakDto.NY_STILLING == årsakType
            || EndringsårsakDto.NY_STILLINGSPROSENT == årsakType
            || EndringsårsakDto.VARIG_LØNNSENDRING == årsakType;
    }

    private static boolean kreverFomOgTomDato(EndringsårsakDto årsakType) {
        return EndringsårsakDto.FERIE == årsakType
            || EndringsårsakDto.PERMISJON == årsakType
            || EndringsårsakDto.PERMITTERING == årsakType
            || EndringsårsakDto.SYKEFRAVÆR == årsakType;
    }

    private static boolean skalÅrsakVæreUnik(EndringsårsakDto årsakType) {
        return !(EndringsårsakDto.FERIE == årsakType
            || EndringsårsakDto.PERMISJON == årsakType
            || EndringsårsakDto.PERMITTERING == årsakType
            || EndringsårsakDto.SYKEFRAVÆR == årsakType);
    }
}
