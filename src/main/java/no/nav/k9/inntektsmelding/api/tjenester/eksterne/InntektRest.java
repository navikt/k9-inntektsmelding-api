package no.nav.k9.inntektsmelding.api.tjenester.eksterne;

import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import no.nav.k9.inntektsmelding.api.forespørsel.Forespørsel;
import no.nav.k9.inntektsmelding.api.integrasjoner.K9inntektsmeldingTjeneste;
import no.nav.k9.inntektsmelding.api.server.auth.Tilgang;

import no.nav.k9.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.k9.inntektsmelding.api.server.exceptions.ErrorResponse;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.responses.InntektDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Path(InntektRest.BASE_PATH)
@Tag(name = "Inntekt")
public class InntektRest {
    public static final String BASE_PATH = "/inntekt";
    private static final String HENT_INNTEKT = "/{forespoerselId}";
    private static final Logger LOG = LoggerFactory.getLogger(InntektRest.class);
    private K9inntektsmeldingTjeneste k9InntektsmeldingTjeneste;
    private Tilgang tilgang;

    InntektRest() {
        // for CDI
    }

    @Inject
    public InntektRest(K9inntektsmeldingTjeneste k9InntektsmeldingTjeneste, Tilgang tilgang) {
        this.k9InntektsmeldingTjeneste = k9InntektsmeldingTjeneste;
        this.tilgang = tilgang;
    }

    @GET
    @Path(HENT_INNTEKT)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(summary = "Hent inntekt", description = """
        Henter innrapportert inntekt for de siste tre månedene (basert på skjæringstidspunktet til forespørselen) og beregnet         gjennomsnittsinntekt, gitt en forespørselId.

        Skille mellom `0` og `null` i `inntektPerMaaned`:
        - `0` betyr at arbeidsgiver har rapportert en inntekt på 0 kr for måneden.
        - `null` betyr at det ikke er rapportert inntekt for måneden (ennå). Måneden er likevel med i responsen.

        Hvis forespørselen ikke finnes, svarer endepunktet 404 med feilkode `TOM_FORESPOERSEL`.
        Hvis inntekt ikke kan hentes fra A-ordningen, for eksempel ved nedetid, svarer endepunktet 404 med feilkode `INNTEKT_IKKE_TILGJENGELIG`.
        Da kan kallet prøves igjen senere.""")
    @ApiResponse(responseCode = "200", description = "Inntekt med gjennomsnitt. `0` = rapportert inntekt på 0 kr, `null` = ikke rapportert inntekt.",
        content = @Content(schema = @Schema(implementation = InntektDto.class)))
    @ApiResponse(responseCode = "400", description = "Ugyldig UUID-format",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Mangler gyldig autentisering",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Ikke tilgang til oppgitt organisasjon")
    @ApiResponse(responseCode = "404", description = "`TOM_FORESPOERSEL`: forespørselen ble ikke funnet. `INNTEKT_IKKE_TILGJENGELIG`: inntekt kunne ikke hentes fra A-ordningen, prøv igjen senere",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "Intern serverfeil",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response hentInntekt(@NotNull @Valid @PathParam("forespoerselId")
                                @Parameter(description = "UUID til forespørselen")
                                @Pattern(regexp = "^[a-fA-F\\d]{8}(?:-[a-fA-F\\d]{4}){3}-[a-fA-F\\d]{12}$", message = "Ugyldig UUID-format") String forespoerselId) {
        LOG.info("Innkomende kall på API for hent inntekt {}", forespoerselId);
        var uuid = UUID.fromString(forespoerselId);

        Forespørsel forespørsel = k9InntektsmeldingTjeneste.hentForespørsel(uuid);
        if (forespørsel == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse(EksponertFeilmelding.TOM_FORESPOERSEL.name(), EksponertFeilmelding.TOM_FORESPOERSEL.getTekst() + ": " + forespoerselId,
                    forespoerselId))
                .build();
        }

        tilgang.sjekkAtSystemHarTilgangTilOrganisasjon(forespørsel.orgnummer());

        var inntekt = k9InntektsmeldingTjeneste.hentInntekt(uuid);
        if (inntekt == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse(EksponertFeilmelding.INNTEKT_IKKE_TILGJENGELIG.name(),
                    EksponertFeilmelding.INNTEKT_IKKE_TILGJENGELIG.getTekst() + ": " + forespoerselId, forespoerselId))
                .build();
        }

        // NB: InntektDto sitt felt heter "gjennomsnittAvMaaneder" for å samsvare med kontrakten til
        // sykepenger-api sitt tilsvarende /v1/inntekt-endepunkt.
        var dto = new InntektDto(inntekt.inntektPerMåned(), inntekt.gjennomsnitt());
        return Response.ok(dto).build();
    }
}
