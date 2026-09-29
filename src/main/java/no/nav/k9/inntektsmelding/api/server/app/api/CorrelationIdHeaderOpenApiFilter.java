package no.nav.k9.inntektsmelding.api.server.app.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.swagger.v3.core.filter.AbstractSpecFilter;
import io.swagger.v3.core.model.ApiDescription;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import no.nav.k9.inntektsmelding.api.server.auth.AutentiseringFilter;

public class CorrelationIdHeaderOpenApiFilter extends AbstractSpecFilter {

    static final String CORRELATION_HEADER_NAME = AutentiseringFilter.X_CORRELATION_ID;
    static final String CORRELATION_PARAMETER_COMPONENT = "CorrelationId";
    static final String CORRELATION_BESKRIVELSE = "ID brukt til å korrelere logger. Anbefalt format: UUID";

    private static final String CORRELATION_PARAMETER_REF = "#/components/parameters/" + CORRELATION_PARAMETER_COMPONENT;

    @Override
    public Optional<Operation> filterOperation(Operation operation,
                                               ApiDescription api,
                                               Map<String, List<String>> params,
                                               Map<String, String> cookies,
                                               Map<String, List<String>> headers) {
        if (operation == null) {
            return Optional.empty();
        }

        var existingParameters = operation.getParameters();
        if (harCorrelationHeader(existingParameters)) {
            return Optional.of(operation);
        }

        var parameters = existingParameters == null ? new ArrayList<Parameter>() : new ArrayList<>(existingParameters);
        parameters.add(new Parameter().$ref(CORRELATION_PARAMETER_REF));
        operation.setParameters(parameters);
        return Optional.of(operation);
    }

    private boolean harCorrelationHeader(List<Parameter> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return false;
        }

        return parameters.stream().anyMatch(parameter ->
            CORRELATION_PARAMETER_REF.equals(parameter.get$ref())
                || CORRELATION_HEADER_NAME.equalsIgnoreCase(parameter.getName()));
    }
}
