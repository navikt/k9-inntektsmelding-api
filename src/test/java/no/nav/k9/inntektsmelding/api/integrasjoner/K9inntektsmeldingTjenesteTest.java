package no.nav.k9.inntektsmelding.api.integrasjoner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.k9.inntektsmelding.api.forespørsel.Forespørsel;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.Avsender;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.InntektInfo;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.InntektsmeldingRequest;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.Kontaktinformasjon;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.Naturalytelse;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.Refusjon;
import no.nav.k9.inntektsmelding.api.typer.EndringsårsakDto;
import no.nav.k9.inntektsmelding.api.typer.ForespørselStatus;
import no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto;
import no.nav.k9.inntektsmelding.api.typer.Organisasjonsnummer;
import no.nav.k9.inntektsmelding.api.typer.YtelseType;
import no.nav.k9.inntektsmelding.felles.AvsenderSystemDto;
import no.nav.k9.inntektsmelding.felles.ForespørselStatusDto;
import no.nav.k9.inntektsmelding.felles.FødselsnummerDto;
import no.nav.k9.inntektsmelding.felles.KontaktpersonDto;
import no.nav.k9.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.k9.inntektsmelding.felles.YtelseTypeDto;
import no.nav.k9.inntektsmelding.imapi.forespørsel.ForespørselDto;
import no.nav.k9.inntektsmelding.imapi.forespørsel.HentForespørselerRequest;
import no.nav.k9.inntektsmelding.imapi.forespørsel.HentForespørslerResponse;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.HentInntektsmeldingerResponse;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.InntektsmeldingDto;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingResponse;

@ExtendWith(MockitoExtension.class)
class K9inntektsmeldingTjenesteTest {
    @Mock
    private K9inntektsmeldingKlient k9inntektsmeldingKlient;

    private K9inntektsmeldingTjeneste k9inntektsmeldingTjeneste;

    @BeforeEach
    void setUp() {
        k9inntektsmeldingTjeneste = new K9inntektsmeldingTjeneste(k9inntektsmeldingKlient);
    }

    @Test
    void skal_hente_bestemt_forespørsel() {
        var orgnummer = "999999999";
        var uuid = UUID.randomUUID();
        var fødselsnummer = "123";
        var response = new ForespørselDto(uuid, new OrganisasjonsnummerDto(orgnummer), new FødselsnummerDto(fødselsnummer),
            LocalDate.now(), YtelseTypeDto.PLEIEPENGER_SYKT_BARN, ForespørselStatusDto.UNDER_BEHANDLING, List.of(), LocalDateTime.now());
        when(k9inntektsmeldingKlient.hentForespørsel(uuid)).thenReturn(response);
        var forespørsel = k9inntektsmeldingTjeneste.hentForespørsel(uuid);
        assertThat(forespørsel.orgnummer().orgnr()).isEqualTo(orgnummer);
        assertThat(forespørsel.ytelseType()).isEqualTo(YtelseType.PLEIEPENGER_SYKT_BARN);
        assertThat(forespørsel.fødselsnummer()).isEqualTo(fødselsnummer);
    }

    @Test
    void skal_hente_tom_liste_forespørsler() {
        var orgnummer = "999999999";
        when(k9inntektsmeldingKlient.hentForespørsler(new HentForespørselerRequest(new OrganisasjonsnummerDto(orgnummer),
            null,
            null,
            null,
            null,
            null))).thenReturn(
            new HentForespørslerResponse(List.of()));
        var forespørsler = k9inntektsmeldingTjeneste.hentForespørsler(orgnummer, null, null, null, null, null);
        assertThat(forespørsler).isEmpty();
    }

