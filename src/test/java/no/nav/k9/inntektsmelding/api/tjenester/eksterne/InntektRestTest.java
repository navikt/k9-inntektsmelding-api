package no.nav.k9.inntektsmelding.api.tjenester.eksterne;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.k9.inntektsmelding.api.forespørsel.Forespørsel;
import no.nav.k9.inntektsmelding.api.inntekt.Inntekt;
import no.nav.k9.inntektsmelding.api.integrasjoner.K9inntektsmeldingTjeneste;
import no.nav.k9.inntektsmelding.api.server.auth.Tilgang;
import no.nav.k9.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.k9.inntektsmelding.api.server.exceptions.ErrorResponse;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.responses.InntektDto;
import no.nav.k9.inntektsmelding.api.typer.ForespørselStatus;
import no.nav.k9.inntektsmelding.api.typer.Organisasjonsnummer;
import no.nav.k9.inntektsmelding.api.typer.YtelseType;

@ExtendWith(MockitoExtension.class)
class InntektRestTest {
    @Mock
    private K9inntektsmeldingTjeneste k9inntektsmeldingTjeneste;
    @Mock
    private Tilgang tilgang;

    private InntektRest inntektRest;

    @BeforeEach
    void setUp() {
        inntektRest = new InntektRest(k9inntektsmeldingTjeneste, tilgang);
    }

    @Test
    void skal_hente_inntekt() {
        var orgnummer = "999999999";
        var forespørselUuid = UUID.randomUUID();
        var forespørsel = new Forespørsel(1L, forespørselUuid, new Organisasjonsnummer(orgnummer), "11111111111", LocalDate.now(),
            ForespørselStatus.UNDER_BEHANDLING, YtelseType.PLEIEPENGER_SYKT_BARN, null, LocalDateTime.now());
        var inntekt = new Inntekt(Map.of(YearMonth.of(2025, 3), BigDecimal.valueOf(30000)), BigDecimal.valueOf(30000));

        when(k9inntektsmeldingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(forespørsel);
        when(k9inntektsmeldingTjeneste.hentInntekt(forespørselUuid)).thenReturn(inntekt);

        var response = inntektRest.hentInntekt(forespørselUuid.toString());

        assertThat(response.getStatus()).isEqualTo(200);
        var dto = (InntektDto) response.getEntity();
        assertThat(dto.gjennomsnittAvMaaneder()).isEqualByComparingTo(BigDecimal.valueOf(30000));
        assertThat(dto.inntektPerMaaned()).containsEntry(YearMonth.of(2025, 3), BigDecimal.valueOf(30000));
        verify(tilgang).sjekkAtSystemHarTilgangTilOrganisasjon(new Organisasjonsnummer(orgnummer));
    }

    @Test
    void skal_returnere_404_når_forespørsel_ikke_finnes() {
        var forespørselUuid = UUID.randomUUID();
        when(k9inntektsmeldingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(null);

        var response = inntektRest.hentInntekt(forespørselUuid.toString());

        assertThat(response.getStatus()).isEqualTo(404);
        var error = (ErrorResponse) response.getEntity();
        assertThat(error.feilkode()).isEqualTo(EksponertFeilmelding.TOM_FORESPOERSEL.name());
    }

    @Test
    void skal_returnere_404_med_egen_feilkode_når_inntekt_ikke_kan_hentes() {
        var orgnummer = "999999999";
        var forespørselUuid = UUID.randomUUID();
        var forespørsel = new Forespørsel(1L, forespørselUuid, new Organisasjonsnummer(orgnummer), "11111111111", LocalDate.now(),
            ForespørselStatus.UNDER_BEHANDLING, YtelseType.PLEIEPENGER_SYKT_BARN, null, LocalDateTime.now());

        when(k9inntektsmeldingTjeneste.hentForespørsel(forespørselUuid)).thenReturn(forespørsel);
        when(k9inntektsmeldingTjeneste.hentInntekt(forespørselUuid)).thenReturn(null);

        var response = inntektRest.hentInntekt(forespørselUuid.toString());

        assertThat(response.getStatus()).isEqualTo(404);
        var error = (ErrorResponse) response.getEntity();
        assertThat(error.feilkode()).isEqualTo(EksponertFeilmelding.INNTEKT_IKKE_TILGJENGELIG.name());
    }
}
