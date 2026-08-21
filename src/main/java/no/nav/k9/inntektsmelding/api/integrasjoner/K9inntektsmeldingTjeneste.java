package no.nav.k9.inntektsmelding.api.integrasjoner;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.k9.inntektsmelding.api.forespørsel.Forespørsel;
import no.nav.k9.inntektsmelding.api.inntektsmelding.Inntektsmelding;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.InntektInfo;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.InntektsmeldingRequest;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.Kontaktinformasjon;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.Naturalytelse;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.OmsorgspengerInfo;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.Refusjon;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.RefusjonskravOmsorgspengerRequest;
import no.nav.k9.inntektsmelding.api.typer.EndringsaarsakDto;
import no.nav.k9.inntektsmelding.api.typer.KodeverkMapper;
import no.nav.k9.inntektsmelding.api.typer.Organisasjonsnummer;
import no.nav.k9.inntektsmelding.api.typer.Periode;
import no.nav.k9.inntektsmelding.api.typer.StatusDto;
import no.nav.k9.inntektsmelding.api.typer.YtelseType;
import no.nav.k9.inntektsmelding.felles.AvsenderSystemDto;
import no.nav.k9.inntektsmelding.felles.BortfaltNaturalytelseDto;
import no.nav.k9.inntektsmelding.felles.EndringsårsakDto;
import no.nav.k9.inntektsmelding.felles.EndringsårsakerDto;
import no.nav.k9.inntektsmelding.felles.FødselsnummerDto;
import no.nav.k9.inntektsmelding.felles.KontaktpersonDto;
import no.nav.k9.inntektsmelding.felles.NaturalytelsetypeDto;
import no.nav.k9.inntektsmelding.felles.OmsorgspengerDto;
import no.nav.k9.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.k9.inntektsmelding.felles.PeriodeDto;
import no.nav.k9.inntektsmelding.felles.RefusjonDto;
import no.nav.k9.inntektsmelding.felles.YtelseTypeDto;
import no.nav.k9.inntektsmelding.imapi.forespørsel.ForespørselDto;
import no.nav.k9.inntektsmelding.imapi.forespørsel.HentForespørselerRequest;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.HentInntektsmeldingerRequest;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.InntektsmeldingDto;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingRequest;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingResponse;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.SendRefusjonOmsorgspengerRequest;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.SendRefusjonOmsorgspengerResponse;
import no.nav.vedtak.konfig.Tid;

@Dependent
public class K9inntektsmeldingTjeneste {
    private K9inntektsmeldingKlient k9inntektsmeldingKlient;

    K9inntektsmeldingTjeneste() {
        // for CDI proxy
    }

    @Inject
    public K9inntektsmeldingTjeneste(K9inntektsmeldingKlient k9inntektsmeldingKlient) {
        this.k9inntektsmeldingKlient = k9inntektsmeldingKlient;
    }

    public Forespørsel hentForespørsel(UUID forespørselUuid) {
        var response = k9inntektsmeldingKlient.hentForespørsel(forespørselUuid);
        return response != null ? mapResponseTilDomeneobjekt(response) : null;
    }

    public List<Forespørsel> hentForespørsler(String orgnr,
                                              String fnr,
                                              StatusDto status,
                                              YtelseType ytelseType,
                                              LocalDate fom,
                                              LocalDate tom,
                                              Long fraLoepenr) {
        var filter = new HentForespørselerRequest(new OrganisasjonsnummerDto(orgnr),
            fnr == null ? null : new FødselsnummerDto(fnr),
            status == null ? null : KodeverkMapper.mapApiStatusTilForespørselStatus(status),
            ytelseType == null ? null : mapYtelseType(ytelseType),
            fom,
            tom,
            fraLoepenr);
        var response = k9inntektsmeldingKlient.hentForespørsler(filter);
        if (response == null || response.forespørsler() == null) {
            return List.of();
        }
        return response.forespørsler().stream().map(this::mapResponseTilDomeneobjekt).toList();
    }

    public Inntektsmelding hentInntektsmelding(UUID innsendingId) {
        var response = k9inntektsmeldingKlient.hentInntektsmelding(innsendingId);
        return response == null ? null : mapInntektsmeldingResponseTilDomeneobjekt(response);
    }

