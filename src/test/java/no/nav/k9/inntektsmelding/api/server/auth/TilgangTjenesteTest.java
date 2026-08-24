package no.nav.k9.inntektsmelding.api.server.auth;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import no.nav.k9.inntektsmelding.api.server.auth.altinnPdp.PdpKlient;
import no.nav.k9.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.k9.inntektsmelding.api.server.exceptions.InntektsmeldingAPIException;
import no.nav.k9.inntektsmelding.api.typer.Organisasjonsnummer;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

class TilgangTjenesteTest {

    private static final String ORGNR = "999999999";
    private static final String SYSTEM_USER_ID = "systemuser";

    private final TilgangTjeneste tilgangTjeneste = new TilgangTjeneste();

    @AfterEach
    void tearDown() {
        KontekstHolder.fjernKontekst();
    }

    @Test
    void skal_kaste_ikke_tilgang_altinn_med_401_når_systemet_ikke_har_rettighet() throws Exception {
        // Arrange
        settTokenKontekst();
        var pdpKlient = mock(PdpKlient.class);
        when(pdpKlient.systemHarRettighetForOrganisasjon(anyString(), anyString(), anyString())).thenReturn(false);

        try (MockedStatic<PdpKlient> pdpKlientMock = mockStatic(PdpKlient.class)) {
            pdpKlientMock.when(PdpKlient::instance).thenReturn(pdpKlient);

            // Act
            var ex = assertThrows(InntektsmeldingAPIException.class,
                () -> tilgangTjeneste.sjekkAtSystemHarTilgangTilOrganisasjon(new Organisasjonsnummer(ORGNR)));

            // Assert - skal gi 401 Unauthorized, IKKE 500, når PDP-kallet svarer at systemet mangler tilgang
            assertThat(ex.getFeilmelding()).isEqualTo(EksponertFeilmelding.IKKE_TILGANG_ALTINN);
            assertThat(ex.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED);
        }
    }

    @Test
    void skal_kaste_feil_oppslag_altinn_med_500_når_pdp_kallet_feiler_teknisk() throws Exception {
        // Arrange
        settTokenKontekst();
        var pdpKlient = mock(PdpKlient.class);
        when(pdpKlient.systemHarRettighetForOrganisasjon(anyString(), anyString(), anyString())).thenThrow(new RuntimeException("teknisk feil mot PDP"));

        try (MockedStatic<PdpKlient> pdpKlientMock = mockStatic(PdpKlient.class)) {
            pdpKlientMock.when(PdpKlient::instance).thenReturn(pdpKlient);

            // Act
            var ex = assertThrows(InntektsmeldingAPIException.class,
                () -> tilgangTjeneste.sjekkAtSystemHarTilgangTilOrganisasjon(new Organisasjonsnummer(ORGNR)));

            // Assert - skal gi 500 kun når selve PDP-kallet feiler teknisk
            assertThat(ex.getFeilmelding()).isEqualTo(EksponertFeilmelding.FEIL_OPPSLAG_ALTINN);
            assertThat(ex.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Test
    void skal_ikke_kaste_noe_når_systemet_har_rettighet() throws Exception {
        // Arrange
        settTokenKontekst();
        var pdpKlient = mock(PdpKlient.class);
        when(pdpKlient.systemHarRettighetForOrganisasjon(anyString(), anyString(), anyString())).thenReturn(true);

        try (MockedStatic<PdpKlient> pdpKlientMock = mockStatic(PdpKlient.class)) {
            pdpKlientMock.when(PdpKlient::instance).thenReturn(pdpKlient);

            // Act + Assert
            tilgangTjeneste.sjekkAtSystemHarTilgangTilOrganisasjon(new Organisasjonsnummer(ORGNR));
        }
    }

    private void settTokenKontekst() {
        KontekstHolder.setKontekst(new TokenKontekst("uuid", ORGNR, ORGNR, SYSTEM_USER_ID));
    }
}
