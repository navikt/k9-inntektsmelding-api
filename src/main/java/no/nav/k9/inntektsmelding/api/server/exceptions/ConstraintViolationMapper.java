package no.nav.k9.inntektsmelding.api.server.exceptions;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import no.nav.vedtak.server.rest.RestSecureLogFeature;
import no.nav.vedtak.server.rest.RestServerFeilUtils;
import no.nav.vedtak.server.rest.ValidationExceptionMapper;

public class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        RestServerFeilUtils.ensureCallId();
        var feilmelding = ValidationExceptionMapper.getFeilmeldingTekst(exception);
        RestServerFeilUtils.loggWarning(feilmelding);
        if (RestSecureLogFeature.erSikkerloggEnabled()) {
            RestSecureLogFeature.sikkerloggWarning(String.format("%s - input %s", feilmelding, ValidationExceptionMapper.getInputs(exception)));
        }
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(new ErrorResponse(EksponertFeilmelding.VALIDERINGSFEIL.name(), feilmelding)).type(MediaType.APPLICATION_JSON)
            .build();
    }

}