    public List<Inntektsmelding> hentInntektsmeldinger(String orgnr,
                                                       String fnr,
                                                       UUID uuid,
                                                       YtelseType ytelseType,
                                                       LocalDate fom,
                                                       LocalDate tom,
                                                       Long fraLoepenr) {
        var request = new HentInntektsmeldingerRequest(new OrganisasjonsnummerDto(orgnr),
            fnr == null ? null : new FødselsnummerDto(fnr),
            ytelseType == null ? null : mapYtelseType(ytelseType),
            uuid,
            fom,
            tom,
            fraLoepenr);
        var response = k9inntektsmeldingKlient.hentInntektsmeldinger(request);
        if (response == null || response.inntektsmeldinger() == null) {
            return List.of();
        }
        return response.inntektsmeldinger().stream().map(this::mapInntektsmeldingResponseTilDomeneobjekt).toList();
    }

    private Inntektsmelding mapInntektsmeldingResponseTilDomeneobjekt(InntektsmeldingDto response) {
        return new Inntektsmelding(
            response.loepenr(),
            response.inntektsmeldingUuid(),
            response.fnr().fnr(),
            KodeverkMapper.mapTilDto(KodeverkMapper.mapYtelseType(response.ytelseType())),
            new Organisasjonsnummer(response.arbeidsgiver().orgnr()),
            new Inntektsmelding.Kontaktperson(response.kontaktperson().navn(), response.kontaktperson().telefonnummer()),
            response.startdato(),
            response.inntekt(),
            response.startdato(), // TODO: legg inn skjæringstidspunkt
            response.innsendtTidspunkt(),
            new Inntektsmelding.AvsenderSystem(response.avsenderSystem().systemNavn(), response.avsenderSystem().systemVersjon()),
            response.refusjonPrMnd(),
            response.opphørsdatoRefusjon(),
            response.refusjonsendringer() == null ? List.of() : response.refusjonsendringer().stream()
                .map(r -> new Inntektsmelding.Refusjon(r.fom(), r.beløp()))
                .toList(),
            response.bortfaltNaturalytelsePerioder() == null ? List.of() : response.bortfaltNaturalytelsePerioder().stream()
                .map(b -> new Inntektsmelding.BortfaltNaturalytelse(b.fom(),
                    b.tom(),
                    mapNaturalytelseTypeTilApiType(b.naturalytelsetype()),
                    b.beløp()))
                .toList(),
            response.endringAvInntektÅrsaker() == null ? List.of() : response.endringAvInntektÅrsaker().stream()
                .map(e -> new Inntektsmelding.Endringsårsaker(mapEndringsårsakTilApiType(e.årsak()), e.fom(), e.tom(), e.bleKjentFom()))
                .toList(),
            mapOmsorgspengerTilDomeneobjekt(response.omsorgspenger())
        );
    }

    private static Inntektsmelding.OmsorgspengerInfo mapOmsorgspengerTilDomeneobjekt(OmsorgspengerDto omsorgspengerDto) {
        if (omsorgspengerDto == null) {
            return null;
        }
        List<Inntektsmelding.OmsorgspengerInfo.FraværHeleDagenPeriode> heleDager = omsorgspengerDto.fraværHeleDager() == null ? List.of() :
                                                                                   omsorgspengerDto.fraværHeleDager().stream()
                .map(f -> new Inntektsmelding.OmsorgspengerInfo.FraværHeleDagenPeriode(f.fom(), f.tom()))
                .toList();
        List<Inntektsmelding.OmsorgspengerInfo.FraværDelerAvDagen> delerAvDager = omsorgspengerDto.fraværDelerAvDagen() == null ? List.of() :
                                                                                  omsorgspengerDto.fraværDelerAvDagen().stream()
                .map(f -> new Inntektsmelding.OmsorgspengerInfo.FraværDelerAvDagen(f.dato(), f.timer()))
                .toList();
        return new Inntektsmelding.OmsorgspengerInfo(omsorgspengerDto.harUtbetaltPliktigeDager(), heleDager, delerAvDager);
    }

