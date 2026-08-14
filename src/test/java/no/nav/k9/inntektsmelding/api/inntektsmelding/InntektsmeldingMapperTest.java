package no.nav.k9.inntektsmelding.api.inntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.k9.inntektsmelding.api.typer.EndringsårsakDto;
import no.nav.k9.inntektsmelding.api.typer.NaturalytelsetypeDto;
import no.nav.k9.inntektsmelding.api.typer.Organisasjonsnummer;
import no.nav.k9.inntektsmelding.api.typer.YtelseTypeDto;
import no.nav.vedtak.konfig.Tid;

class InntektsmeldingMapperTest {

    private static final UUID TEST_UUID = UUID.randomUUID();
    private static final String FNR = "12345678901";
    private static final String ORGNR = "123456789";
    private static final LocalDate STARTDATO = LocalDate.of(2024, 1, 1);
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2024, 1, 1);
    private static final LocalDateTime INNSENDT_TIDSPUNKT = LocalDateTime.of(2024, 1, 2, 10, 0);
    private static final BigDecimal MÅNEDS_INNTEKT = new BigDecimal("50000.00");
    private static final BigDecimal MÅNEDS_REFUSJON = new BigDecimal("30000.00");

    @Test
    void skal_mappe_grunnleggende_felter() {
        var inntektsmelding = lagInntektsmeldingMedTommeLister();

        var dto = InntektsmeldingMapper.mapTilDto(inntektsmelding);

        assertThat(dto.inntektsmeldingId()).isEqualTo(TEST_UUID);
        assertThat(dto.soekerFnr()).isEqualTo(FNR);
        assertThat(dto.ytelse()).isEqualTo(YtelseTypeDto.PLEIEPENGER_SYKT_BARN);
        assertThat(dto.arbeidsgiver().orgnr()).isEqualTo(ORGNR);
        assertThat(dto.startdato()).isEqualTo(STARTDATO);
        assertThat(dto.innsendtTid()).isEqualTo(INNSENDT_TIDSPUNKT);
        assertThat(dto.arbeidsgiver().kontaktperson().navn()).isEqualTo("Ola Nordmann");
        assertThat(dto.arbeidsgiver().kontaktperson().telefonnummer()).isEqualTo("12345678");
        assertThat(dto.avsender().systemNavn()).isEqualTo("TestSystem");
        assertThat(dto.avsender().systemVersjon()).isEqualTo("1.0");
        assertThat(dto.inntekt().beloep()).isEqualByComparingTo(MÅNEDS_INNTEKT);
        assertThat(dto.inntekt().inntektsdato()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(dto.inntekt().endringAarsaker()).isEmpty();

    }

    @Test
    void skal_mappe_inntekt_endringsårsaker() {
        var endringsårsak = new Inntektsmelding.Endringsårsaker(
            EndringsårsakDto.Bonus,
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 1, 31),
            LocalDate.of(2024, 1, 15)
        );
        var inntektsmelding = lagInntektsmeldingBuilder(List.of(endringsårsak), List.of(), List.of());

        var dto = InntektsmeldingMapper.mapTilDto(inntektsmelding);

        assertThat(dto.inntekt().endringAarsaker()).hasSize(1);
        var mappetÅrsak = dto.inntekt().endringAarsaker().getFirst();
        assertThat(mappetÅrsak.aarsak()).isEqualTo(EndringsårsakDto.Bonus);
        assertThat(mappetÅrsak.fom()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(mappetÅrsak.tom()).isEqualTo(LocalDate.of(2024, 1, 31));
        assertThat(mappetÅrsak.bleKjentFom()).isEqualTo(LocalDate.of(2024, 1, 15));
    }

    @Test
    void skal_mappe_refusjon_uten_opphørsdato() {
        var refusjonsendring = new Inntektsmelding.Refusjon(LocalDate.of(2024, 3, 1), new BigDecimal("20000.00"));
        var inntektsmelding = lagInntektsmeldingBuilder(List.of(), List.of(refusjonsendring), List.of());

        var dto = InntektsmeldingMapper.mapTilDto(inntektsmelding);

        assertThat(dto.refusjon().beloepPerMaaned()).isEqualByComparingTo(MÅNEDS_REFUSJON);
        assertThat(dto.refusjon().endringer()).hasSize(1);
        assertThat(dto.refusjon().endringer().getFirst().beloepPerMaaned()).isEqualByComparingTo(new BigDecimal("20000.00"));
        assertThat(dto.refusjon().endringer().getFirst().fom()).isEqualTo(LocalDate.of(2024, 3, 1));
    }

    @Test
    void skal_mappe_refusjon_med_opphørsdato() {
        var opphørsdato = LocalDate.of(2024, 6, 1);
        var inntektsmelding = lagInntektsmeldingBuilder(List.of(), List.of(), List.of(), opphørsdato, MÅNEDS_REFUSJON);

        var dto = InntektsmeldingMapper.mapTilDto(inntektsmelding);

        // Opphørsdato skal legges til som en ekstra RefusjonEndring med beloepPerMaaned 0
        assertThat(dto.refusjon().endringer()).hasSize(1);
        var opphørEndring = dto.refusjon().endringer().getFirst();
        assertThat(opphørEndring.beloepPerMaaned()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(opphørEndring.fom()).isEqualTo(opphørsdato);
    }

    @Test
    void skal_mappe_refusjon_med_endringer_og_opphørsdato() {
        var opphørsdato = LocalDate.of(2024, 6, 1);
        var refusjonsendring = new Inntektsmelding.Refusjon(LocalDate.of(2024, 3, 1), new BigDecimal("20000.00"));
        var inntektsmelding = lagInntektsmeldingBuilder(List.of(), List.of(refusjonsendring), List.of(), opphørsdato, MÅNEDS_REFUSJON);

        var dto = InntektsmeldingMapper.mapTilDto(inntektsmelding);

        // Skal ha den originale endringen pluss opphøret
        assertThat(dto.refusjon().endringer()).hasSize(2);
        assertThat(dto.refusjon().endringer().get(0).beloepPerMaaned()).isEqualByComparingTo(new BigDecimal("20000.00"));
        assertThat(dto.refusjon().endringer().get(1).beloepPerMaaned()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.refusjon().endringer().get(1).fom()).isEqualTo(opphørsdato);
    }

    @Test
    void skal_mappe_naturalytelser() {
        var naturalytelse = new Inntektsmelding.BortfaltNaturalytelse(
            LocalDate.of(2024, 2, 1),
            LocalDate.of(2024, 2, 28),
            NaturalytelsetypeDto.Bil,
            new BigDecimal("3000.00")
        );
        var inntektsmelding = lagInntektsmeldingBuilder(List.of(), List.of(), List.of(naturalytelse));

        var dto = InntektsmeldingMapper.mapTilDto(inntektsmelding);

        assertThat(dto.naturalytelser()).hasSize(1);
        var mappetNaturalytelse = dto.naturalytelser().getFirst();
        assertThat(mappetNaturalytelse.verdi()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(mappetNaturalytelse.sluttdato()).isEqualTo(LocalDate.of(2024, 2, 1));
        assertThat(mappetNaturalytelse.naturalytelse()).isEqualTo(NaturalytelsetypeDto.Bil);
    }

    @Test
    void skal_mappe_flere_naturalytelser() {
        var naturalytelse1 = new Inntektsmelding.BortfaltNaturalytelse(
            LocalDate.of(2024, 2, 1),
            LocalDate.of(2024, 2, 28),
            NaturalytelsetypeDto.Bil,
            new BigDecimal("3000.00")
        );
        var naturalytelse2 = new Inntektsmelding.BortfaltNaturalytelse(
            LocalDate.of(2024, 3, 1),
            null,
            NaturalytelsetypeDto.ElektroniskKommunikasjon,
            new BigDecimal("500.00")
        );
        var inntektsmelding = lagInntektsmeldingBuilder(List.of(), List.of(), List.of(naturalytelse1, naturalytelse2));

        var dto = InntektsmeldingMapper.mapTilDto(inntektsmelding);

        assertThat(dto.naturalytelser()).hasSize(2);
        assertThat(dto.naturalytelser().get(0).naturalytelse()).isEqualTo(NaturalytelsetypeDto.Bil);
        assertThat(dto.naturalytelser().get(1).naturalytelse()).isEqualTo(NaturalytelsetypeDto.ElektroniskKommunikasjon);
    }

    @Test
    void skal_returnere_tomme_lister_når_ingen_data() {
        var inntektsmelding = lagInntektsmeldingMedTommeLister();

        var dto = InntektsmeldingMapper.mapTilDto(inntektsmelding);

        assertThat(dto.inntekt().endringAarsaker()).isEmpty();
        assertThat(dto.refusjon().endringer()).isEmpty();
        assertThat(dto.naturalytelser()).isEmpty();
    }

    @Test
    void skal_returnere_tomme_lister_når_lister_er_null() {
        var inntektsmelding = lagInntektsmeldingBuilder(null, null, null, null, null);

        var dto = InntektsmeldingMapper.mapTilDto(inntektsmelding);

        assertThat(dto.inntekt().endringAarsaker()).isEmpty();
        assertThat(dto.refusjon().endringer()).isEmpty();
        assertThat(dto.naturalytelser()).isEmpty();
    }

    private Inntektsmelding lagInntektsmeldingMedTommeLister() {
        return lagInntektsmeldingBuilder(List.of(), List.of(), List.of(), null, null);
    }
    private Inntektsmelding lagInntektsmeldingBuilder(
        List<Inntektsmelding.Endringsårsaker> endringsårsaker,
        List<Inntektsmelding.Refusjon> refusjonsendringer,
        List<Inntektsmelding.BortfaltNaturalytelse> naturalytelser){
         return lagInntektsmeldingBuilder(endringsårsaker, refusjonsendringer, naturalytelser, Tid.TIDENES_ENDE, MÅNEDS_REFUSJON);
    }

    private Inntektsmelding lagInntektsmeldingBuilder(
        List<Inntektsmelding.Endringsårsaker> endringsårsaker,
        List<Inntektsmelding.Refusjon> refusjonsendringer,
        List<Inntektsmelding.BortfaltNaturalytelse> naturalytelser,
        LocalDate opphørsdatoRefusjon,
        BigDecimal refusjonPrMnd
    ) {
        return new Inntektsmelding(
            TEST_UUID,
            FNR,
            YtelseTypeDto.PLEIEPENGER_SYKT_BARN,
            new Organisasjonsnummer(ORGNR),
            new Inntektsmelding.Kontaktperson("Ola Nordmann", "12345678"),
            STARTDATO,
            MÅNEDS_INNTEKT,
            SKJÆRINGSTIDSPUNKT,
            INNSENDT_TIDSPUNKT,
            new Inntektsmelding.AvsenderSystem("TestSystem", "1.0"),
            refusjonPrMnd,
            opphørsdatoRefusjon,
            refusjonsendringer,
            naturalytelser,
            endringsårsaker,
            null
        );
    }
}

