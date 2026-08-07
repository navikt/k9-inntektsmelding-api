package no.nav.k9.inntektsmelding.api.tjenester.eksterne;

import jakarta.ws.rs.core.Response;

import no.nav.k9.inntektsmelding.api.forespørsel.Forespørsel;
import no.nav.k9.inntektsmelding.api.inntektsmelding.Inntektsmelding;
import no.nav.k9.inntektsmelding.api.integrasjoner.K9inntektsmeldingTjeneste;
import no.nav.k9.inntektsmelding.api.server.auth.Tilgang;
import no.nav.k9.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.k9.inntektsmelding.api.server.exceptions.ErrorResponse;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.responses.InntektsmeldingDto;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.Avsender;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.InntektInfo;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.InntektsmeldingFilter;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.InntektsmeldingRequest;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.Kontaktinformasjon;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.OmsorgspengerInfo;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.Refusjon;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.RefusjonskravOmsorgspengerRequest;
import no.nav.k9.inntektsmelding.api.typer.ForespørselStatus;
import no.nav.k9.inntektsmelding.api.typer.Organisasjonsnummer;
import no.nav.k9.inntektsmelding.api.typer.YtelseType;
import no.nav.k9.inntektsmelding.api.typer.YtelseTypeDto;
import no.nav.k9.inntektsmelding.felles.FeilInfo;
import no.nav.k9.inntektsmelding.felles.FeilkodeDto;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingResponse;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.SendRefusjonOmsorgspengerResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InntektsmeldingRestTest {
    @Mock
    private K9inntektsmeldingTjeneste k9inntektsmeldingTjeneste;
    @Mock
    private Tilgang tilgang;

    private InntektsmeldingRest inntektsmeldingRest;

    @BeforeEach
    void setUp() {
        inntektsmeldingRest = new InntektsmeldingRest(k9inntektsmeldingTjeneste, tilgang);
    }

    @Test
    void skal_sende_inntektsmelding_med_success() {
        // Arrange
        var orgnummer = "999999999";
        var fødselsnummer = "12345678901";
        var forespørselUuid = UUID.randomUUID();
        var responseUuid = UUID.randomUUID();

        var forespørsel = new Forespørsel(forespørselUuid, new Organisasjonsnummer(orgnummer), fødselsnummer,
            LocalDate.now(), ForespørselStatus.UNDER_BEHANDLING, YtelseType.PLEIEPENGER_SYKT_BARN,
            List.of(), LocalDateTime.now());

        var inntektsmeldingRequest = new InntektsmeldingRequest(
            forespørselUuid,
            fødselsnummer,
            LocalDate.now(),
            YtelseType.PLEIEPENGER_SYKT_BARN,
            new InntektInfo(BigDecimal.valueOf(25000.00), List.of()),
            new Refusjon(BigDecimal.valueOf(25000.00), List.of()),
            List.of(),
            new Kontaktinformasjon("Kontaktperson","12345678"),
            new Avsender("TestSystem", "1.0.0"),
            null
        );
        when(k9inntektsmeldingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(forespørsel);
        when(k9inntektsmeldingTjeneste.sendInntektsmelding(any(), any()))
            .thenReturn(new SendInntektsmeldingResponse(true, responseUuid, null));

        // Act
        var response = inntektsmeldingRest.sendInntektsmelding(inntektsmeldingRequest);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.getEntity()).isEqualTo(responseUuid);
    }

    @Test
    void skal_returnere_feil_når_forespørsel_ikke_finnes() {
        // Arrange
        var forespørselUuid = UUID.randomUUID();

        var inntektsmeldingRequest = new InntektsmeldingRequest(
            forespørselUuid,
            "12345678901",
            LocalDate.now(),
            YtelseType.PLEIEPENGER_SYKT_BARN,
            new InntektInfo(BigDecimal.valueOf(25000.00), List.of()),
            new Refusjon(BigDecimal.valueOf(25000.00), List.of()),
            List.of(),
            new Kontaktinformasjon("Kontaktperson", "12345678"),
            new Avsender("TestSystem", "1.0.0"),
            null
        );

        when(k9inntektsmeldingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(null);

        // Act
        var response = inntektsmeldingRest.sendInntektsmelding(inntektsmeldingRequest);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        var errorResponse = (ErrorResponse) response.getEntity();
        assertThat(errorResponse.feilmelding()).isEqualTo(EksponertFeilmelding.TOM_FORESPOERSEL.getTekst() + ": " + forespørselUuid);
    }

    @Test
    void skal_sende_refusjonskrav_omsorgspenger_med_success() {
        // Arrange
        var orgnummer = "999999999";
        var responseUuid = UUID.randomUUID();
        var refusjonskravRequest = lagRefusjonskravOmsorgspengerRequest(orgnummer, gyldigOmsorgspengerInfo());

        when(k9inntektsmeldingTjeneste.sendRefusjonOmsorgspenger(any()))
            .thenReturn(new SendRefusjonOmsorgspengerResponse(true, responseUuid, null));

        // Act
        var response = inntektsmeldingRest.sendRefusjonskravOmsorgspenger(refusjonskravRequest);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.getEntity()).isEqualTo(responseUuid);
        verify(tilgang).sjekkAtSystemHarTilgangTilOrganisasjon(new Organisasjonsnummer(orgnummer));
    }

    @Test
    void skal_returnere_bad_request_når_omsorgspenger_info_mangler_fraværsperioder() {
        // Arrange
        var orgnummer = "999999999";
        var omsorgspengerInfoUtenFravær = new OmsorgspengerInfo(false, List.of(), List.of(), List.of());
        var refusjonskravRequest = lagRefusjonskravOmsorgspengerRequest(orgnummer, omsorgspengerInfoUtenFravær);

        // Act
        var response = inntektsmeldingRest.sendRefusjonskravOmsorgspenger(refusjonskravRequest);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        var errorResponse = (ErrorResponse) response.getEntity();
        assertThat(errorResponse.feilmelding()).isEqualTo(EksponertFeilmelding.OMSORGSPENGER_MANGLER_FRAVÆRSPERIODER.getTekst());
    }

    @Test
    void skal_returnere_conflict_når_refusjonskrav_omsorgspenger_er_duplikat() {
        // Arrange
        var orgnummer = "999999999";
        var refusjonskravRequest = lagRefusjonskravOmsorgspengerRequest(orgnummer, gyldigOmsorgspengerInfo());

        when(k9inntektsmeldingTjeneste.sendRefusjonOmsorgspenger(any()))
            .thenReturn(new SendRefusjonOmsorgspengerResponse(false, null,
                new FeilInfo(FeilkodeDto.DUPLIKAT, "Duplikat innsending", "ref-1")));

        // Act
        var response = inntektsmeldingRest.sendRefusjonskravOmsorgspenger(refusjonskravRequest);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.CONFLICT.getStatusCode());
    }

    @Test
    void skal_returnere_service_unavailable_når_nedetid_ainntekt_for_refusjonskrav_omsorgspenger() {
        // Arrange
        var orgnummer = "999999999";
        var refusjonskravRequest = lagRefusjonskravOmsorgspengerRequest(orgnummer, gyldigOmsorgspengerInfo());

        when(k9inntektsmeldingTjeneste.sendRefusjonOmsorgspenger(any()))
            .thenReturn(new SendRefusjonOmsorgspengerResponse(false, null,
                new FeilInfo(FeilkodeDto.NEDETID_AINNTEKT, "A-inntekt nede", "ref-2")));

        // Act
        var response = inntektsmeldingRest.sendRefusjonskravOmsorgspenger(refusjonskravRequest);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
    }

    private RefusjonskravOmsorgspengerRequest lagRefusjonskravOmsorgspengerRequest(String orgnummer, OmsorgspengerInfo omsorgspengerInfo) {
        return new RefusjonskravOmsorgspengerRequest(
            "12345678901",
            orgnummer,
            LocalDate.now(),
            new InntektInfo(BigDecimal.valueOf(25000.00), List.of()),
            new Kontaktinformasjon("Kontaktperson", "12345678"),
            new Avsender("TestSystem", "1.0.0"),
            omsorgspengerInfo
        );
    }

    private OmsorgspengerInfo gyldigOmsorgspengerInfo() {
        return new OmsorgspengerInfo(false,
            List.of(new OmsorgspengerInfo.Periode(LocalDate.now(), LocalDate.now().plusDays(2))),
            List.of(),
            List.of());
    }

    @Test
    void skal_hente_inntektsmeldinger_med_filter_uten_innsendingId() {
        var orgnr = "999999999";
        var fnr = "12345678901";
        var forespørselId = UUID.randomUUID();
        var filter = new InntektsmeldingFilter(orgnr, fnr, forespørselId, null, YtelseType.PLEIEPENGER_SYKT_BARN, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));

        var inntektsmelding = lagInntektsmelding(orgnr);
        when(k9inntektsmeldingTjeneste.hentInntektsmeldinger(orgnr, fnr, forespørselId, YtelseType.PLEIEPENGER_SYKT_BARN, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)))
            .thenReturn(List.of(inntektsmelding));

        var response = inntektsmeldingRest.hentInntektsmeldinger(filter);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        @SuppressWarnings("unchecked")
        var dtoList = (List<InntektsmeldingDto>) response.getEntity();
        assertThat(dtoList).hasSize(1);
        verify(tilgang).sjekkAtSystemHarTilgangTilOrganisasjon(new Organisasjonsnummer(orgnr));
    }

    @Test
    void skal_returnere_bad_request_når_fom_er_etter_tom_med_innsendingId() {
        var orgnr = "999999999";
        var innsendingId = UUID.randomUUID();
        var filter = new InntektsmeldingFilter(orgnr, null, null, innsendingId, null, LocalDate.of(2025, 12, 31), LocalDate.of(2025, 1, 1));

        var inntektsmelding = lagInntektsmelding(orgnr);
        when(k9inntektsmeldingTjeneste.hentInntektsmelding(innsendingId)).thenReturn(inntektsmelding);

        var response = inntektsmeldingRest.hentInntektsmeldinger(filter);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        var errorResponse = (ErrorResponse) response.getEntity();
        assertThat(errorResponse.feilmelding()).isEqualTo(EksponertFeilmelding.UGYLDIG_PERIODE.getTekst());
    }

    private Inntektsmelding lagInntektsmelding(String orgnr) {
        return new Inntektsmelding(
            UUID.randomUUID(), "12345678901", YtelseTypeDto.PLEIEPENGER_SYKT_BARN,
            new Organisasjonsnummer(orgnr),
            new Inntektsmelding.Kontaktperson("Test", "12345678"),
            LocalDate.now(),
            BigDecimal.valueOf(50000), LocalDate.now(), LocalDateTime.now(),
            new Inntektsmelding.AvsenderSystem("Test", "1.0"),
            null, null, List.of(), List.of(), List.of(), null
        );
    }
}