    @Test
    void skal_hente_liste_forespørsler() {
        var orgnummer = "999999999";
        var fødselsnummer = "123";
        var response1 = new ForespørselDto(UUID.randomUUID(), new OrganisasjonsnummerDto(orgnummer), new FødselsnummerDto(fødselsnummer),
            LocalDate.now(), YtelseTypeDto.PLEIEPENGER_SYKT_BARN, ForespørselStatusDto.UNDER_BEHANDLING, List.of(), LocalDateTime.now());
        var response2 = new ForespørselDto(UUID.randomUUID(), new OrganisasjonsnummerDto(orgnummer), new FødselsnummerDto(fødselsnummer),
            LocalDate.now(), YtelseTypeDto.OMSORGSPENGER, ForespørselStatusDto.UTGÅTT, List.of(), LocalDateTime.now());

        when(k9inntektsmeldingKlient.hentForespørsler(new HentForespørselerRequest(new OrganisasjonsnummerDto(orgnummer),
            null,
            null,
            null,
            null,
            null))).thenReturn(
            new HentForespørslerResponse(List.of(response1, response2)));
        var forespørsler = k9inntektsmeldingTjeneste.hentForespørsler(orgnummer, null, null, null, null, null);
        assertThat(forespørsler).hasSize(2);
        var forespørsel1 = forespørsler.stream().filter(f -> f.ytelseType().equals(YtelseType.PLEIEPENGER_SYKT_BARN)).findFirst().orElseThrow();
        var forespørsel2 = forespørsler.stream().filter(f -> f.ytelseType().equals(YtelseType.OMSORGSPENGER)).findFirst().orElseThrow();

        assertThat(forespørsel1.orgnummer().orgnr()).isEqualTo(orgnummer);
        assertThat(forespørsel1.status()).isEqualTo(ForespørselStatus.UNDER_BEHANDLING);
        assertThat(forespørsel1.fødselsnummer()).isEqualTo(fødselsnummer);

        assertThat(forespørsel2.orgnummer().orgnr()).isEqualTo(orgnummer);
        assertThat(forespørsel2.status()).isEqualTo(ForespørselStatus.UTGÅTT);
        assertThat(forespørsel2.fødselsnummer()).isEqualTo(fødselsnummer);

    }

    @Test
    void skal_sende_inntektsmelding_med_foreldrepenger() {
        var orgnummer = "999999999";
        var fødselsnummer = "12345678901";
        var uuid = UUID.randomUUID();
        var forespørsel = new Forespørsel(uuid, new Organisasjonsnummer(orgnummer), fødselsnummer,
            LocalDate.now(), ForespørselStatus.UNDER_BEHANDLING, YtelseType.PLEIEPENGER_SYKT_BARN, List.of(), LocalDateTime.now());
        var inntektsmeldingRequest = new InntektsmeldingRequest(
            uuid,
            fødselsnummer,
            LocalDate.now(),
            YtelseType.PLEIEPENGER_SYKT_BARN,
            new InntektInfo(BigDecimal.valueOf(25000.00), List.of()),
            new Refusjon(BigDecimal.valueOf(25000.00), List.of()),
            List.of(),
            new Kontaktinformasjon("Kontaktperson", "12345678"),
            new Avsender("TestSystem", "1.0.0"), null
        );
        var responseUuid = UUID.randomUUID();
        when(k9inntektsmeldingKlient.sendInntektsmelding(any())).thenReturn(new SendInntektsmeldingResponse(true, responseUuid, null));

        var response = k9inntektsmeldingTjeneste.sendInntektsmelding(inntektsmeldingRequest, forespørsel);

        assertThat(response).isNotNull();
        verify(k9inntektsmeldingKlient).sendInntektsmelding(any());
    }

