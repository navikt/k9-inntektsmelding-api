package no.nav.k9.inntektsmelding.api.server.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;

import no.nav.vedtak.sikkerhet.oidc.token.texas.IdProvider;
import no.nav.vedtak.sikkerhet.oidc.token.texas.IntrospectTokenRequest;
import no.nav.vedtak.sikkerhet.oidc.token.texas.IntrospectTokenResponse;
import no.nav.vedtak.sikkerhet.oidc.token.texas.TexasTokenKlient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.k9.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.k9.inntektsmelding.api.server.exceptions.InntektsmeldingAPIException;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.oidc.token.TokenString;

@ExtendWith(MockitoExtension.class)
class AuthTjenesteTest {
    private static final String KORREKT_SCOPE = "nav:inntektsmelding/sif";
    @Mock
    private TexasTokenKlient authKlient;

    private AuthTjeneste authTjeneste;

    @BeforeEach
    void setUp() {
        authTjeneste = new AuthTjeneste(authKlient);
    }

    @Test
    void skal_feile_om_ikke_rett_scope() {
        // Arrange
        var tokenString = new TokenString("123");
        var authDetails = new IntrospectTokenResponse.AuthorizationDetails("type",
            null,
            List.of("systemuser"),
            new IntrospectTokenResponse.OrgDetails("mitt_orgnr", null));
        var introspectTokenRequest = new IntrospectTokenRequest(IdProvider.MASKINPORTEN, tokenString.token());
        when(authKlient.introspectToken(introspectTokenRequest)).thenReturn(createIntrospectTokenResponse(true, "ugyldigScope", "minId", List.of(authDetails)));

        // Act
        var ex = assertThrows(InntektsmeldingAPIException.class, () -> authTjeneste.validerOgSettKontekst(tokenString));

        // Assert
        assertThat(ex.getFeilmelding()).isEqualTo(EksponertFeilmelding.FEIL_SCOPE);
    }

    @Test
    void skal_feile_om_token_ikke_aktivt() {
        // Arrange
        var tokenString = new TokenString("123");
        var authDetails = new IntrospectTokenResponse.AuthorizationDetails("type",
            null,
            List.of("systemuser"),
            new IntrospectTokenResponse.OrgDetails("mitt_orgnr", null));
        var introspectTokenRequest = new IntrospectTokenRequest(IdProvider.MASKINPORTEN, tokenString.token());
        when(authKlient.introspectToken(introspectTokenRequest)).thenReturn(createIntrospectTokenResponse(false, KORREKT_SCOPE, "minId", List.of(authDetails)));

        // Act
        var ex = assertThrows(InntektsmeldingAPIException.class, () -> authTjeneste.validerOgSettKontekst(tokenString));

        // Assert
        assertThat(ex.getFeilmelding()).isEqualTo(EksponertFeilmelding.UTGAATT_TOKEN);
    }

    @Test
    void skal_feile_om_token_ikke_inneholder_påkrevd_felt() {
        // Arrange
        var tokenString = new TokenString("123");
        var introspectTokenRequest = new IntrospectTokenRequest(IdProvider.MASKINPORTEN, tokenString.token());
        when(authKlient.introspectToken(introspectTokenRequest)).thenReturn(createIntrospectTokenResponse(true, KORREKT_SCOPE, "minId", List.of()));

        // Act
        var ex = assertThrows(InntektsmeldingAPIException.class, () -> authTjeneste.validerOgSettKontekst(tokenString));

        // Assert
        assertThat(ex.getFeilmelding()).isEqualTo(EksponertFeilmelding.UGYLDIG_TOKEN);
    }

    @Test
    void skal_sette_kontektst_om_token_er_gyldig() {
        // Arrange
        var tokenString = new TokenString("123");
        var systemuser = "systemuser";
        var orgnr = "999999999";
        var authDetails = new IntrospectTokenResponse.AuthorizationDetails("type",
            null,
            List.of(systemuser),
            new IntrospectTokenResponse.OrgDetails(orgnr, null));
        var introspectTokenRequest = new IntrospectTokenRequest(IdProvider.MASKINPORTEN, tokenString.token());
        when(authKlient.introspectToken(introspectTokenRequest)).thenReturn(createIntrospectTokenResponse(true, KORREKT_SCOPE, "minId", List.of(authDetails)));

        // Act
        authTjeneste.validerOgSettKontekst(tokenString);

        // Assert
        var kontekst = KontekstHolder.getKontekst();
        assertThat(kontekst).isInstanceOf(TokenKontekst.class);
        var tokenKontekst = (TokenKontekst) kontekst;
        assertThat(tokenKontekst.getSystemUserId()).isEqualTo(systemuser);
        assertThat(tokenKontekst.getOrganisasjonNummer().orgnr()).isEqualTo(orgnr);
    }

    private static IntrospectTokenResponse createIntrospectTokenResponse(boolean active, String scope, String consumerId, List<IntrospectTokenResponse.AuthorizationDetails> authorizationDetails) {
        return new IntrospectTokenResponse(
            active,
            null, // error
            null, // token_type
            null, // aud
            null, // azp
            null, // azp_name
            null, // groups
            null, // roles
            null, // tid
            null, // exp
            null, // iat
            null, // nbf
            null, // iss
            null, // jti
            null, // oid
            null, // sub
            null, // NAVident
            null, // idtyp
            null, // acr
            null, // pid
            null, // client_id
            null, // client_amr
            scope,
            new IntrospectTokenResponse.OrgDetails(consumerId, null),
            authorizationDetails
        );
    }
}