    private no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto mapNaturalytelseTypeTilApiType(NaturalytelsetypeDto naturalytelsetype) {
        return switch (naturalytelsetype) {
            case ELEKTRISK_KOMMUNIKASJON -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.ElektroniskKommunikasjon;
            case AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS ->
                no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.AksjerGrunnfondsbevisTilUnderkurs;
            case LOSJI -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.Losji;
            case KOST_DOEGN -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.KostDoegn;
            case BESØKSREISER_HJEMMET_ANNET -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.BesoeksreiserHjemmetAnnet;
            case KOSTBESPARELSE_I_HJEMMET -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.KostbesparelseIHjemmet;
            case RENTEFORDEL_LÅN -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.RentefordelLaan;
            case BIL -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.Bil;
            case KOST_DAGER -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.KostDager;
            case BOLIG -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.Bolig;
            case SKATTEPLIKTIG_DEL_FORSIKRINGER ->
                no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.SkattepliktigDelForsikringer;
            case FRI_TRANSPORT -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.FriTransport;
            case OPSJONER -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.Opsjoner;
            case TILSKUDD_BARNEHAGEPLASS -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.TilskuddBarnehageplass;
            case ANNET -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.Annet;
            case BEDRIFTSBARNEHAGEPLASS -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.Bedriftsbarnehageplass;
            case YRKEBIL_TJENESTLIGBEHOV_KILOMETER ->
                no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.YrkebilTjenestligbehovKilometer;
            case YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS ->
                no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.YrkebilTjenestligbehovListepris;
            case INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING ->
                no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.InnbetalingTilUtenlandskPensjonsordning;
        };
    }

    private EndringsaarsakDto mapEndringsårsakTilApiType(EndringsårsakDto årsak) {
        return switch (årsak) {
            case PERMITTERING -> EndringsaarsakDto.Permittering;
            case NY_STILLING -> EndringsaarsakDto.NyStilling;
            case NY_STILLINGSPROSENT -> EndringsaarsakDto.NyStillingsprosent;
            case SYKEFRAVÆR -> EndringsaarsakDto.Sykefravaer;
            case BONUS -> EndringsaarsakDto.Bonus;
            case FERIETREKK_ELLER_UTBETALING_AV_FERIEPENGER ->
                EndringsaarsakDto.Ferietrekk;
            case NYANSATT -> EndringsaarsakDto.Nyansatt;
            case MANGELFULL_RAPPORTERING_AORDNING ->
                EndringsaarsakDto.MangelfullRapporteringAordning;
            case INNTEKT_IKKE_RAPPORTERT_ENDA_AORDNING ->
                EndringsaarsakDto.InntektIkkeRapportertEndaAordning;
            case TARIFFENDRING -> EndringsaarsakDto.Tariffendring;
            case FERIE -> EndringsaarsakDto.Ferie;
            case VARIG_LØNNSENDRING -> EndringsaarsakDto.VarigLoennsendring;
            case PERMISJON -> EndringsaarsakDto.Permisjon;
        };
    }


    public SendInntektsmeldingResponse sendInntektsmelding(InntektsmeldingRequest inntektsmeldingRequest, Forespørsel forespørsel) {
        var inntektsmeldingRequestDto = new SendInntektsmeldingRequest(
            forespørsel.forespørselUuid(),
            new FødselsnummerDto(forespørsel.fødselsnummer()),
            new OrganisasjonsnummerDto(forespørsel.orgnummer().orgnr()),
            inntektsmeldingRequest.startdato(),
            mapYtelseType(inntektsmeldingRequest.ytelse()),
            mapKontaktPersonDto(inntektsmeldingRequest.kontaktinformasjon()),
            inntektsmeldingRequest.inntekt().beloepPerMaaned(),
            mapRefusjonDto(inntektsmeldingRequest.refusjon(), inntektsmeldingRequest.startdato()),
            mapNaturalYtelseDto(inntektsmeldingRequest.naturalytelser()),
            mapEndringsårsakerDto(inntektsmeldingRequest.inntekt().endringAarsaker()),
            new AvsenderSystemDto(inntektsmeldingRequest.avsender().systemNavn(), inntektsmeldingRequest.avsender().systemVersjon())
        );

        return k9inntektsmeldingKlient.sendInntektsmelding(inntektsmeldingRequestDto);
    }

