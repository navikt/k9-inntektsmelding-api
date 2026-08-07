package no.nav.k9.inntektsmelding.api.tjenester.eksterne;

import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.k9.inntektsmelding.api.inntektsmelding.InntektsmeldingMapper;
import no.nav.k9.inntektsmelding.api.integrasjoner.K9inntektsmeldingTjeneste;
import no.nav.k9.inntektsmelding.api.server.auth.Tilgang;
import no.nav.k9.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.k9.inntektsmelding.api.server.exceptions.ErrorResponse;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.responses.InntektsmeldingDto;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.InntektsmeldingFilter;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.InntektsmeldingRequest;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.requests.RefusjonskravOmsorgspengerRequest;
import no.nav.k9.inntektsmelding.api.typer.Organisasjonsnummer;
import no.nav.k9.inntektsmelding.felles.FeilkodeDto;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(InntektsmeldingRest.BASE_PATH)
@Tag(name = "Inntektsmelding")
public class InntektsmeldingRest {
    public static final String BASE_PATH = "/inntektsmelding";
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingRest.class);
    private static final String SEND_INNTEKTSMELDING = "/send-inn";
    private static final String SEND_REFUSJONSKRAV_OMSORGSPENGER = "/refusjonskrav-omsorgspenger/send";
    private static final String HENT_INNTEKTSMELDING = "/hent/{inntektsmeldingId}";
    private static final String HENT_INNTEKTSMELDINGER = "/hent/inntektsmeldinger";
    private static final Environment ENV = Environment.current();
    private K9inntektsmeldingTjeneste k9inntektsmeldingTjeneste;
    private Tilgang tilgang;
    private boolean apiEnabled;

    InntektsmeldingRest() {
        // for CDI proxy
    }

    @Inject
    public InntektsmeldingRest(K9inntektsmeldingTjeneste k9inntektsmeldingTjeneste, Tilgang tilgang) {
        this.k9inntektsmeldingTjeneste = k9inntektsmeldingTjeneste;
        this.tilgang = tilgang;
        this.apiEnabled = ENV.getProperty("inntektsmelding-api.enabled", Boolean.class, true);
    }

    @POST
    @Path(SEND_INNTEKTSMELDING)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Send inn inntektsmelding",
        description = "Sender inn en inntektsmelding for en gitt forespørsel. Inntekten valideres mot A-inntekt og duplikater avvises.")
    @ApiResponse(responseCode = "200", description = "Inntektsmeldingen ble mottatt. Returnerer UUID til den innsendte inntektsmeldingen.",
        content = @Content(schema = @Schema(implementation = java.util.UUID.class)))
    @ApiResponse(responseCode = "400", description = "Valideringsfeil eller ugyldig inntektsmelding (f.eks. inntekt avviker fra A-inntekt uten endringsårsak)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Mangler gyldig autentisering",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Ikke tilgang til oppgitt organisasjon")
    @ApiResponse(responseCode = "404", description = "Forespørselen ble ikke funnet",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Duplikat – inntektsmelding er identisk med siste innsendte",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "A-inntekt er midlertidig utilgjengelig. Prøv igjen om litt.",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "Intern serverfeil",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response sendInntektsmelding(@Valid @NotNull InntektsmeldingRequest inntektsmeldingRequest) {
        if (!apiEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(new ErrorResponse("API_IKKE_AKTIVERT", "API er ikke aktivert"))
                .build();
        }
        var forespørselUuid = inntektsmeldingRequest.forespoerselId();
        LOG.info("Mottatt inntektsmelding for forespørselUuid {} ", forespørselUuid);
        var forespørsel = k9inntektsmeldingTjeneste.hentForespørsel(forespørselUuid);

        if (forespørsel == null) {
            LOG.info("Avvist inntektsmelding for forespørselUuid {}. Forespørsel ikke funnet.", forespørselUuid);
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse(EksponertFeilmelding.TOM_FORESPOERSEL.name(),
                    EksponertFeilmelding.TOM_FORESPOERSEL.getTekst() + ": " + forespørselUuid,
                    forespørselUuid.toString()))
                .build();
        }

        if (!forespørsel.fødselsnummer().equals(inntektsmeldingRequest.soekerFnr())) {
            LOG.info("Avvist inntektsmelding for forespørselUuid {}. Forespørsel og inntektsmelding har ikke samme fødselsnummer.", forespørselUuid);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(EksponertFeilmelding.MISMATCH_FOEDSELSNUMMER.name(),
                    EksponertFeilmelding.MISMATCH_FOEDSELSNUMMER.getTekst(),
                    forespørselUuid.toString()))
                .build();
        }

        tilgang.sjekkAtSystemHarTilgangTilOrganisasjon(new Organisasjonsnummer(forespørsel.orgnummer().orgnr()));

        var feilmelding = InntektsmeldingValidererUtil.validerInntektsmelding(inntektsmeldingRequest, forespørsel);
        if (feilmelding.isPresent()) {
            LOG.info("Avvist inntektsmelding for forespørselUuid {}. Validering av inntektsmelding feilet. Feilmelding: {}",
                inntektsmeldingRequest.forespoerselId(), feilmelding.get().getTekst());
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(feilmelding.get().name(), feilmelding.get().getTekst(), forespørselUuid.toString()))
                .build();
        }
        var response = k9inntektsmeldingTjeneste.sendInntektsmelding(inntektsmeldingRequest, forespørsel);

        if (response.success()) {
            return Response.ok(response.inntektsmeldingUuid()).build();
        } else {
            var errorResponse = new ErrorResponse(response.feilinformasjon().feilkode().name(),
                response.feilinformasjon().feilmelding(),
                response.feilinformasjon().referanseId());

            if (FeilkodeDto.DUPLIKAT.equals(response.feilinformasjon().feilkode())) {
                return Response.status(Response.Status.CONFLICT)
                    .entity(errorResponse)
                    .build();
            } else if (FeilkodeDto.NEDETID_AINNTEKT.equals(response.feilinformasjon().feilkode())) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(errorResponse)
                    .build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorResponse)
                    .build();
            }
        }
    }

    @POST
    @Path(SEND_REFUSJONSKRAV_OMSORGSPENGER)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Send inn refusjonskrav for omsorgspenger",
        description = "Sender inn et refusjonskrav for omsorgspenger. Krever ingen forespørsel siden refusjonskrav for omsorgspenger ikke er knyttet til en forespørsel.")
    @ApiResponse(responseCode = "200", description = "Refusjonskravet ble mottatt. Returnerer UUID til det innsendte refusjonskravet.",
        content = @Content(schema = @Schema(implementation = java.util.UUID.class)))
    @ApiResponse(responseCode = "400", description = "Valideringsfeil eller ugyldig refusjonskrav",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Mangler gyldig autentisering",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Ikke tilgang til oppgitt organisasjon")
    @ApiResponse(responseCode = "409", description = "Duplikat – refusjonskrav er identisk med siste innsendte",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "A-inntekt er midlertidig utilgjengelig. Prøv igjen om litt.",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "Intern serverfeil",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response sendRefusjonskravOmsorgspenger(@Valid @NotNull RefusjonskravOmsorgspengerRequest refusjonskravRequest) {
        if (!apiEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(new ErrorResponse("API_IKKE_AKTIVERT", "API er ikke aktivert"))
                .build();
        }
        LOG.info("Mottatt refusjonskrav for omsorgspenger for orgnr {}", new Organisasjonsnummer(refusjonskravRequest.orgnr()));

        tilgang.sjekkAtSystemHarTilgangTilOrganisasjon(new Organisasjonsnummer(refusjonskravRequest.orgnr()));

        var feilmelding = InntektsmeldingValidererUtil.validerRefusjonskravOmsorgspenger(refusjonskravRequest);
        if (feilmelding.isPresent()) {
            LOG.info("Avvist refusjonskrav for omsorgspenger for orgnr {}. Validering feilet. Feilmelding: {}",
                new Organisasjonsnummer(refusjonskravRequest.orgnr()), feilmelding.get().getTekst());
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(feilmelding.get().name(), feilmelding.get().getTekst()))
                .build();
        }

        var response = k9inntektsmeldingTjeneste.sendRefusjonOmsorgspenger(refusjonskravRequest);

        if (response.success()) {
            return Response.ok(response.inntektsmeldingUuid()).build();
        } else {
            var errorResponse = new ErrorResponse(response.feilinformasjon().feilkode().name(),
                response.feilinformasjon().feilmelding(),
                response.feilinformasjon().referanseId());

            if (FeilkodeDto.DUPLIKAT.equals(response.feilinformasjon().feilkode())) {
                return Response.status(Response.Status.CONFLICT)
                    .entity(errorResponse)
                    .build();
            } else if (FeilkodeDto.NEDETID_AINNTEKT.equals(response.feilinformasjon().feilkode())) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(errorResponse)
                    .build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorResponse)
                    .build();
            }
        }
    }

    @GET
    @Path(HENT_INNTEKTSMELDING)
    @Operation(summary = "Hent inntektsmelding", description = "Henter en spesifikk inntektsmelding basert på inntektsmeldingId.")
    @ApiResponse(responseCode = "200", description = "Inntektsmeldingen ble funnet",
        content = @Content(schema = @Schema(implementation = InntektsmeldingDto.class)))
    @ApiResponse(responseCode = "400", description = "Ugyldig UUID-format",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Mangler gyldig autentisering",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Ikke tilgang til oppgitt organisasjon")
    @ApiResponse(responseCode = "404", description = "Inntektsmeldingen ble ikke funnet",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "Intern serverfeil",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response hentInntektsmelding(@NotNull @Valid @PathParam("inntektsmeldingId")
                                        @Parameter(description = "UUID til inntektsmeldingen (inntektsmeldingId)")
                                        @Pattern(regexp = "^[a-fA-F\\d]{8}(?:-[a-fA-F\\d]{4}){3}-[a-fA-F\\d]{12}$", message = "Ugyldig UUID-format")
                                        String inntektsmeldingId) {
        if (!apiEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(new ErrorResponse("API_IKKE_AKTIVERT", "API er ikke aktivert"))
                .build();
        }
        LOG.info("Hent inntektsmelding med inntektsmeldingId {} ", inntektsmeldingId);
        var inntektsmelding = k9inntektsmeldingTjeneste.hentInntektsmelding(UUID.fromString(inntektsmeldingId));

        if (inntektsmelding == null) {
            LOG.info("Avvist inntektsmelding for inntektsmeldingId {}. Inntektsmelding ikke funnet.", inntektsmeldingId);
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse(EksponertFeilmelding.TOM_INNTEKTSMELDING.name(),
                    EksponertFeilmelding.TOM_INNTEKTSMELDING.getTekst() + ": " + inntektsmeldingId,
                    inntektsmeldingId))
                .build();
        }

        tilgang.sjekkAtSystemHarTilgangTilOrganisasjon(new Organisasjonsnummer(inntektsmelding.orgnr().orgnr()));

        var dto = InntektsmeldingMapper.mapTilDto(inntektsmelding);

        return Response.status(Response.Status.OK)
            .entity(dto)
            .build();
    }

    @POST
    @Path(HENT_INNTEKTSMELDINGER)
    @Operation(summary = "Hent inntektsmeldinger", description = "Filtrer inntektsmeldinger på orgnr, soekerFnr, forespørselId, inntektsmeldingId, ytelseType og/eller dato inntektsmeldingen ble mottatt av NAV.")
    @ApiResponse(responseCode = "200", description = "Liste med inntektsmeldinger som matcher filteret",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = InntektsmeldingDto.class))))
    @ApiResponse(responseCode = "400", description = "Ugyldig periode (fom er etter tom)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Mangler gyldig autentisering",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Ikke tilgang til oppgitt organisasjon")
    @ApiResponse(responseCode = "500", description = "Intern serverfeil",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response hentInntektsmeldinger(@NotNull @Valid InntektsmeldingFilter inntektsmeldingFilter) {
        if (!apiEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(new ErrorResponse("API_IKKE_AKTIVERT", "API er ikke aktivert"))
                .build();
        }
        LOG.info("Innkomende kall på søk etter inntektsmeldinger");
        tilgang.sjekkAtSystemHarTilgangTilOrganisasjon(new Organisasjonsnummer(inntektsmeldingFilter.orgnr()));
        if (inntektsmeldingFilter.inntektsmeldingId() != null) {
            var inntektsmelding = k9inntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingFilter.inntektsmeldingId());
            if (inntektsmelding == null) {
                LOG.info("Inntektsmelding med inntektsmeldingId {} ikke funnet.", inntektsmeldingFilter.inntektsmeldingId());
                return Response.ok(new ErrorResponse(EksponertFeilmelding.TOM_INNTEKTSMELDING.name(), EksponertFeilmelding.TOM_INNTEKTSMELDING.getTekst(), inntektsmeldingFilter.inntektsmeldingId().toString())).build();
            }
            if (datoerErUgyldige(inntektsmeldingFilter)) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(EksponertFeilmelding.UGYLDIG_PERIODE.name(), EksponertFeilmelding.UGYLDIG_PERIODE.getTekst()))
                    .build();
            }

            var dto = InntektsmeldingMapper.mapTilDto(inntektsmelding);

            return Response.status(Response.Status.OK)
                .entity(dto)
                .build();
        }

        var inntektsmeldinger = k9inntektsmeldingTjeneste.hentInntektsmeldinger(inntektsmeldingFilter.orgnr(),
            inntektsmeldingFilter.soekerFnr(),
            inntektsmeldingFilter.forespoerselId(),
            inntektsmeldingFilter.ytelseType(),
            inntektsmeldingFilter.fom(),
            inntektsmeldingFilter.tom());

        var dto = inntektsmeldinger.stream().map(InntektsmeldingMapper::mapTilDto).toList();

        return Response.status(Response.Status.OK)
            .entity(dto)
            .build();
    }

    private boolean datoerErUgyldige(InntektsmeldingFilter filterRequest) {
        return filterRequest.fom() != null && filterRequest.tom() != null && filterRequest.fom().isAfter(filterRequest.tom());
    }
}



