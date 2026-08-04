package no.nav.k9.inntektsmelding.api.integrasjoner;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.k9.inntektsmelding.api.forespørsel.Forespørsel;
import no.nav.k9.inntektsmelding.api.inntektsmelding.Inntektsmelding;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.InntektsmeldingRequest;
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
                                              LocalDate tom) {
        var filter = new HentForespørselerRequest(new OrganisasjonsnummerDto(orgnr),
            fnr == null ? null : new FødselsnummerDto(fnr),
            status == null ? null : KodeverkMapper.mapApiStatusTilForespørselStatus(status),
            ytelseType == null ? null : mapYtelseType(ytelseType),
            fom,
            tom);
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
                                                       LocalDate tom) {
        var request = new HentInntektsmeldingerRequest(new OrganisasjonsnummerDto(orgnr),
            fnr == null ? null : new FødselsnummerDto(fnr),
            ytelseType == null ? null : mapYtelseType(ytelseType),
            uuid,
            fom,
            tom);
        var response = k9inntektsmeldingKlient.hentInntektsmeldinger(request);
        if (response == null || response.inntektsmeldinger() == null) {
            return List.of();
        }
        return response.inntektsmeldinger().stream().map(this::mapInntektsmeldingResponseTilDomeneobjekt).toList();
    }

    private Inntektsmelding mapInntektsmeldingResponseTilDomeneobjekt(InntektsmeldingDto response) {
        return new Inntektsmelding(
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
            case ELEKTRISK_KOMMUNIKASJON -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.ELEKTRISK_KOMMUNIKASJON;
            case AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS ->
                no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS;
            case LOSJI -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.LOSJI;
            case KOST_DOEGN -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.KOST_DOEGN;
            case BESØKSREISER_HJEMMET_ANNET -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.BESØKSREISER_HJEMMET_ANNET;
            case KOSTBESPARELSE_I_HJEMMET -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.KOSTBESPARELSE_I_HJEMMET;
            case RENTEFORDEL_LÅN -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.RENTEFORDEL_LÅN;
            case BIL -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.BIL;
            case KOST_DAGER -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.KOST_DAGER;
            case BOLIG -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.BOLIG;
            case SKATTEPLIKTIG_DEL_FORSIKRINGER ->
                no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.SKATTEPLIKTIG_DEL_FORSIKRINGER;
            case FRI_TRANSPORT -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.FRI_TRANSPORT;
            case OPSJONER -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.OPSJONER;
            case TILSKUDD_BARNEHAGEPLASS -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.TILSKUDD_BARNEHAGEPLASS;
            case ANNET -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.ANNET;
            case BEDRIFTSBARNEHAGEPLASS -> no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.BEDRIFTSBARNEHAGEPLASS;
            case YRKEBIL_TJENESTLIGBEHOV_KILOMETER ->
                no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.YRKEBIL_TJENESTLIGBEHOV_KILOMETER;
            case YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS ->
                no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS;
            case INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING ->
                no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto.INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING;
        };
    }

    private no.nav.k9.inntektsmelding.api.typer.EndringsårsakDto mapEndringsårsakTilApiType(EndringsårsakDto årsak) {
        return switch (årsak) {
            case PERMITTERING -> no.nav.k9.inntektsmelding.api.typer.EndringsårsakDto.PERMITTERING;
            case NY_STILLING -> no.nav.k9.inntektsmelding.api.typer.EndringsårsakDto.NY_STILLING;
            case NY_STILLINGSPROSENT -> no.nav.k9.inntektsmelding.api.typer.EndringsårsakDto.NY_STILLINGSPROSENT;
            case SYKEFRAVÆR -> no.nav.k9.inntektsmelding.api.typer.EndringsårsakDto.SYKEFRAVÆR;
            case BONUS -> no.nav.k9.inntektsmelding.api.typer.EndringsårsakDto.BONUS;
            case FERIETREKK_ELLER_UTBETALING_AV_FERIEPENGER ->
                no.nav.k9.inntektsmelding.api.typer.EndringsårsakDto.FERIETREKK_ELLER_UTBETALING_AV_FERIEPENGER;
            case NYANSATT -> no.nav.k9.inntektsmelding.api.typer.EndringsårsakDto.NYANSATT;
            case MANGELFULL_RAPPORTERING_AORDNING ->
                no.nav.k9.inntektsmelding.api.typer.EndringsårsakDto.MANGELFULL_RAPPORTERING_AORDNING;
            case INNTEKT_IKKE_RAPPORTERT_ENDA_AORDNING ->
                no.nav.k9.inntektsmelding.api.typer.EndringsårsakDto.INNTEKT_IKKE_RAPPORTERT_ENDA_AORDNING;
            case TARIFFENDRING -> no.nav.k9.inntektsmelding.api.typer.EndringsårsakDto.TARIFFENDRING;
            case FERIE -> no.nav.k9.inntektsmelding.api.typer.EndringsårsakDto.FERIE;
            case VARIG_LØNNSENDRING -> no.nav.k9.inntektsmelding.api.typer.EndringsårsakDto.VARIG_LØNNSENDRING;
            case PERMISJON -> no.nav.k9.inntektsmelding.api.typer.EndringsårsakDto.PERMISJON;
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
            new AvsenderSystemDto(inntektsmeldingRequest.avsender().systemNavn(),
                inntektsmeldingRequest.avsender().systemVersjon()),
            mapOmsorgspengerDto(inntektsmeldingRequest.omsorgspengerInfo())
        );

        return k9inntektsmeldingKlient.sendInntektsmelding(inntektsmeldingRequestDto);
    }

    private List<EndringsårsakerDto> mapEndringsårsakerDto(List<InntektsmeldingRequest.InntektInfo.Endringsårsak> endringsårsak) {
        if (endringsårsak == null) {
            return List.of();
        }
        return endringsårsak.stream()
            .map(e -> new EndringsårsakerDto(mapÅrsakType(e.aarsak()), e.fom(), e.tom(), e.gjelderFra()))
            .toList();
    }

    private EndringsårsakDto mapÅrsakType(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType årsakType) {
        return switch (årsakType) {
            case PERMITTERING -> EndringsårsakDto.PERMITTERING;
            case NY_STILLING -> EndringsårsakDto.NY_STILLING;
            case NY_STILLINGSPROSENT -> EndringsårsakDto.NY_STILLINGSPROSENT;
            case SYKEFRAVÆR -> EndringsårsakDto.SYKEFRAVÆR;
            case BONUS -> EndringsårsakDto.BONUS;
            case FERIETREKK_ELLER_UTBETALING_AV_FERIEPENGER -> EndringsårsakDto.FERIETREKK_ELLER_UTBETALING_AV_FERIEPENGER;
            case NYANSATT -> EndringsårsakDto.NYANSATT;
            case MANGELFULL_RAPPORTERING_AORDNING -> EndringsårsakDto.MANGELFULL_RAPPORTERING_AORDNING;
            case INNTEKT_IKKE_RAPPORTERT_ENDA_AORDNING -> EndringsårsakDto.INNTEKT_IKKE_RAPPORTERT_ENDA_AORDNING;
            case TARIFFENDRING -> EndringsårsakDto.TARIFFENDRING;
            case FERIE -> EndringsårsakDto.FERIE;
            case VARIG_LØNNSENDRING -> EndringsårsakDto.VARIG_LØNNSENDRING;
            case PERMISJON -> EndringsårsakDto.PERMISJON;
        };
    }

    private List<BortfaltNaturalytelseDto> mapNaturalYtelseDto(List<InntektsmeldingRequest.Naturalytelse> naturalYtelser) {
        if (naturalYtelser == null) {
            return List.of();
        }
        return naturalYtelser.stream()
            .map(b -> new BortfaltNaturalytelseDto(b.bortfallerFra(), b.bortfallerTil() != null ? b.bortfallerTil() : Tid.TIDENES_ENDE, mapNaturalYtelseType(b.naturalytelse()), b.beloepPerMaaned()))
            .toList();
    }

    private NaturalytelsetypeDto mapNaturalYtelseType(InntektsmeldingRequest.Naturalytelse.Naturalytelsetype naturalytelsetype) {
        return switch (naturalytelsetype) {
            case ELEKTRISK_KOMMUNIKASJON -> NaturalytelsetypeDto.ELEKTRISK_KOMMUNIKASJON;
            case AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS -> NaturalytelsetypeDto.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS;
            case LOSJI -> NaturalytelsetypeDto.LOSJI;
            case KOST_DOEGN -> NaturalytelsetypeDto.KOST_DOEGN;
            case BESØKSREISER_HJEMMET_ANNET -> NaturalytelsetypeDto.BESØKSREISER_HJEMMET_ANNET;
            case KOSTBESPARELSE_I_HJEMMET -> NaturalytelsetypeDto.KOSTBESPARELSE_I_HJEMMET;
            case RENTEFORDEL_LÅN -> NaturalytelsetypeDto.RENTEFORDEL_LÅN;
            case BIL -> NaturalytelsetypeDto.BIL;
            case KOST_DAGER -> NaturalytelsetypeDto.KOST_DAGER;
            case BOLIG -> NaturalytelsetypeDto.BOLIG;
            case SKATTEPLIKTIG_DEL_FORSIKRINGER -> NaturalytelsetypeDto.SKATTEPLIKTIG_DEL_FORSIKRINGER;
            case FRI_TRANSPORT -> NaturalytelsetypeDto.FRI_TRANSPORT;
            case OPSJONER -> NaturalytelsetypeDto.OPSJONER;
            case TILSKUDD_BARNEHAGEPLASS -> NaturalytelsetypeDto.TILSKUDD_BARNEHAGEPLASS;
            case ANNET -> NaturalytelsetypeDto.ANNET;
            case BEDRIFTSBARNEHAGEPLASS -> NaturalytelsetypeDto.BEDRIFTSBARNEHAGEPLASS;
            case YRKEBIL_TJENESTLIGBEHOV_KILOMETER -> NaturalytelsetypeDto.YRKEBIL_TJENESTLIGBEHOV_KILOMETER;
            case YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS -> NaturalytelsetypeDto.YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS;
            case INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING -> NaturalytelsetypeDto.INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING;
        };
    }

    private List<RefusjonDto> mapRefusjonDto(InntektsmeldingRequest.Refusjon refusjon, LocalDate startdato) {
        if (refusjon == null) {
            return List.of();
        }
        List<RefusjonDto> refusjonDtoListe = new ArrayList<>();
        refusjonDtoListe.add(new RefusjonDto(startdato, refusjon.beloepPerMaaned()));
        refusjon.endringer().forEach(r -> refusjonDtoListe.add(new RefusjonDto(r.stardato(), r.beloepPerMaaned())));
        return refusjonDtoListe;
    }


    private no.nav.k9.inntektsmelding.felles.OmsorgspengerDto mapOmsorgspengerDto(InntektsmeldingRequest.OmsorgspengerInfo omsorgspengerInfo) {
        if (omsorgspengerInfo == null) {
            return null;
        }
        List<no.nav.k9.inntektsmelding.felles.OmsorgspengerDto.FraværHeleDagerDto> fraværHeleDager = omsorgspengerInfo.fraværHeleDagenPerioder() == null ? List.of() :
                                                                                                     omsorgspengerInfo.fraværHeleDagenPerioder().stream()
                .map(f -> new no.nav.k9.inntektsmelding.felles.OmsorgspengerDto.FraværHeleDagerDto(f.fom(), f.tom()))
                .toList();
        List<no.nav.k9.inntektsmelding.felles.OmsorgspengerDto.FraværDelerAvDagenDto> fraværDelerAvDager = omsorgspengerInfo.fraværDelerAvDager() == null ? List.of() :
                                                                                                           omsorgspengerInfo.fraværDelerAvDager().stream()
                .map(f -> new no.nav.k9.inntektsmelding.felles.OmsorgspengerDto.FraværDelerAvDagenDto(f.dato(), f.timer()))
                .toList();
        return new no.nav.k9.inntektsmelding.felles.OmsorgspengerDto(omsorgspengerInfo.harUtbetaltPliktigeDager(), fraværHeleDager, fraværDelerAvDager);
    }

    private YtelseTypeDto mapYtelseType(YtelseType ytelseType) {
        return switch (ytelseType) {
            case PLEIEPENGER_SYKT_BARN -> YtelseTypeDto.PLEIEPENGER_SYKT_BARN;
            case PLEIEPENGER_I_LIVETS_SLUTTFASE -> YtelseTypeDto.PLEIEPENGER_I_LIVETS_SLUTTFASE;
            case OPPLÆRINGSPENGER -> YtelseTypeDto.OPPLÆRINGSPENGER;
            case OMSORGSPENGER -> YtelseTypeDto.OMSORGSPENGER;
        };
    }

    private KontaktpersonDto mapKontaktPersonDto(InntektsmeldingRequest.Kontaktinformasjon kontaktinformasjon) {
        return new KontaktpersonDto(kontaktinformasjon.arbeidsgiverNavn(), kontaktinformasjon.arbeidsgiverTlf());
    }

    private Forespørsel mapResponseTilDomeneobjekt(ForespørselDto response) {
        return new Forespørsel(response.forespørselUuid(),
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
