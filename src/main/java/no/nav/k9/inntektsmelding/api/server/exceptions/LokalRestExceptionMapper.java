package no.nav.k9.inntektsmelding.api.server.exceptions;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import no.nav.vedtak.server.rest.RestServerFeilUtils;

/**
 * Vi ønsker ikke eksponere detaljerte feilmeldinger frontend. Vi spesialbehandler tilgangsmangel, ellers får alle en generell melding om serverfeil.
 * Legger alltid ved callId så frontend kan vise denne og vi kan finne den igjen i loggene hvis arbeidsgiver melder den inn.
 */
@Provider
public class LokalRestExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable feil) {
        RestServerFeilUtils.loggFeil(feil);
        if (feil instanceof InntektsmeldingAPIException ex) {
            return Response.status(ex.getStatus())
                .entity(new ErrorResponse(ex.getFeilmelding().name(), ex.getFeilmelding().getTekst()))
                .type(MediaType.APPLICATION_JSON)
                .build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(new ErrorResponse(EksponertFeilmelding.STANDARD_FEIL.name(), EksponertFeilmelding.STANDARD_FEIL.getTekst()))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }
}
