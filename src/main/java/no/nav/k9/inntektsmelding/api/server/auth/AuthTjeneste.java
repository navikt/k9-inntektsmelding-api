package no.nav.k9.inntektsmelding.api.server.auth;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.k9.inntektsmelding.api.server.exceptions.InntektsmeldingAPIException;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.oidc.token.TokenString;
import no.nav.vedtak.sikkerhet.oidc.token.texas.IdProvider;
import no.nav.vedtak.sikkerhet.oidc.token.texas.IntrospectTokenRequest;
import no.nav.vedtak.sikkerhet.oidc.token.texas.TexasTokenKlient;

public class AuthTjeneste {
    private static final Environment ENV = Environment.current();
    private static final Logger LOG = LoggerFactory.getLogger(AuthTjeneste.class);

    private final TexasTokenKlient tokenKlient;

    public AuthTjeneste() {
        this(TexasTokenKlient.instance());
    }

    protected AuthTjeneste(TexasTokenKlient tokenKlient) {
        this.tokenKlient = tokenKlient;
    }

    public void validerOgSettKontekst(TokenString tokenString) {

        var response = tokenKlient.introspectToken(new IntrospectTokenRequest(IdProvider.MASKINPORTEN, tokenString.token()));

        if (!response.active()) {
            LOG.info("Token er inaktivt. Token introspect respons: {}", response.error());
            throw new InntektsmeldingAPIException(EksponertFeilmelding.UTGAATT_TOKEN, Response.Status.UNAUTHORIZED);
        }

        List<String> scopes = List.of(response.scope().split(" "));
        var gyldigScope = "nav:inntektsmelding/sif";
        boolean harGyldigScope = scopes.stream().anyMatch(s -> s.equals(gyldigScope));

        if (!harGyldigScope) {
            LOG.info("Token har ikke gyldig scope. Token introspect respons: {}", response.error());
            throw new InntektsmeldingAPIException(EksponertFeilmelding.FEIL_SCOPE, Response.Status.UNAUTHORIZED);
        }

        if (response.authorization_details() == null || response.authorization_details().isEmpty()) {
            throw new InntektsmeldingAPIException(EksponertFeilmelding.UGYLDIG_TOKEN, Response.Status.UNAUTHORIZED);
        }
        var tokenKontekst = new TokenKontekst(
            response.consumer().id(),
            response.consumer().id(),
            response.authorization_details().getFirst().systemuser_org().id(),
            response.authorization_details().getFirst().systemuser_id().getFirst());

        if (!ENV.isProd()) {
            var consumerId = response.consumer().id();
            var systemuserOrg = response.authorization_details().getFirst().systemuser_org().id();
            var systemuserId = response.authorization_details().getFirst().systemuser_id().getFirst();
            LOG.info("Token validering vellykket, consumerId: {}, systemuser_org: {}, systemuser_id: {}",
                consumerId.substring(Math.max(0, consumerId.length() - 3)),
                systemuserOrg.substring(Math.max(0, systemuserOrg.length() - 3)),
                systemuserId.substring(Math.max(0, systemuserId.length() - 3)));
        }

        KontekstHolder.setKontekst(tokenKontekst);
    }
}