    @Test
    void skal_sende_inntektsmelding_med_bortfalt_naturalytelse() {
        var orgnummer = "777777777";
        var fødselsnummer = "11111111111";
        var uuid = UUID.randomUUID();
        var forespørsel = new Forespørsel(uuid, new Organisasjonsnummer(orgnummer), fødselsnummer,
            LocalDate.now(), ForespørselStatus.UNDER_BEHANDLING, YtelseType.PLEIEPENGER_SYKT_BARN, List.of(), LocalDateTime.now());
        var bortfaltNaturalytelse = new Naturalytelse(
            NaturalytelsetypeDto.ELEKTRISK_KOMMUNIKASJON,
            BigDecimal.valueOf(500.00),
            LocalDate.now(),
            LocalDate.now().plusDays(10)
        );
        var inntektsmeldingRequest = new InntektsmeldingRequest(
            uuid,
            fødselsnummer,
            LocalDate.now(),
            YtelseType.PLEIEPENGER_SYKT_BARN,
            new InntektInfo(BigDecimal.valueOf(25000.00), List.of()),
            null,
            List.of(bortfaltNaturalytelse),
            new Kontaktinformasjon("Kontaktperson","12345678"),
            new Avsender("TestSystem", "1.0.0"), null
        );
        var responseUuid = UUID.randomUUID();
        when(k9inntektsmeldingKlient.sendInntektsmelding(any())).thenReturn(new SendInntektsmeldingResponse(true, responseUuid, null));

        var response = k9inntektsmeldingTjeneste.sendInntektsmelding(inntektsmeldingRequest, forespørsel);

        assertThat(response).isNotNull();
        verify(k9inntektsmeldingKlient).sendInntektsmelding(any());
    }

    @Test
    void skal_sende_inntektsmelding_med_endringsaarsaker() {
        var orgnummer = "666666666";
        var fødselsnummer = "22222222222";
        var uuid = UUID.randomUUID();
        var forespørsel = new Forespørsel(uuid, new Organisasjonsnummer(orgnummer), fødselsnummer,
            LocalDate.now(), ForespørselStatus.UNDER_BEHANDLING, YtelseType.PLEIEPENGER_SYKT_BARN, List.of(), LocalDateTime.now());
        var endringsårsak = new InntektInfo.Endringsårsak(
            EndringsårsakDto.PERMISJON,
            LocalDate.now(),
            LocalDate.now().plusDays(5),
            LocalDate.now().minusDays(1)
        );
        var inntektsmeldingRequest = new InntektsmeldingRequest(
            uuid,
            fødselsnummer,
            LocalDate.now(),
            YtelseType.PLEIEPENGER_SYKT_BARN,
            new InntektInfo(BigDecimal.valueOf(25000.00), List.of(endringsårsak)),
            new Refusjon(BigDecimal.valueOf(25000.00), List.of()),
            List.of(),
            new Kontaktinformasjon("Kontaktperson", "12345678"),
            new Avsender("TestSystem", "1.0.0"), null
        );
        var responseUuid = UUID.randomUUID();
        when(k9inntektsmeldingKlient.sendInntektsmelding(any())).thenReturn(new SendInntektsmeldingResponse(true, responseUuid, null));

        var response = k9inntektsmeldingTjeneste.sendInntektsmelding(inntektsmeldingRequest, forespørsel);

        assertThat(response).isNotNull();
        verify(k9inntektsmeldingKlient).sendInntektsmelding(any());
    }

    @Test
    void skal_sende_inntektsmelding_med_flere_refusjonsperioder() {
        var orgnummer = "555555555";
        var fødselsnummer = "33333333333";
        var uuid = UUID.randomUUID();
        var forespørsel = new Forespørsel(uuid, new Organisasjonsnummer(orgnummer), fødselsnummer,
            LocalDate.now(), ForespørselStatus.UNDER_BEHANDLING, YtelseType.PLEIEPENGER_SYKT_BARN, List.of(), LocalDateTime.now());
        var refusjoner =
            new Refusjon(BigDecimal.valueOf(25000.00), List.of(
                new Refusjon.RefusjonEndring(BigDecimal.valueOf(20000),LocalDate.now().plusDays(10)),
                new Refusjon.RefusjonEndring(BigDecimal.valueOf(15000), LocalDate.now().plusDays(20)))
            );
        var inntektsmeldingRequest = new InntektsmeldingRequest(
            uuid,
            fødselsnummer,
            LocalDate.now(),
            YtelseType.PLEIEPENGER_SYKT_BARN,
            new InntektInfo(BigDecimal.valueOf(25000.00), List.of()),
            refusjoner,
            List.of(),
            new Kontaktinformasjon("Kontaktperson", "12345678"),
            new Avsender("TestSystem", "1.0.0"), null
        );
        var responseUuid = UUID.randomUUID();
        when(k9inntektsmeldingKlient.sendInntektsmelding(any())).thenReturn(new SendInntektsmeldingResponse(true, responseUuid, null));

        var response = k9inntektsmeldingTjeneste.sendInntektsmelding(inntektsmeldingRequest, forespørsel);

        assertThat(response).isNotNull();
        verify(k9inntektsmeldingKlient).sendInntektsmelding(any());
    }

