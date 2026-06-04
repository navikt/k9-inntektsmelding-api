package no.nav.k9.inntektsmelding.api.tjenester.eksterne;

import jakarta.ws.rs.core.Response;

import no.nav.k9.inntektsmelding.api.forespørsel.Forespørsel;
import no.nav.k9.inntektsmelding.api.inntektsmelding.Inntektsmelding;
import no.nav.k9.inntektsmelding.api.inntektsmelding.InntektsmeldingDto;
import no.nav.k9.inntektsmelding.api.integrasjoner.K9inntektsmeldingTjeneste;
import no.nav.k9.inntektsmelding.api.server.auth.Tilgang;
import no.nav.k9.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.k9.inntektsmelding.api.server.exceptions.ErrorResponse;
import no.nav.k9.inntektsmelding.api.typer.ForespørselStatus;
import no.nav.k9.inntektsmelding.api.typer.Organisasjonsnummer;
import no.nav.k9.inntektsmelding.api.typer.YtelseType;
import no.nav.k9.inntektsmelding.api.typer.YtelseTypeDto;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingResponse;

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
            LocalDateTime.now());

        var inntektsmeldingRequest = new InntektsmeldingRequest(
            forespørselUuid,
            fødselsnummer,
            LocalDate.now(),
            YtelseType.PLEIEPENGER_SYKT_BARN,
            new InntektsmeldingRequest.InntektInfo(BigDecimal.valueOf(25000.00), List.of()),
            new InntektsmeldingRequest.Refusjon(BigDecimal.valueOf(25000.00), List.of()),
            List.of(),
            new InntektsmeldingRequest.Kontaktinformasjon("Kontaktperson","12345678"),
            new InntektsmeldingRequest.Avsender("TestSystem", "1.0.0")
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
            new InntektsmeldingRequest.InntektInfo(BigDecimal.valueOf(25000.00), List.of()),
            new InntektsmeldingRequest.Refusjon(BigDecimal.valueOf(25000.00), List.of()),
            List.of(),
            new InntektsmeldingRequest.Kontaktinformasjon("Kontaktperson", "12345678"),
            new InntektsmeldingRequest.Avsender("TestSystem", "1.0.0")
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
            null, null, List.of(), List.of(), List.of()
        );
    }
}
