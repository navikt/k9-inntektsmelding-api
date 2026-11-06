package no.nav.k9.inntektsmelding.api.tjenester.eksterne;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.core.Response;

import no.nav.k9.inntektsmelding.api.tjenester.eksterne.responses.ForespørselDto;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.ForespørselFilter;
import no.nav.k9.inntektsmelding.api.typer.YtelseType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.k9.inntektsmelding.api.forespørsel.Forespørsel;
import no.nav.k9.inntektsmelding.api.integrasjoner.K9inntektsmeldingTjeneste;
import no.nav.k9.inntektsmelding.api.server.auth.TilgangTjeneste;
import no.nav.k9.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.k9.inntektsmelding.api.server.exceptions.ErrorResponse;
import no.nav.k9.inntektsmelding.api.server.exceptions.InntektsmeldingAPIException;
import no.nav.k9.inntektsmelding.api.typer.ForespørselStatus;
import no.nav.k9.inntektsmelding.api.typer.Organisasjonsnummer;
import no.nav.k9.inntektsmelding.api.typer.StatusDto;

@ExtendWith(MockitoExtension.class)
class ForespørselRestTest {
    @Mock
    private K9inntektsmeldingTjeneste k9inntektsmeldingTjeneste;
    @Mock
    private TilgangTjeneste tilgangTjeneste;

    private ForespørselRest forespørselRest;

    @BeforeEach
    void setUp() {
        forespørselRest = new ForespørselRest(k9inntektsmeldingTjeneste, tilgangTjeneste);
    }

    @Test
    void skal_returnere_tom_liste() {
        var orgnummer = "999999999";
        var response = forespørselRest.hentForespørsler(new ForespørselFilter(orgnummer, null, null, null, null, null, null, null));
        assertThat(response.getStatus()).isEqualTo(200);
        var forespørsler = (List<ForespørselDto>) response.getEntity();
        assertThat(forespørsler).isEmpty();
    }

    @Test
    void skal_returnere_et_resultat_om_uuid_oppgit_og_ignorere_andre_filter_valg() {
        var orgnummer = "999999999";
        var uuid = UUID.randomUUID();
        when(k9inntektsmeldingTjeneste.hentForespørsel(uuid)).thenReturn(new Forespørsel(1L, uuid, new Organisasjonsnummer(orgnummer), "11111111111", LocalDate.now(),
            ForespørselStatus.UNDER_BEHANDLING, YtelseType.PLEIEPENGER_SYKT_BARN, List.of(), LocalDate.now().atStartOfDay()));
        var response = forespørselRest.hentForespørsler(new ForespørselFilter(orgnummer, null, uuid, StatusDto.FORKASTET, YtelseType.OMSORGSPENGER, null, null, null));
        assertThat(response.getStatus()).isEqualTo(200);
        var forespørsler = (List<ForespørselDto>) response.getEntity();
        assertThat(forespørsler).hasSize(1);
        assertThat(forespørsler.getFirst().forespoerselId()).isEqualTo(uuid);
        assertThat(forespørsler.getFirst().orgnr()).isEqualTo(orgnummer);
        assertThat(forespørsler.getFirst().status()).isEqualTo(StatusDto.AKTIV);
    }

    @Test
    void skal_returnere_feil_om_datoer_er_feil() {
        var orgnummer = "999999999";
        var response = forespørselRest.hentForespørsler(new ForespørselFilter(orgnummer, null, null, StatusDto.FORKASTET, YtelseType.OMSORGSPENGER, LocalDate.now(), LocalDate.now().minusMonths(1), null));
        assertThat(response.getStatus()).isEqualTo(400);
        var forespørsler = (ErrorResponse) response.getEntity();
        assertThat(forespørsler.feilmelding()).isEqualTo(EksponertFeilmelding.UGYLDIG_PERIODE.getTekst());
    }

    @Test
    void skal_returnere_liste_om_flere_matcher() {
        var orgnummer = "999999999";
        var forespørsel2 = new Forespørsel(1L, UUID.randomUUID(), new Organisasjonsnummer(orgnummer), "22222222222", LocalDate.now(),
            ForespørselStatus.UNDER_BEHANDLING, YtelseType.PLEIEPENGER_SYKT_BARN, List.of(), LocalDate.now().atStartOfDay());
        var forespørsel1 = new Forespørsel(2L, UUID.randomUUID(), new Organisasjonsnummer(orgnummer), "11111111111", LocalDate.now(),
            ForespørselStatus.UNDER_BEHANDLING, YtelseType.PLEIEPENGER_SYKT_BARN, List.of(), LocalDate.now().atStartOfDay());
        when(k9inntektsmeldingTjeneste.hentForespørsler(orgnummer, null, null, null, null, null, null)).thenReturn(List.of(forespørsel1, forespørsel2));
        var response = forespørselRest.hentForespørsler(new ForespørselFilter(orgnummer, null, null, null, null, null, null, null));
        assertThat(response.getStatus()).isEqualTo(200);
        var forespørsler = (List<ForespørselDto>) response.getEntity();
        assertThat(forespørsler).hasSize(2);
    }

