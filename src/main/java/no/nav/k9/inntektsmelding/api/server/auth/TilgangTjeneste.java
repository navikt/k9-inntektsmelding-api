package no.nav.k9.inntektsmelding.api.server.auth;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.ws.rs.core.Response;

import no.nav.k9.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.k9.inntektsmelding.api.server.exceptions.InntektsmeldingAPIException;

import no.nav.k9.inntektsmelding.api.typer.Organisasjonsnummer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.inntektsmelding.api.server.auth.altinnPdp.PdpKlient;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@ApplicationScoped
public class TilgangTjeneste implements Tilgang {
    private static final Logger LOG = LoggerFactory.getLogger(TilgangTjeneste.class);
    private static final Environment ENV = Environment.current();

    @Override
    public void sjekkAtSystemHarTilgangTilOrganisasjon(Organisasjonsnummer orgnummerFraForespørsel) {
        var orgnummerFraKontekst = hentOrgnrFraKontekst();
        var systemId = hentSystemIdFraKontekst();
        if (!orgnummerFraKontekst.equals(orgnummerFraForespørsel)) {
            LOG.info("Kontekst har ikke samme orgnummer som forespørsel. Dette skyldes trolig at orgnummer fra token er juridisk enhet. "
                + "Orgnummer fra kontekst var {} og orgnummer fra forespørsel var {}", orgnummerFraKontekst, orgnummerFraForespørsel);
        }
        var ressurs = ENV.getRequiredProperty("altinn.tre.inntektsmelding.ressurs");

        boolean harTilgang;
        try {
            harTilgang = PdpKlient.instance().systemHarRettighetForOrganisasjon(systemId, orgnummerFraForespørsel.orgnr(), ressurs);
        } catch (Exception e) {
            LOG.warn(e.toString());
            throw new InntektsmeldingAPIException(EksponertFeilmelding.FEIL_OPPSLAG_ALTINN, Response.Status.INTERNAL_SERVER_ERROR, e);
        }
        if (!harTilgang) {
            throw new InntektsmeldingAPIException(EksponertFeilmelding.IKKE_TILGANG_ALTINN, Response.Status.UNAUTHORIZED);
        }
    }

    private Organisasjonsnummer hentOrgnrFraKontekst() {
        if (KontekstHolder.getKontekst() instanceof TokenKontekst tk) {
            return tk.getOrganisasjonNummer();
        }
        throw ikkeTilgang("Har ikke gyldig token-kontekst");
    }

    private String hentSystemIdFraKontekst() {
        if (KontekstHolder.getKontekst() instanceof TokenKontekst tk) {
            return tk.getSystemUserId();
        }
        throw ikkeTilgang("Har ikke gyldig token-kontekst");
    }

    private static ManglerTilgangException ikkeTilgang(String begrunnelse) {
        LOG.warn("IM-00403: Mangler tilgang til tjenesten. {}", begrunnelse);
        throw new InntektsmeldingAPIException(EksponertFeilmelding.STANDARD_FEIL, Response.Status.INTERNAL_SERVER_ERROR);
    }

}
