package no.nav.k9.inntektsmelding.api.server.app.api;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import no.nav.k9.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.k9.inntektsmelding.api.server.exceptions.ErrorResponse;
import no.nav.vedtak.log.util.LoggerUtils;
import no.nav.vedtak.server.rest.RestServerFeilUtils;
import tools.jackson.core.JacksonException;

public class Jackson3ExceptionMapper implements ExceptionMapper<JacksonException> {

    @Override
    public Response toResponse(JacksonException exception) {
        RestServerFeilUtils.ensureCallId();
        var feilmelding = LoggerUtils.removeLineBreaks(exception.getMessage());
        RestServerFeilUtils.loggWarning("FIM-252294: JSON-feil: " + feilmelding);

        return Response.status(Response.Status.BAD_REQUEST)
            .entity(new ErrorResponse(
                EksponertFeilmelding.SERIALISERINGSFEIL.name(),
                EksponertFeilmelding.SERIALISERINGSFEIL.getTekst() + ": " + feilmelding,
                null))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

}