    @Test
    void skal_avvise_forespoerselId_for_annen_organisasjon_enn_filteret() {
        var orgnr = "999999999";
        var forespørsel = lagForespørsel("888888888");
        when(k9inntektsmeldingTjeneste.hentForespørsel(forespørsel.forespørselUuid())).thenReturn(forespørsel);
        var filter = new ForespørselFilter(orgnr, null, forespørsel.forespørselUuid(), null, null, null, null, null);

        var ex = assertThrows(InntektsmeldingAPIException.class, () -> forespørselRest.hentForespørsler(filter));

        assertThat(ex.getFeilmelding()).isEqualTo(EksponertFeilmelding.IKKE_TILGANG_ALTINN);
        assertThat(ex.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED);
        verify(tilgangTjeneste).sjekkAtSystemHarTilgangTilOrganisasjon(new Organisasjonsnummer(orgnr));
        verifyNoMoreInteractions(tilgangTjeneste);
    }

    @Test
    void skal_hente_forespoerselId_for_samme_organisasjon_som_filteret() {
        var orgnr = "999999999";
        var forespørsel = lagForespørsel(orgnr);
        when(k9inntektsmeldingTjeneste.hentForespørsel(forespørsel.forespørselUuid())).thenReturn(forespørsel);
        var filter = new ForespørselFilter(orgnr, null, forespørsel.forespørselUuid(), null, null, null, null, null);

        var response = forespørselRest.hentForespørsler(filter);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        @SuppressWarnings("unchecked")
        var forespørsler = (List<ForespørselDto>) response.getEntity();
        assertThat(forespørsler).hasSize(1);
        assertThat(forespørsler.getFirst().forespoerselId()).isEqualTo(forespørsel.forespørselUuid());
        assertThat(forespørsler.getFirst().orgnr()).isEqualTo(orgnr);
        verify(tilgangTjeneste).sjekkAtSystemHarTilgangTilOrganisasjon(new Organisasjonsnummer(orgnr));
        verifyNoMoreInteractions(tilgangTjeneste);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void skal_avvise_hele_oppslaget_når_en_forespørsel_tilhører_annen_organisasjon(boolean inkluderEgenOrganisasjon) {
        var orgnr = "999999999";
        var fremmedForespørsel = lagForespørsel("888888888");
        var forespørsler = inkluderEgenOrganisasjon
            ? List.of(lagForespørsel(orgnr), fremmedForespørsel)
            : List.of(fremmedForespørsel);
        when(k9inntektsmeldingTjeneste.hentForespørsler(orgnr, null, null, null, null, null, null)).thenReturn(forespørsler);
        var filter = new ForespørselFilter(orgnr, null, null, null, null, null, null, null);

        var ex = assertThrows(InntektsmeldingAPIException.class, () -> forespørselRest.hentForespørsler(filter));

        assertThat(ex.getFeilmelding()).isEqualTo(EksponertFeilmelding.IKKE_TILGANG_ALTINN);
        assertThat(ex.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED);
        verify(tilgangTjeneste).sjekkAtSystemHarTilgangTilOrganisasjon(new Organisasjonsnummer(orgnr));
        verifyNoMoreInteractions(tilgangTjeneste);
    }

    @Test
    void skal_returnere_tom_liste_når_forespoerselId_ikke_finnes() {
        var orgnr = "999999999";
        var forespørselId = UUID.randomUUID();
        when(k9inntektsmeldingTjeneste.hentForespørsel(forespørselId)).thenReturn(null);
        var filter = new ForespørselFilter(orgnr, null, forespørselId, null, null, null, null, null);

        var response = forespørselRest.hentForespørsler(filter);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.getEntity()).isEqualTo(List.of());
        verify(tilgangTjeneste).sjekkAtSystemHarTilgangTilOrganisasjon(new Organisasjonsnummer(orgnr));
    }

    private Forespørsel lagForespørsel(String orgnr) {
        return new Forespørsel(1L, UUID.randomUUID(), new Organisasjonsnummer(orgnr), "11111111111", LocalDate.now(),
            ForespørselStatus.UNDER_BEHANDLING, YtelseType.PLEIEPENGER_SYKT_BARN, List.of(), LocalDate.now().atStartOfDay());
    }
}