    @Test
    void skal_returnere_tomme_lister_naar_refusjonsendringer_naturalytelser_og_endringsaarsaker_er_null_i_hentInntektsmelding() {
        var uuid = UUID.randomUUID();
        var dto = lagInntektsmeldingDtoMedNullLister(uuid);
        when(k9inntektsmeldingKlient.hentInntektsmelding(uuid)).thenReturn(dto);

        var inntektsmelding = k9inntektsmeldingTjeneste.hentInntektsmelding(uuid);

        assertThat(inntektsmelding).isNotNull();
        assertThat(inntektsmelding.refusjon()).isEmpty();
        assertThat(inntektsmelding.bortfaltNaturalytelsePerioder()).isEmpty();
        assertThat(inntektsmelding.endringAvInntektÅrsaker()).isEmpty();
    }

    @Test
    void skal_returnere_tomme_lister_naar_refusjonsendringer_naturalytelser_og_endringsaarsaker_er_null_i_hentInntektsmeldinger() {
        var orgnr = "999999999";
        var dto = lagInntektsmeldingDtoMedNullLister(UUID.randomUUID());
        when(k9inntektsmeldingKlient.hentInntektsmeldinger(any())).thenReturn(new HentInntektsmeldingerResponse(List.of(dto)));

        var inntektsmeldinger = k9inntektsmeldingTjeneste.hentInntektsmeldinger(orgnr, null, null, YtelseType.PLEIEPENGER_SYKT_BARN, null, null);

        assertThat(inntektsmeldinger).hasSize(1);
        var inntektsmelding = inntektsmeldinger.getFirst();
        assertThat(inntektsmelding.refusjon()).isEmpty();
        assertThat(inntektsmelding.bortfaltNaturalytelsePerioder()).isEmpty();
        assertThat(inntektsmelding.endringAvInntektÅrsaker()).isEmpty();
    }

    @Test
    void skal_returnere_tom_liste_naar_hentForespørsler_klient_svarer_null() {
        when(k9inntektsmeldingKlient.hentForespørsler(any())).thenReturn(null);

        var forespørsler = k9inntektsmeldingTjeneste.hentForespørsler("999999999", null, null, null, null, null);

        assertThat(forespørsler).isEmpty();
    }

    @Test
    void skal_returnere_tom_liste_naar_hentInntektsmeldinger_klient_svarer_null() {
        when(k9inntektsmeldingKlient.hentInntektsmeldinger(any())).thenReturn(null);

        var inntektsmeldinger = k9inntektsmeldingTjeneste.hentInntektsmeldinger("999999999", null, null, YtelseType.PLEIEPENGER_SYKT_BARN, null, null);

        assertThat(inntektsmeldinger).isEmpty();
    }

    private static InntektsmeldingDto lagInntektsmeldingDtoMedNullLister(UUID inntektsmeldingUuid) {
        return new InntektsmeldingDto(
            inntektsmeldingUuid,
            UUID.randomUUID(),
            new FødselsnummerDto("12345678901"),
            YtelseTypeDto.PLEIEPENGER_SYKT_BARN,
            new OrganisasjonsnummerDto("999999999"),
            new KontaktpersonDto("Ola Nordmann", "99999999"),
            LocalDate.now(),
            BigDecimal.valueOf(50000),
            LocalDateTime.now(),
            BigDecimal.valueOf(50000),
            LocalDate.now().plusMonths(1),
            new AvsenderSystemDto("TestSystem", "1.0"),
            null,  // refusjonsendringer
            null,  // bortfaltNaturalytelsePerioder
            null,  // endringAvInntektÅrsaker
            null   // omsorgspenger
        );
    }
}