    public SendRefusjonOmsorgspengerResponse sendRefusjonOmsorgspenger(RefusjonskravOmsorgspengerRequest refusjonskravRequest) {
        var refusjonskravRequestDto = new SendRefusjonOmsorgspengerRequest(
            new FødselsnummerDto(refusjonskravRequest.soekerFnr()),
            new OrganisasjonsnummerDto(refusjonskravRequest.orgnr()),
            refusjonskravRequest.startdato(),
            mapKontaktPersonDto(refusjonskravRequest.kontaktinformasjon()),
            refusjonskravRequest.refusjon().beloepPerMaaned(),
            mapEndringsårsakerDto(refusjonskravRequest.refusjon().endringAarsaker()),
            new AvsenderSystemDto(refusjonskravRequest.avsender().systemNavn(),
                refusjonskravRequest.avsender().systemVersjon()),
            mapOmsorgspengerDto(refusjonskravRequest.omsorgspengerInfo())
        );

        return k9inntektsmeldingKlient.sendRefusjonOmsorgspenger(refusjonskravRequestDto);
    }

    private List<EndringsårsakerDto> mapEndringsårsakerDto(List<InntektInfo.Endringsaarsak> endringsårsak) {
        if (endringsårsak == null) {
            return List.of();
        }
        return endringsårsak.stream()
            .map(e -> new EndringsårsakerDto(mapÅrsakType(e.aarsak()), e.fom(), e.tom(), e.gjelderFra()))
            .toList();
    }

    private EndringsårsakDto mapÅrsakType(EndringsaarsakDto årsakType) {
        return switch (årsakType) {
            case Permittering -> EndringsårsakDto.PERMITTERING;
            case NyStilling -> EndringsårsakDto.NY_STILLING;
            case NyStillingsprosent -> EndringsårsakDto.NY_STILLINGSPROSENT;
            case Sykefravaer -> EndringsårsakDto.SYKEFRAVÆR;
            case Bonus -> EndringsårsakDto.BONUS;
            case Ferietrekk -> EndringsårsakDto.FERIETREKK_ELLER_UTBETALING_AV_FERIEPENGER;
            case Nyansatt -> EndringsårsakDto.NYANSATT;
            case MangelfullRapporteringAordning -> EndringsårsakDto.MANGELFULL_RAPPORTERING_AORDNING;
            case InntektIkkeRapportertEndaAordning -> EndringsårsakDto.INNTEKT_IKKE_RAPPORTERT_ENDA_AORDNING;
            case Tariffendring -> EndringsårsakDto.TARIFFENDRING;
            case Ferie -> EndringsårsakDto.FERIE;
            case VarigLoennsendring -> EndringsårsakDto.VARIG_LØNNSENDRING;
            case Permisjon -> EndringsårsakDto.PERMISJON;
        };
    }

    private List<BortfaltNaturalytelseDto> mapNaturalYtelseDto(List<Naturalytelse> naturalYtelser) {
        if (naturalYtelser == null) {
            return List.of();
        }
        return naturalYtelser.stream()
            .map(b -> new BortfaltNaturalytelseDto(b.bortfallerFra(), b.bortfallerTil() != null ? b.bortfallerTil() : Tid.TIDENES_ENDE, mapNaturalYtelseType(b.naturalytelse()), b.beloepPerMaaned()))
            .toList();
    }

    private NaturalytelsetypeDto mapNaturalYtelseType(no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto naturalytelsetype) {
        return switch (naturalytelsetype) {
            case ElektroniskKommunikasjon -> NaturalytelsetypeDto.ELEKTRISK_KOMMUNIKASJON;
            case AksjerGrunnfondsbevisTilUnderkurs -> NaturalytelsetypeDto.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS;
            case Losji -> NaturalytelsetypeDto.LOSJI;
            case KostDoegn -> NaturalytelsetypeDto.KOST_DOEGN;
            case BesoeksreiserHjemmetAnnet -> NaturalytelsetypeDto.BESØKSREISER_HJEMMET_ANNET;
            case KostbesparelseIHjemmet -> NaturalytelsetypeDto.KOSTBESPARELSE_I_HJEMMET;
            case RentefordelLaan -> NaturalytelsetypeDto.RENTEFORDEL_LÅN;
            case Bil -> NaturalytelsetypeDto.BIL;
            case KostDager -> NaturalytelsetypeDto.KOST_DAGER;
            case Bolig -> NaturalytelsetypeDto.BOLIG;
            case SkattepliktigDelForsikringer -> NaturalytelsetypeDto.SKATTEPLIKTIG_DEL_FORSIKRINGER;
            case FriTransport -> NaturalytelsetypeDto.FRI_TRANSPORT;
            case Opsjoner -> NaturalytelsetypeDto.OPSJONER;
            case TilskuddBarnehageplass -> NaturalytelsetypeDto.TILSKUDD_BARNEHAGEPLASS;
            case Annet -> NaturalytelsetypeDto.ANNET;
            case Bedriftsbarnehageplass -> NaturalytelsetypeDto.BEDRIFTSBARNEHAGEPLASS;
            case YrkebilTjenestligbehovKilometer -> NaturalytelsetypeDto.YRKEBIL_TJENESTLIGBEHOV_KILOMETER;
            case YrkebilTjenestligbehovListepris -> NaturalytelsetypeDto.YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS;
            case InnbetalingTilUtenlandskPensjonsordning -> NaturalytelsetypeDto.INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING;
        };
    }

