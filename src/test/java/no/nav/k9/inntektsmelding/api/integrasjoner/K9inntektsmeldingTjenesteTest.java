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
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.InntektsmeldingRequest;
import no.nav.k9.inntektsmelding.api.typer.ForespørselStatus;
import no.nav.k9.inntektsmelding.api.typer.Organisasjonsnummer;
import no.nav.k9.inntektsmelding.api.typer.YtelseType;
import no.nav.k9.inntektsmelding.felles.ForespørselStatusDto;
import no.nav.k9.inntektsmelding.felles.FødselsnummerDto;
import no.nav.k9.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.k9.inntektsmelding.felles.YtelseTypeDto;
import no.nav.k9.inntektsmelding.imapi.forespørsel.ForespørselDto;
import no.nav.k9.inntektsmelding.imapi.forespørsel.HentForespørselerRequest;
import no.nav.k9.inntektsmelding.imapi.forespørsel.HentForespørslerResponse;
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
            LocalDate.now(), ForespørselStatus.UNDER_BEHANDLING, YtelseType.PLEIEPENGER_SYKT_BARN, LocalDateTime.now());
        var inntektsmeldingRequest = new InntektsmeldingRequest(
            uuid,
            fødselsnummer,
            LocalDate.now(),
            YtelseType.PLEIEPENGER_SYKT_BARN,
            new InntektsmeldingRequest.InntektInfo(BigDecimal.valueOf(25000.00), List.of()),
            new InntektsmeldingRequest.Refusjon(BigDecimal.valueOf(25000.00), List.of()),
            List.of(),
            new InntektsmeldingRequest.Kontaktinformasjon("Kontaktperson", "12345678"),
            new InntektsmeldingRequest.Avsender("TestSystem", "1.0.0")
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
            LocalDate.now(), ForespørselStatus.UNDER_BEHANDLING, YtelseType.PLEIEPENGER_SYKT_BARN, LocalDateTime.now());
        var bortfaltNaturalytelse = new InntektsmeldingRequest.Naturalytelse(
            InntektsmeldingRequest.Naturalytelse.Naturalytelsetype.ELEKTRISK_KOMMUNIKASJON,
            BigDecimal.valueOf(500.00),
            LocalDate.now(),
            LocalDate.now().plusDays(10)
        );
        var inntektsmeldingRequest = new InntektsmeldingRequest(
            uuid,
            fødselsnummer,
            LocalDate.now(),
            YtelseType.PLEIEPENGER_SYKT_BARN,
            new InntektsmeldingRequest.InntektInfo(BigDecimal.valueOf(25000.00), List.of()),
            null,
            List.of(bortfaltNaturalytelse),
            new InntektsmeldingRequest.Kontaktinformasjon("Kontaktperson","12345678"),
            new InntektsmeldingRequest.Avsender("TestSystem", "1.0.0")
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
            LocalDate.now(), ForespørselStatus.UNDER_BEHANDLING, YtelseType.PLEIEPENGER_SYKT_BARN, LocalDateTime.now());
        var endringsårsak = new InntektsmeldingRequest.InntektInfo.Endringsårsak(
            InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.PERMISJON,
            LocalDate.now(),
            LocalDate.now().plusDays(5),
            LocalDate.now().minusDays(1)
        );
        var inntektsmeldingRequest = new InntektsmeldingRequest(
            uuid,
            fødselsnummer,
            LocalDate.now(),
            YtelseType.PLEIEPENGER_SYKT_BARN,
            new InntektsmeldingRequest.InntektInfo(BigDecimal.valueOf(25000.00), List.of(endringsårsak)),
            new InntektsmeldingRequest.Refusjon(BigDecimal.valueOf(25000.00), List.of()),
            List.of(),
            new InntektsmeldingRequest.Kontaktinformasjon("Kontaktperson", "12345678"),
            new InntektsmeldingRequest.Avsender("TestSystem", "1.0.0")
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
            LocalDate.now(), ForespørselStatus.UNDER_BEHANDLING, YtelseType.PLEIEPENGER_SYKT_BARN, LocalDateTime.now());
        var refusjoner =
            new InntektsmeldingRequest.Refusjon(BigDecimal.valueOf(25000.00), List.of(
                new InntektsmeldingRequest.Refusjon.RefusjonEndring(BigDecimal.valueOf(20000),LocalDate.now().plusDays(10)),
                new InntektsmeldingRequest.Refusjon.RefusjonEndring(BigDecimal.valueOf(15000), LocalDate.now().plusDays(20)))
            );
        var inntektsmeldingRequest = new InntektsmeldingRequest(
            uuid,
            fødselsnummer,
            LocalDate.now(),
            YtelseType.PLEIEPENGER_SYKT_BARN,
            new InntektsmeldingRequest.InntektInfo(BigDecimal.valueOf(25000.00), List.of()),
            refusjoner,
            List.of(),
            new InntektsmeldingRequest.Kontaktinformasjon("Kontaktperson", "12345678"),
            new InntektsmeldingRequest.Avsender("TestSystem", "1.0.0")
        );
        var responseUuid = UUID.randomUUID();
        when(k9inntektsmeldingKlient.sendInntektsmelding(any())).thenReturn(new SendInntektsmeldingResponse(true, responseUuid, null));

        var response = k9inntektsmeldingTjeneste.sendInntektsmelding(inntektsmeldingRequest, forespørsel);

        assertThat(response).isNotNull();
        verify(k9inntektsmeldingKlient).sendInntektsmelding(any());
    }
}
