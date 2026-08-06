package no.nav.k9.inntektsmelding.api.tjenester.eksterne;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import no.nav.k9.inntektsmelding.api.tjenester.eksterne.responses.ForespørselDto;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.ForespørselFilter;
import no.nav.k9.inntektsmelding.api.typer.YtelseType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.k9.inntektsmelding.api.forespørsel.Forespørsel;
import no.nav.k9.inntektsmelding.api.integrasjoner.K9inntektsmeldingTjeneste;
import no.nav.k9.inntektsmelding.api.server.auth.TilgangTjeneste;
import no.nav.k9.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.k9.inntektsmelding.api.server.exceptions.ErrorResponse;
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
        var response = forespørselRest.hentForespørsler(new ForespørselFilter(orgnummer, null, null, null, null, null, null));
        assertThat(response.getStatus()).isEqualTo(200);
        var forespørsler = (List<ForespørselDto>) response.getEntity();
        assertThat(forespørsler).isEmpty();
    }

    @Test
    void skal_returnere_et_resultat_om_uuid_oppgit_og_ignorere_andre_filter_valg() {
        var orgnummer = "999999999";
        var uuid = UUID.randomUUID();
        when(k9inntektsmeldingTjeneste.hentForespørsel(uuid)).thenReturn(new Forespørsel(uuid, new Organisasjonsnummer(orgnummer), "11111111111", LocalDate.now(),
            ForespørselStatus.UNDER_BEHANDLING, YtelseType.PLEIEPENGER_SYKT_BARN, List.of(), LocalDate.now().atStartOfDay()));
        var response = forespørselRest.hentForespørsler(new ForespørselFilter(orgnummer, null, uuid, StatusDto.FORKASTET, YtelseType.OMSORGSPENGER, null, null));
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
        var response = forespørselRest.hentForespørsler(new ForespørselFilter(orgnummer, null, null, StatusDto.FORKASTET, YtelseType.OMSORGSPENGER, LocalDate.now(), LocalDate.now().minusMonths(1)));
        assertThat(response.getStatus()).isEqualTo(400);
        var forespørsler = (ErrorResponse) response.getEntity();
        assertThat(forespørsler.feilmelding()).isEqualTo(EksponertFeilmelding.UGYLDIG_PERIODE.getTekst());
    }

    @Test
    void skal_returnere_liste_om_flere_matcher() {
        var orgnummer = "999999999";
        var forespørsel2 = new Forespørsel(UUID.randomUUID(), new Organisasjonsnummer(orgnummer), "22222222222", LocalDate.now(),
            ForespørselStatus.UNDER_BEHANDLING, YtelseType.PLEIEPENGER_SYKT_BARN, List.of(), LocalDate.now().atStartOfDay());
        var forespørsel1 = new Forespørsel(UUID.randomUUID(), new Organisasjonsnummer(orgnummer), "11111111111", LocalDate.now(),
            ForespørselStatus.UNDER_BEHANDLING, YtelseType.PLEIEPENGER_SYKT_BARN, List.of(), LocalDate.now().atStartOfDay());
        when(k9inntektsmeldingTjeneste.hentForespørsler(orgnummer, null, null, null, null, null)).thenReturn(List.of(forespørsel1, forespørsel2));
        var response = forespørselRest.hentForespørsler(new ForespørselFilter(orgnummer, null, null, null, null, null, null));
        assertThat(response.getStatus()).isEqualTo(200);
        var forespørsler = (List<ForespørselDto>) response.getEntity();
        assertThat(forespørsler).hasSize(2);
    }

}
