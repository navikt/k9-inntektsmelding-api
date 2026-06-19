package no.nav.k9.inntektsmelding.api.tjenester.eksterne;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.k9.inntektsmelding.api.forespørsel.Forespørsel;
import no.nav.k9.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.k9.inntektsmelding.api.typer.ForespørselStatus;
import no.nav.k9.inntektsmelding.api.typer.Organisasjonsnummer;
import no.nav.k9.inntektsmelding.api.typer.YtelseType;

class InntektsmeldingValidererUtilTest {

    private static final UUID DEFAULT_UUID = UUID.randomUUID();
    private static final String DEFAULT_FNR = "12345678901";
    private static final LocalDate STARTDATO = LocalDate.of(2025, 6, 1);
    private static final BigDecimal DEFAULT_BELØP = new BigDecimal("25000.00");


    // =====================================================================
    // validerInntektsmeldingMotForespørsel
    // =====================================================================

    @Test
    void skal_avvise_mismatch_ytelsetype() {
        var forespørsel = lagForespørsel(ForespørselStatus.UNDER_BEHANDLING, STARTDATO, YtelseType.OMSORGSPENGER);
        var result = InntektsmeldingValidererUtil.validerInntektsmeldingMotForespørsel(lagDefaultRequest(), forespørsel);
        assertThat(result).hasValue(EksponertFeilmelding.MISMATCH_YTELSE);
    }

    @Test
    void skal_godkjenne_gyldig_forespørsel() {
        var result = InntektsmeldingValidererUtil.validerInntektsmeldingMotForespørsel(lagDefaultRequest(), lagDefaultForespørsel());
        assertThat(result).isEmpty();
    }

    @Test
    void skal_avvise_utgått_forespørsel() {
        var forespørsel = lagForespørsel(ForespørselStatus.UTGÅTT, STARTDATO, YtelseType.PLEIEPENGER_SYKT_BARN);
        var result = InntektsmeldingValidererUtil.validerInntektsmeldingMotForespørsel(lagDefaultRequest(), forespørsel);
        assertThat(result).hasValue(EksponertFeilmelding.UGYLDIG_FORESPOERSEL);
    }

    @Test
    void skal_avvise_mismatch_startdato() {
        var forespørsel = lagForespørsel(ForespørselStatus.UNDER_BEHANDLING, STARTDATO.plusDays(5), YtelseType.PLEIEPENGER_SYKT_BARN);
        var result = InntektsmeldingValidererUtil.validerInntektsmeldingMotForespørsel(lagDefaultRequest(), forespørsel);
        assertThat(result).hasValue(EksponertFeilmelding.MISMATCH_SKJAERINGSTIDSPUNKT);
    }