    private List<RefusjonDto> mapRefusjonDto(Refusjon refusjon, LocalDate startdato) {
        if (refusjon == null) {
            return List.of();
        }
        List<RefusjonDto> refusjonDtoListe = new ArrayList<>();
        refusjonDtoListe.add(new RefusjonDto(startdato, refusjon.beloepPerMaaned()));
        refusjon.endringer().forEach(r -> refusjonDtoListe.add(new RefusjonDto(r.startdato(), r.beloepPerMaaned())));
        return refusjonDtoListe;
    }


    private OmsorgspengerDto mapOmsorgspengerDto(OmsorgspengerInfo omsorgspengerInfo) {
        if (omsorgspengerInfo == null) {
            return null;
        }
        List<PeriodeDto> fraværHeleDager = omsorgspengerInfo.fraværHeleDagenPerioder() == null ? List.of() :
                                           omsorgspengerInfo.fraværHeleDagenPerioder()
                                               .stream()
                                               .map(f -> new PeriodeDto(f.fom(), f.tom()))
                                               .toList();

        List<OmsorgspengerDto.FraværDelerAvDagenDto> fraværDelerAvDager = omsorgspengerInfo.fraværDelerAvDager() == null ? List.of() :
                                                                          omsorgspengerInfo.fraværDelerAvDager()
                                                                              .stream()
                                                                              .map(f -> new OmsorgspengerDto.FraværDelerAvDagenDto(f.dato(), f.timer()))
                                                                              .toList();

        List<PeriodeDto> trukketPerioder = omsorgspengerInfo.trukketPerioder() == null ? List.of() :
                                           omsorgspengerInfo.trukketPerioder()
                                               .stream()
                                               .map(f -> new PeriodeDto(f.fom(), f.tom()))
                                               .toList();

        return new OmsorgspengerDto(omsorgspengerInfo.harUtbetaltPliktigeDager(), fraværHeleDager, fraværDelerAvDager, trukketPerioder);
    }

    private YtelseTypeDto mapYtelseType(YtelseType ytelseType) {
        return switch (ytelseType) {
            case PLEIEPENGER_SYKT_BARN -> YtelseTypeDto.PLEIEPENGER_SYKT_BARN;
            case PLEIEPENGER_I_LIVETS_SLUTTFASE -> YtelseTypeDto.PLEIEPENGER_I_LIVETS_SLUTTFASE;
            case OPPLÆRINGSPENGER -> YtelseTypeDto.OPPLÆRINGSPENGER;
            case OMSORGSPENGER -> YtelseTypeDto.OMSORGSPENGER;
        };
    }

    private KontaktpersonDto mapKontaktPersonDto(Kontaktinformasjon kontaktinformasjon) {
        return new KontaktpersonDto(kontaktinformasjon.arbeidsgiverNavn(), kontaktinformasjon.arbeidsgiverTlf());
    }

    private Forespørsel mapResponseTilDomeneobjekt(ForespørselDto response) {
        return new Forespørsel(
            response.loepenr(),
            response.forespørselUuid(),
            new Organisasjonsnummer(response.orgnummer().orgnr()),
            response.fødselsnummer().fnr(),
            response.skjæringstidspunkt(),
            KodeverkMapper.mapForespørselStatus(response.status()),
            KodeverkMapper.mapYtelseType(response.ytelseType()),
            mapEtterspurtePerioder(response.etterspurtePerioder()),
            response.opprettetTid());
    }

    private static List<Periode> mapEtterspurtePerioder(List<PeriodeDto> etterspurtePerioder) {
        if (etterspurtePerioder == null) {
            return List.of();
        }
        return etterspurtePerioder.stream()
                .map(p -> new Periode(p.fom(), p.tom()))
                .toList();
    }
}
