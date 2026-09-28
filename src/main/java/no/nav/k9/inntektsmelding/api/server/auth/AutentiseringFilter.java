package no.nav.k9.inntektsmelding.api.server.auth;

import java.util.Optional;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.k9.inntektsmelding.api.server.app.api.OpenApiRest;
import no.nav.k9.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.k9.inntektsmelding.api.server.exceptions.InntektsmeldingAPIException;
import no.nav.vedtak.log.mdc.MDCOperations;
import no.nav.vedtak.sikkerhet.jaxrs.AuthenticationFilterDelegate;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.oidc.token.TokenString;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AutentiseringFilter implements ContainerRequestFilter, ContainerResponseFilter {
    public static final String X_CORRELATION_ID = "X-Correlation-Id";

    private static final Logger LOG = LoggerFactory.getLogger(AutentiseringFilter.class);
    private static final Environment ENV = Environment.current();
    private final AuthTjeneste authTjeneste;

    @Context
    private ResourceInfo resourceinfo;

    public AutentiseringFilter() {
        this.authTjeneste = new AuthTjeneste();
    }

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) {
        AuthenticationFilterDelegate.fjernKontekst();
        var callId = MDCOperations.getCallId();
        if (callId != null) {
            res.getHeaders().add(X_CORRELATION_ID, callId);
        }
    }

    @Override
    public void filter(ContainerRequestContext req) {
        // Swagger UI er kun tilgjengelig utenfor prod og trenger tilgang uten autentisering
        if (!ENV.isProd() && OpenApiRest.class.equals(getResourceinfo().getResourceClass())) {
            return;
        }
        assertValidRequest(req);
    }

    void assertValidRequest(ContainerRequestContext req) {
        var method = getResourceinfo().getResourceMethod();
        Optional<TokenString> tokenFromHeader = AuthenticationFilterDelegate.getTokenFromHeader(req);

        var correlationId = req.getHeaderString(X_CORRELATION_ID);
        if (correlationId == null || correlationId.isEmpty()) {
            MDCOperations.putCallId(MDCOperations.generateCallId());
        } else {
            MDCOperations.putCallId(correlationId);
        }

        if (tokenFromHeader.isEmpty()) {
            throw new InntektsmeldingAPIException(EksponertFeilmelding.MANGLER_TOKEN, Response.Status.UNAUTHORIZED);
        }

        LOG.trace("{} i klasse {}", method.getName(), method.getDeclaringClass());
        fjernKontekstHvisFinnes();
        authTjeneste.validerOgSettKontekst(tokenFromHeader.get());
    }

    private void fjernKontekstHvisFinnes() {
        if (KontekstHolder.harKontekst()) {
            LOG.info("Kall til {} hadde kontekst {}", getResourceinfo().getResourceMethod().getName(), KontekstHolder.getKontekst().getKompaktUid());
            KontekstHolder.fjernKontekst();
            MDC.clear();
        }
    }

    private ResourceInfo getResourceinfo() {
        return resourceinfo;
    }
}