    @Test
    void skal_godkjenne_omsorgspenger_matcher() {
        var request = lagRequest(YtelseType.OMSORGSPENGER,
            new InntektsmeldingRequest.Refusjon(DEFAULT_BELØP, List.of()),
            Collections.emptyList(),
            new InntektsmeldingRequest.InntektInfo(DEFAULT_BELØP,List.of()));
        var forespørsel = lagForespørsel(ForespørselStatus.UNDER_BEHANDLING, STARTDATO, YtelseType.OMSORGSPENGER);
        var result = InntektsmeldingValidererUtil.validerInntektsmeldingMotForespørsel(request, forespørsel);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_ferdig_forespørsel() {
        var forespørsel = lagForespørsel(ForespørselStatus.FERDIG, STARTDATO, YtelseType.PLEIEPENGER_SYKT_BARN);
        var result = InntektsmeldingValidererUtil.validerInntektsmeldingMotForespørsel(lagDefaultRequest(), forespørsel);
        assertThat(result).isEmpty();
    }

    // =====================================================================
    // validerOmsorgspenger
    // =====================================================================

    @Test
    void skal_godkjenne_omsorgspenger_med_kun_hele_dager() {
        var omsorgspenger = new InntektsmeldingRequest.OmsorgspengerInfo(
            true,
            List.of(new InntektsmeldingRequest.OmsorgspengerInfo.FraværHeleDagenPeriode(STARTDATO, STARTDATO.plusDays(2))),
            List.of()
        );
        assertThat(InntektsmeldingValidererUtil.validerOmsorgspengerInfo(omsorgspenger)).isEmpty();
    }

    @Test
    void skal_godkjenne_omsorgspenger_med_kun_deler_av_dagen() {
        var omsorgspenger = new InntektsmeldingRequest.OmsorgspengerInfo(
            false,
            List.of(),
            List.of(new InntektsmeldingRequest.OmsorgspengerInfo.FraværDelerAvDagen(STARTDATO, BigDecimal.valueOf(3.5)))
        );
        assertThat(InntektsmeldingValidererUtil.validerOmsorgspengerInfo(omsorgspenger)).isEmpty();
    }

    @Test
    void skal_avvise_omsorgspenger_uten_fraværsperioder() {
        var omsorgspenger = new InntektsmeldingRequest.OmsorgspengerInfo(true, List.of(), List.of());
        assertThat(InntektsmeldingValidererUtil.validerOmsorgspengerInfo(omsorgspenger))
            .hasValue(EksponertFeilmelding.OMSORGSPENGER_MANGLER_FRAVÆRSPERIODER);
    }

    @Test
    void skal_avvise_fom_etter_tom_i_hele_dager() {
        var omsorgspenger = new InntektsmeldingRequest.OmsorgspengerInfo(
            true,
            List.of(new InntektsmeldingRequest.OmsorgspengerInfo.FraværHeleDagenPeriode(STARTDATO.plusDays(5), STARTDATO)),
            List.of()
        );
        assertThat(InntektsmeldingValidererUtil.validerOmsorgspengerInfo(omsorgspenger))
            .hasValue(EksponertFeilmelding.FRA_DATO_ETTER_TOM);
    }

    @Test
    void skal_avvise_overlappende_hele_dager() {
        var omsorgspenger = new InntektsmeldingRequest.OmsorgspengerInfo(
            true,
            List.of(
                new InntektsmeldingRequest.OmsorgspengerInfo.FraværHeleDagenPeriode(STARTDATO, STARTDATO.plusDays(5)),
                new InntektsmeldingRequest.OmsorgspengerInfo.FraværHeleDagenPeriode(STARTDATO.plusDays(3), STARTDATO.plusDays(8))
            ),
            List.of()
        );
        assertThat(InntektsmeldingValidererUtil.validerOmsorgspengerInfo(omsorgspenger))
            .hasValue(EksponertFeilmelding.OMSORGSPENGER_OVERLAPP_I_HELE_DAGER);
    }

    @Test
    void skal_avvise_duplikate_datoer_i_deler_av_dagen() {
        var omsorgspenger = new InntektsmeldingRequest.OmsorgspengerInfo(
            false,
            List.of(),
            List.of(
                new InntektsmeldingRequest.OmsorgspengerInfo.FraværDelerAvDagen(STARTDATO, BigDecimal.valueOf(2)),
                new InntektsmeldingRequest.OmsorgspengerInfo.FraværDelerAvDagen(STARTDATO, BigDecimal.valueOf(3))
            )
        );
        assertThat(InntektsmeldingValidererUtil.validerOmsorgspengerInfo(omsorgspenger))
            .hasValue(EksponertFeilmelding.OMSORGSPENGER_DUPLIKAT_FRAVAR_DELER_AV_DAGEN);
    }

    @Test
    void skal_avvise_deler_av_dagen_innenfor_hel_dag_periode() {
        var omsorgspenger = new InntektsmeldingRequest.OmsorgspengerInfo(
            true,
            List.of(new InntektsmeldingRequest.OmsorgspengerInfo.FraværHeleDagenPeriode(STARTDATO, STARTDATO.plusDays(5))),
            List.of(new InntektsmeldingRequest.OmsorgspengerInfo.FraværDelerAvDagen(STARTDATO.plusDays(2), BigDecimal.valueOf(4)))
        );
        assertThat(InntektsmeldingValidererUtil.validerOmsorgspengerInfo(omsorgspenger))
            .hasValue(EksponertFeilmelding.OMSORGSPENGER_FRAVAR_DELER_AV_DAGEN_OVERLAPPER_HEL_DAG);
    }

    // =====================================================================
    // validerRefusjon
    // =====================================================================

    @Test
    void skal_godkjenne_null_refusjon() {
        var result = InntektsmeldingValidererUtil.validerRefusjon(null, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_enkel_refusjon_med_startdato() {
        var refusjon = new InntektsmeldingRequest.Refusjon(BigDecimal.valueOf(20000), List.of());
        var result = InntektsmeldingValidererUtil.validerRefusjon(refusjon, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_sammenhengende_refusjonsendringer() {
        var refusjon = new InntektsmeldingRequest.Refusjon(DEFAULT_BELØP, List.of(
            new InntektsmeldingRequest.Refusjon.RefusjonEndring(BigDecimal.valueOf(20000), STARTDATO.plusDays(10)),
            new InntektsmeldingRequest.Refusjon.RefusjonEndring(BigDecimal.valueOf(15000), STARTDATO.plusDays(20)))
        );
        var result = InntektsmeldingValidererUtil.validerRefusjon(refusjon, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_avvise_duplikat_fom_dato() {
        var refusjon = new InntektsmeldingRequest.Refusjon(DEFAULT_BELØP, List.of(
            new InntektsmeldingRequest.Refusjon.RefusjonEndring(BigDecimal.valueOf(20000), STARTDATO.plusDays(10)),
            new InntektsmeldingRequest.Refusjon.RefusjonEndring(BigDecimal.valueOf(15000), STARTDATO.plusDays(10)))
        );
        var result = InntektsmeldingValidererUtil.validerRefusjon(refusjon, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.LIK_START_DATO_REFUSJONSENDRINGER);
    }

    @Test
    void skal_avvise_endring_på_startdato() {
        var refusjon = new InntektsmeldingRequest.Refusjon(DEFAULT_BELØP, List.of(
            new InntektsmeldingRequest.Refusjon.RefusjonEndring(BigDecimal.valueOf(20000), STARTDATO),
            new InntektsmeldingRequest.Refusjon.RefusjonEndring(BigDecimal.valueOf(15000), STARTDATO.plusDays(10)))
        );
        var result = InntektsmeldingValidererUtil.validerRefusjon(refusjon, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.REFUSJON_ENDRING_LIK_STARTDATO);
    }

    @Test
    void skal_godkjenne_usorterte_sammenhengende_refusjoner() {
        var refusjon = new InntektsmeldingRequest.Refusjon(DEFAULT_BELØP, List.of(
            new InntektsmeldingRequest.Refusjon.RefusjonEndring(BigDecimal.valueOf(15000), STARTDATO.plusDays(20)),
            new InntektsmeldingRequest.Refusjon.RefusjonEndring(BigDecimal.valueOf(20000), STARTDATO.plusDays(10)))
        );
        var result = InntektsmeldingValidererUtil.validerRefusjon(refusjon, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_usammenhengende_refusjonsperioder() {
        var refusjon = new InntektsmeldingRequest.Refusjon(DEFAULT_BELØP, List.of(
            new InntektsmeldingRequest.Refusjon.RefusjonEndring(BigDecimal.valueOf(15000), STARTDATO.plusDays(40)),
            new InntektsmeldingRequest.Refusjon.RefusjonEndring(BigDecimal.valueOf(20000), STARTDATO.plusDays(10)))
        );
        var result = InntektsmeldingValidererUtil.validerRefusjon(refusjon, STARTDATO);
        assertThat(result).isEmpty();
    }

    // =====================================================================
    // validerNaturalytelse
    // =====================================================================

    @Test
    void skal_godkjenne_null_naturalytelse() {
        var result = InntektsmeldingValidererUtil.validerNaturalytelse(null);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_tom_liste() {
        var result = InntektsmeldingValidererUtil.validerNaturalytelse(Collections.emptyList());
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_enkel_periode() {
        var perioder = List.of(lagNaturalytelse(STARTDATO, STARTDATO.plusDays(10)));
        var result = InntektsmeldingValidererUtil.validerNaturalytelse(perioder);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_ikke_overlappende_perioder() {
        var perioder = List.of(
            lagNaturalytelse(STARTDATO, STARTDATO.plusDays(10)),
            lagNaturalytelse(STARTDATO.plusDays(12), STARTDATO.plusDays(20))
        );
        var result = InntektsmeldingValidererUtil.validerNaturalytelse(perioder);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_avvise_overlappende_perioder() {
        var perioder = List.of(
            lagNaturalytelse(STARTDATO, STARTDATO.plusDays(10)),
            lagNaturalytelse(STARTDATO.plusDays(5), STARTDATO.plusDays(20))
        );
        var result = InntektsmeldingValidererUtil.validerNaturalytelse(perioder);
        assertThat(result).hasValue(EksponertFeilmelding.OVERLAPP_I_PERIODER);
    }


    @Test
    void skal_avvise_fom_etter_tom() {
        var perioder = List.of(lagNaturalytelse(STARTDATO.plusDays(10), STARTDATO));
        var result = InntektsmeldingValidererUtil.validerNaturalytelse(perioder);
        assertThat(result).hasValue(EksponertFeilmelding.FRA_DATO_ETTER_TOM);
    }

    @Test
    void skal_godkjenne_periode_med_kun_fom() {
        var perioder = List.of(lagNaturalytelse(STARTDATO, null));
        var result = InntektsmeldingValidererUtil.validerNaturalytelse(perioder);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_usammenhengende_perioder() {
        var perioder = List.of(
            lagNaturalytelse(STARTDATO, STARTDATO.plusDays(10)),
            lagNaturalytelse(STARTDATO.plusDays(11), STARTDATO.plusDays(20))
        );
        var result = InntektsmeldingValidererUtil.validerNaturalytelse(perioder);
        assertThat(result).isEmpty();
    }

    private InntektsmeldingRequest.Naturalytelse lagNaturalytelse(LocalDate fom, LocalDate tom) {
        return new InntektsmeldingRequest.Naturalytelse(InntektsmeldingRequest.Naturalytelse.Naturalytelsetype.ELEKTRISK_KOMMUNIKASJON, DEFAULT_BELØP,
            fom, tom);
    }

    // =====================================================================
    // validerEndringsårsaker
    // =====================================================================

    @Test
    void skal_godkjenne_null_endringsårsaker() {
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(null, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_tom_endringsårsak_liste() {
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(Collections.emptyList(), STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_avvise_duplikate_unike_årsaker() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.NY_STILLING, STARTDATO.minusDays(5), null, null),
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.NY_STILLING, STARTDATO.minusDays(3), null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.DUPLIKATER_IKKE_TILATT);
    }

    @Test
    void skal_godkjenne_duplikate_ikke_unike_årsaker() {
        // FERIE, PERMISJON, PERMITTERING, SYKEFRAVÆR er lov å ha flere av
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.FERIE, STARTDATO, STARTDATO.plusDays(5), null),
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.FERIE, STARTDATO.plusDays(10), STARTDATO.plusDays(15), null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_duplikate_permisjon_årsaker() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.PERMISJON, STARTDATO, STARTDATO.plusDays(5), null),
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.PERMISJON, STARTDATO.plusDays(10), STARTDATO.plusDays(15), null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_avvise_duplikat_bonus() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.BONUS, null, null, null),
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.BONUS, null, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.DUPLIKATER_IKKE_TILATT);
    }

    @Test
    void skal_avvise_tariffendring_uten_fom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.TARIFFENDRING, null, null, STARTDATO)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.KREVER_FRA_OG_BLE_KJENT_DATO);
    }

    @Test
    void skal_avvise_tariffendring_uten_ble_kjent_fom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.TARIFFENDRING, STARTDATO, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.KREVER_FRA_OG_BLE_KJENT_DATO);
    }

    @Test
    void skal_avvise_tariffendring_ble_kjent_før_fom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.TARIFFENDRING, STARTDATO, null, STARTDATO.minusDays(1))
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.KREVER_FRA_OG_BLE_KJENT_DATO);
    }

    @Test
    void skal_godkjenne_gyldig_tariffendring() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.TARIFFENDRING, STARTDATO, null, STARTDATO.plusDays(5))
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_tariffendring_ble_kjent_lik_fom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.TARIFFENDRING, STARTDATO, null, STARTDATO)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }


