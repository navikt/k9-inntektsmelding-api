package no.nav.k9.inntektsmelding.api.tjenester.eksterne.responses;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import no.nav.vedtak.mapper.json.DefaultJsonMapper;

class InntektDtoSerialiseringTest {

    @Test
    void skal_serialisere_måned_uten_rapportert_inntekt_som_null_og_rapportert_null_inntekt_som_0() {
        Map<YearMonth, BigDecimal> inntektPerMåned = new LinkedHashMap<>();
        inntektPerMåned.put(YearMonth.of(2025, 1), BigDecimal.valueOf(30000));
        inntektPerMåned.put(YearMonth.of(2025, 2), BigDecimal.ZERO);
        inntektPerMåned.put(YearMonth.of(2025, 3), null);

        var json = DefaultJsonMapper.toJson(new InntektDto(inntektPerMåned, BigDecimal.valueOf(10000)));

        assertThat(json).contains("\"2025-01\":30000", "\"2025-02\":0", "\"2025-03\":null", "\"gjennomsnittAvMaaneder\":10000");
    }
}
