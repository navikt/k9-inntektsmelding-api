package no.nav.k9.inntektsmelding.api.tjenester.eksterne.responses;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Inntekt rapportert til A-ordningen per måned, og gjennomsnittlig månedsinntekt")
public record InntektDto(
    @Schema(description = """
        Inntekt per måned (format YYYY-MM), rapportert til A-ordningen av arbeidsgiveren i forespørselen.
        - Et tall, også `0`, betyr at det er rapportert inntekt for måneden. `0` betyr at det er rapportert en inntekt på 0 kr.
        - `null` betyr at det ikke er rapportert inntekt for måneden, for eksempel fordi rapporteringsfristen ikke er passert
          eller arbeidstakeren er nyansatt. Måneden er likevel med i responsen.""",
        example = "{\"2025-01\": 30000, \"2025-02\": 0, \"2025-03\": null}")
    // Måneder uten rapportert inntekt skal serialiseres som null, og ikke utelates
    @JsonInclude(content = JsonInclude.Include.ALWAYS)
    @NotNull Map<YearMonth, BigDecimal> inntektPerMaaned,
    @Schema(description = "Gjennomsnittlig månedsinntekt. Er alltid satt, og er `0` hvis ingen inntekt er rapportert.",
        example = "10000")
    @NotNull BigDecimal gjennomsnittAvMaaneder) {

}