    @Test
    void skal_avvise_ny_stilling_uten_fom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.NY_STILLING, null, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.AARSAK_KREVER_FRA_DATO);
    }

    @Test
    void skal_avvise_ny_stillingsprosent_uten_fom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.NY_STILLINGSPROSENT, null, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.AARSAK_KREVER_FRA_DATO);
    }

    @Test
    void skal_avvise_varig_lønnsendring_uten_fom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.VARIG_LØNNSENDRING, null, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.AARSAK_KREVER_FRA_DATO);
    }

    @Test
    void skal_godkjenne_ny_stilling_med_fom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.NY_STILLING, STARTDATO.minusDays(10), null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_avvise_varig_lønnsendring_fom_etter_startdato() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.VARIG_LØNNSENDRING, STARTDATO.plusDays(1), null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.FRA_DATO_FOER_STARTDATO);
    }

    @Test
    void skal_avvise_varig_lønnsendring_fom_lik_startdato() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.VARIG_LØNNSENDRING, STARTDATO, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.FRA_DATO_FOER_STARTDATO);
    }

    @Test
    void skal_godkjenne_varig_lønnsendring_fom_før_startdato() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.VARIG_LØNNSENDRING, STARTDATO.minusDays(10), null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_avvise_ferie_uten_fom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.FERIE, null, STARTDATO.plusDays(5), null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.AARSAK_KREVER_FRA_OG_TIL_DATO);
    }

    @Test
    void skal_avvise_ferie_uten_tom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.FERIE, STARTDATO, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.AARSAK_KREVER_FRA_OG_TIL_DATO);
    }

    @Test
    void skal_avvise_permittering_uten_datoer() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.PERMITTERING, null, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.AARSAK_KREVER_FRA_OG_TIL_DATO);
    }

    @Test
    void skal_avvise_sykefravær_uten_tom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.SYKEFRAVÆR, STARTDATO, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.AARSAK_KREVER_FRA_OG_TIL_DATO);
    }

    @Test
    void skal_godkjenne_ferie_med_fom_og_tom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.FERIE, STARTDATO, STARTDATO.plusDays(5), null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_permisjon_med_fom_og_tom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.PERMISJON, STARTDATO, STARTDATO.plusDays(10), null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_avvise_fom_etter_tom_for_endringsårsaker() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.FERIE, STARTDATO.plusDays(10), STARTDATO, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.FRA_DATO_ETTER_TOM);
    }

    @Test
    void skal_godkjenne_fom_lik_tom() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.FERIE, STARTDATO, STARTDATO, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_avvise_overlappende_perioder_for_endringsårsaker() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.FERIE, STARTDATO, STARTDATO.plusDays(10), null),
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.PERMISJON, STARTDATO.plusDays(5), STARTDATO.plusDays(15), null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.OVERLAPP_I_PERIODER);
    }

    @Test
    void skal_godkjenne_ikke_overlappende_perioder_for_endringsårsaker() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.FERIE, STARTDATO, STARTDATO.plusDays(5), null),
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.PERMISJON, STARTDATO.plusDays(7), STARTDATO.plusDays(15), null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_bonus_uten_datoer() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.BONUS, null, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_nyansatt_uten_datoer() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.NYANSATT, null, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_ferietrekk_uten_datoer() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.FERIETREKK_ELLER_UTBETALING_AV_FERIEPENGER, null, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    // =====================================================================
    // validerInntektsmelding (orchestration)
    // =====================================================================

    @Test
    void skal_godkjenne_mangelfull_rapportering_uten_datoer() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.MANGELFULL_RAPPORTERING_AORDNING, null, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_inntekt_ikke_rapportert_uten_datoer() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.INNTEKT_IKKE_RAPPORTERT_ENDA_AORDNING, null, null, null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_godkjenne_flere_ulike_årsaker_med_gyldige_datoer() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.NY_STILLING, STARTDATO.minusDays(5), null, null),
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.BONUS, null, null, null),
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.FERIE, STARTDATO, STARTDATO.plusDays(5), null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).isEmpty();
    }

    @Test
    void skal_ikke_godkjenne_flere_ulike_årsaker_hvor_en_har_ugyldig_dato() {
        var årsaker = List.of(
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.NY_STILLING, STARTDATO.minusDays(5), null, null),
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.BONUS, null, null, null),
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.FERIE, STARTDATO.minusDays(5), STARTDATO.plusDays(5), null),
            lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.FERIE, STARTDATO.minusDays(4), STARTDATO.plusDays(5), null)
        );
        var result = InntektsmeldingValidererUtil.validerEndringsårsaker(årsaker, STARTDATO);
        assertThat(result).hasValue(EksponertFeilmelding.OVERLAPP_I_PERIODER);
    }

    private InntektsmeldingRequest.InntektInfo.Endringsårsak lagEndringsårsak(InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType årsak,
                                                                    LocalDate fom, LocalDate tom, LocalDate bleKjentFom) {
        return new InntektsmeldingRequest.InntektInfo.Endringsårsak(årsak, fom, tom, bleKjentFom);
    }

    @Test
    void skal_godkjenne_gyldig_inntektsmelding() {
        var result = InntektsmeldingValidererUtil.validerInntektsmelding(lagDefaultRequest(), lagDefaultForespørsel());
        assertThat(result).isEmpty();
    }


    @Test
    void skal_returnere_feil_fra_naturalytelse_validering() {
        var request = lagRequest(YtelseType.PLEIEPENGER_SYKT_BARN,
            new InntektsmeldingRequest.Refusjon(DEFAULT_BELØP, List.of()),
            List.of(new InntektsmeldingRequest.Naturalytelse(InntektsmeldingRequest.Naturalytelse.Naturalytelsetype.BIL, DEFAULT_BELØP,
                STARTDATO.plusDays(10), STARTDATO)),
            new InntektsmeldingRequest.InntektInfo(DEFAULT_BELØP, List.of()));

        var result = InntektsmeldingValidererUtil.validerInntektsmelding(request, lagDefaultForespørsel());
        assertThat(result).hasValue(EksponertFeilmelding.FRA_DATO_ETTER_TOM);
    }

    @Test
    void skal_returnere_feil_fra_endringsårsak_validering() {
        var request = lagRequest(YtelseType.PLEIEPENGER_SYKT_BARN,
            new InntektsmeldingRequest.Refusjon(DEFAULT_BELØP, List.of()),
            List.of(), new InntektsmeldingRequest.InntektInfo(DEFAULT_BELØP, List.of(new InntektsmeldingRequest.InntektInfo.Endringsårsak(
                InntektsmeldingRequest.InntektInfo.Endringsårsak.EndringsårsakType.NY_STILLING, null, null, null))));

        var result = InntektsmeldingValidererUtil.validerInntektsmelding(request, lagDefaultForespørsel());
        assertThat(result).hasValue(EksponertFeilmelding.AARSAK_KREVER_FRA_DATO);
    }

    // --- Hjelpemetoder for testdata ---
    private static InntektsmeldingRequest lagRequest(YtelseType ytelse,
                                                     InntektsmeldingRequest.Refusjon refusjon,
                                                     List<InntektsmeldingRequest.Naturalytelse> naturalytelser,
                                                     InntektsmeldingRequest.InntektInfo inntektInfo) {
        return new InntektsmeldingRequest(
            DEFAULT_UUID, DEFAULT_FNR, InntektsmeldingValidererUtilTest.STARTDATO, ytelse, inntektInfo,
            refusjon, naturalytelser, new InntektsmeldingRequest.Kontaktinformasjon("Test Person", "99887766"),
            new InntektsmeldingRequest.Avsender("TestSystem", "1.0"), null);
    }

    private static InntektsmeldingRequest lagDefaultRequest() {
        return lagRequest(YtelseType.PLEIEPENGER_SYKT_BARN,
            new InntektsmeldingRequest.Refusjon(DEFAULT_BELØP, List.of()),
            Collections.emptyList(),
            new InntektsmeldingRequest.InntektInfo(DEFAULT_BELØP, List.of()));
    }

    private static Forespørsel lagForespørsel(ForespørselStatus status, LocalDate skjæringstidspunkt, YtelseType ytelseType) {
        return new Forespørsel(DEFAULT_UUID, new Organisasjonsnummer("999999999"), DEFAULT_FNR, skjæringstidspunkt,
            status, ytelseType, List.of(), LocalDateTime.now());
    }

    private static Forespørsel lagDefaultForespørsel() {
        return lagForespørsel(ForespørselStatus.UNDER_BEHANDLING, STARTDATO, YtelseType.PLEIEPENGER_SYKT_BARN);
    }
}
