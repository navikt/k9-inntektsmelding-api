package no.nav.k9.inntektsmelding.api.integrasjoner;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import no.nav.foreldrepenger.inntektsmelding.imapi.forespørsel.ForespørselFilterRequest;
import no.nav.foreldrepenger.inntektsmelding.imapi.forespørsel.ForespørselResponse;
import no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding.HentInntektsmeldingResponse;
import no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding.InntektsmeldingFilterRequest;
import no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingRequest;

import no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingResponse;

import no.nav.vedtak.mapper.json.DefaultJsonMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.k9.inntektsmelding.api.server.exceptions.InntektsmeldingAPIException;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@Dependent
@RestClientConfig(
    tokenConfig = TokenFlow.AZUREAD_CC,
    scopesProperty = "k9inntektsmelding.scopes",
    endpointProperty = "k9inntektsmelding.url")
public class FpinntektsmeldingKlient {
    private static final Logger LOG = LoggerFactory.getLogger(FpinntektsmeldingKlient.class);
    private static final Logger SECURE_LOG = LoggerFactory.getLogger("secureLogger");

    private final RestClient restClient;
    private final RestConfig restConfig;
    private final URI uriHentForespørsel;
    private final URI uriSendInntektsmelding;
    private final URI uriHentForespørsler;
    private final URI uriHentInntektsmelding;
    private final URI uriHentInntektsmeldinger;

    public FpinntektsmeldingKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(FpinntektsmeldingKlient.class);
        this.uriHentForespørsel = toUri(restConfig.fpContextPath(), "api/imapi/foresporsel/hent");
        this.uriHentForespørsler = toUri(restConfig.fpContextPath(), "api/imapi/foresporsel/hent/foresporsler");
        this.uriSendInntektsmelding = toUri(restConfig.fpContextPath(), "api/imapi/inntektsmelding/send-inntektsmelding");
        this.uriHentInntektsmelding = toUri(restConfig.fpContextPath(), "api/imapi/inntektsmelding/hent");
        this.uriHentInntektsmeldinger = toUri(restConfig.fpContextPath(), "api/imapi/inntektsmelding/hent/inntektsmeldinger");
    }

    ForespørselResponse hentForespørsel(UUID forespørselUuid) {
        try {
            LOG.info("Sender request til fpinntektsmelding for forespørselUuid {} ", forespørselUuid);
            var request = RestRequest.newGET(toUri(uriHentForespørsel, "/" + forespørselUuid), restConfig);
            var response = restClient.sendReturnUnhandled(request);
            if (response.statusCode() == 404) {
                LOG.info("Forespørsel ikke funnet i fpinntektsmelding for uuid: {}", forespørselUuid);
                return null;
            }
            if (response.statusCode() >= 400) {
                LOG.warn("FP-97215: Uventet respons {} ved henting av forespørsel fra fpinntektsmelding for uuid: {}",
                    response.statusCode(), forespørselUuid);
                throw feilVedKallTilFpinntektsmelding();
            }
            return DefaultJsonMapper.fromJson(response.body(), ForespørselResponse.class);
        } catch (InntektsmeldingAPIException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("FP-97215: Feil ved henting av forespørsel fra fpinntektsmelding for uuid: {}. Feilmelding var {}",
                forespørselUuid,
                e.getMessage());
            throw feilVedKallTilFpinntektsmelding();
        }
    }

    List<ForespørselResponse> hentForespørsler(ForespørselFilterRequest filter) {
        try {
            var request = RestRequest.newPOSTJson(filter, uriHentForespørsler, restConfig);
            var response = restClient.send(request, ForespørselResponse[].class);
            return List.of(response);
        } catch (Exception e) {
            LOG.warn("FP-97215: Feil ved henting av forespørsler fra fpinntektsmelding for orgnr: {}. Feilmelding var {}",
                filter.orgnr(),
                e.getMessage());
            throw feilVedKallTilFpinntektsmelding();
        }
    }

    SendInntektsmeldingResponse sendInntektsmelding(SendInntektsmeldingRequest inntektsmeldingRequest) {
        try {
            LOG.info("Sender inntektsmelding til fpinntektsmelding for forespørselUuid {} ", inntektsmeldingRequest.foresporselUuid());
            var request = RestRequest.newPOSTJson(inntektsmeldingRequest, uriSendInntektsmelding, restConfig);
            return restClient.send(request, SendInntektsmeldingResponse.class);
        } catch (Exception e) {
            LOG.warn("FP-97215: Feil ved sending av inntektsmelding-api til fpinntektsmelding for uuid: {}. Feilmelding var {}", inntektsmeldingRequest.foresporselUuid(), e.getMessage());
            SECURE_LOG.info("FP-97215: Feil ved sending av inntektsmelding-api til fpinntektsmelding. InntektsmeldingRequestDto er {}", inntektsmeldingRequest);
            throw feilVedKallTilFpinntektsmelding();
        }
    }


    HentInntektsmeldingResponse hentInntektsmelding(UUID innsendingId) {
        try {
            LOG.info("Henter inntektsmelding fra fpinntektsmelding for uuid {} ", innsendingId);
            var fullUri = uriHentInntektsmelding.toString() + "/" + innsendingId;
            var request = RestRequest.newGET(URI.create(fullUri), restConfig);
            var response = restClient.sendReturnUnhandled(request);
            if (response.statusCode() == 404) {
                LOG.info("Inntektsmelding ikke funnet i fpinntektsmelding for uuid: {}", innsendingId);
                return null;
            }
            if (response.statusCode() >= 400) {
                LOG.warn("FP-97215: Uventet respons {} ved henting av inntektsmelding fra fpinntektsmelding for uuid: {}",
                    response.statusCode(), innsendingId);
                throw feilVedKallTilFpinntektsmelding();
            }
            return DefaultJsonMapper.fromJson(response.body(), HentInntektsmeldingResponse.class);
        } catch (InntektsmeldingAPIException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("FP-97215: Feil ved henting av inntektsmelding fra fpinntektsmelding for uuid: {}. Feilmelding var {}", innsendingId, e.getMessage());
            throw feilVedKallTilFpinntektsmelding();
        }
    }

     List<HentInntektsmeldingResponse> hentInntektsmeldinger(InntektsmeldingFilterRequest filter) {
         try {
             var request = RestRequest.newPOSTJson(filter, uriHentInntektsmeldinger, restConfig);
             var response = restClient.send(request, HentInntektsmeldingResponse[].class);
             return List.of(response);
         } catch (Exception e) {
             LOG.warn("FP-97215: Feil ved henting av inntektsmeldinger fra fpinntektsmelding for orgnr: {}. Feilmelding var {}",
                 filter.orgnr(),
                 e.getMessage());
             throw feilVedKallTilFpinntektsmelding();
         }
    }

    private static TekniskException feilVedKallTilFpinntektsmelding() {
        throw new InntektsmeldingAPIException(EksponertFeilmelding.STANDARD_FEIL, Response.Status.INTERNAL_SERVER_ERROR);
    }

    private URI toUri(URI endpointURI, String path) {
        try {
            return UriBuilder.fromUri(endpointURI).path(path).build();
        } catch (Exception e) {
            LOG.warn("Ugyldig uri: {}, feilmelding {}", endpointURI + path, e.getMessage());
            throw new InntektsmeldingAPIException(EksponertFeilmelding.STANDARD_FEIL, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

}

