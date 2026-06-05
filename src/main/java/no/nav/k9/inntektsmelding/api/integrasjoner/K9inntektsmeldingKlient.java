package no.nav.k9.inntektsmelding.api.integrasjoner;

import java.net.URI;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import no.nav.k9.inntektsmelding.imapi.forespørsel.ForespørselDto;
import no.nav.k9.inntektsmelding.imapi.forespørsel.HentForespørselerRequest;
import no.nav.k9.inntektsmelding.imapi.forespørsel.HentForespørslerResponse;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.HentInntektsmeldingerRequest;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.HentInntektsmeldingerResponse;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.InntektsmeldingDto;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingRequest;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingResponse;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.inntektsmelding.api.server.exceptions.EksponertFeilmelding;
import no.nav.k9.inntektsmelding.api.server.exceptions.InntektsmeldingAPIException;
import no.nav.vedtak.exception.TekniskException;
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
public class K9inntektsmeldingKlient {
    private static final Logger LOG = LoggerFactory.getLogger(K9inntektsmeldingKlient.class);
    private static final Logger SECURE_LOG = LoggerFactory.getLogger("secureLogger");

    private final RestClient restClient;
    private final RestConfig restConfig;
    private final URI uriHentForespørsel;
    private final URI uriSendInntektsmelding;
    private final URI uriHentForespørsler;
    private final URI uriHentInntektsmelding;
    private final URI uriHentInntektsmeldinger;

    public K9inntektsmeldingKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(K9inntektsmeldingKlient.class);
        this.uriHentForespørsel = toUri(restConfig.endpoint(), "api/imapi/foresporsel/hent");
        this.uriHentForespørsler = toUri(restConfig.endpoint(), "api/imapi/foresporsel/hent/foresporsler");
        this.uriSendInntektsmelding = toUri(restConfig.endpoint(), "api/imapi/inntektsmelding/send-inntektsmelding");
        this.uriHentInntektsmelding = toUri(restConfig.endpoint(), "api/imapi/inntektsmelding/hent");
        this.uriHentInntektsmeldinger = toUri(restConfig.endpoint(), "api/imapi/inntektsmelding/hent/inntektsmeldinger");
    }

    ForespørselDto hentForespørsel(UUID forespørselUuid) {
        try {
            LOG.info("Sender request til k9-inntektsmelding for forespørselUuid {} ", forespørselUuid);
            var request = RestRequest.newGET(toUri(uriHentForespørsel, "/" + forespørselUuid), restConfig);
            var response = restClient.sendReturnUnhandled(request);
            if (response.statusCode() == 404) {
                LOG.info("Forespørsel ikke funnet i k9-inntektsmelding for uuid: {}", forespørselUuid);
                return null;
            }
            if (response.statusCode() >= 400) {
                LOG.warn("K9-97215: Uventet respons {} ved henting av forespørsel fra k9-inntektsmelding for uuid: {}",
                    response.statusCode(), forespørselUuid);
                throw feilVedKallTilK9inntektsmelding();
            }
            return DefaultJsonMapper.fromJson(response.body(), ForespørselDto.class);
        } catch (InntektsmeldingAPIException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("K9-97215: Feil ved henting av forespørsel fra k9-inntektsmelding for uuid: {}. Feilmelding var {}",
                forespørselUuid,
                e.getMessage());
            throw feilVedKallTilK9inntektsmelding();
        }
    }

    HentForespørslerResponse hentForespørsler(HentForespørselerRequest filter) {
        try {
            var request = RestRequest.newPOSTJson(filter, uriHentForespørsler, restConfig);
            return restClient.send(request, HentForespørslerResponse.class);
        } catch (Exception e) {
            LOG.warn("K9-97215: Feil ved henting av forespørsler fra k9-inntektsmelding for orgnr: {}. Feilmelding var {}",
                filter.orgnr(),
                e.getMessage());
            throw feilVedKallTilK9inntektsmelding();
        }
    }

    SendInntektsmeldingResponse sendInntektsmelding(SendInntektsmeldingRequest inntektsmeldingRequest) {
        try {
            LOG.info("Sender inntektsmelding til k9-inntektsmelding for forespørselUuid {} ", inntektsmeldingRequest.foresporselUuid());
            var request = RestRequest.newPOSTJson(inntektsmeldingRequest, uriSendInntektsmelding, restConfig);
            return restClient.send(request, SendInntektsmeldingResponse.class);
        } catch (Exception e) {
            LOG.warn("K9-97215: Feil ved sending av inntektsmelding-api til k9-inntektsmelding for uuid: {}. Feilmelding var {}", inntektsmeldingRequest.foresporselUuid(), e.getMessage());
            SECURE_LOG.info("K9-97215: Feil ved sending av inntektsmelding-api til k9-inntektsmelding. InntektsmeldingRequestDto er {}", inntektsmeldingRequest);
            throw feilVedKallTilK9inntektsmelding();
        }
    }


    InntektsmeldingDto hentInntektsmelding(UUID innsendingId) {
        try {
            LOG.info("Henter inntektsmelding fra k9-inntektsmelding for uuid {} ", innsendingId);
            var fullUri = uriHentInntektsmelding.toString() + "/" + innsendingId;
            var request = RestRequest.newGET(URI.create(fullUri), restConfig);
            var response = restClient.sendReturnUnhandled(request);
            if (response.statusCode() == 404) {
                LOG.info("Inntektsmelding ikke funnet i k9-inntektsmelding for uuid: {}", innsendingId);
                return null;
            }
            if (response.statusCode() >= 400) {
                LOG.warn("K9-97215: Uventet respons {} ved henting av inntektsmelding fra k9-inntektsmelding for uuid: {}",
                    response.statusCode(), innsendingId);
                throw feilVedKallTilK9inntektsmelding();
            }
            return DefaultJsonMapper.fromJson(response.body(), InntektsmeldingDto.class);
        } catch (InntektsmeldingAPIException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("K9-97215: Feil ved henting av inntektsmelding fra k9-inntektsmelding for uuid: {}. Feilmelding var {}", innsendingId, e.getMessage());
            throw e;
        }
    }

     HentInntektsmeldingerResponse hentInntektsmeldinger(HentInntektsmeldingerRequest filter) {
         try {
             var request = RestRequest.newPOSTJson(filter, uriHentInntektsmeldinger, restConfig);
             var response = restClient.send(request, HentInntektsmeldingerResponse.class);
             return response;
         } catch (Exception e) {
             LOG.warn("K9-97215: Feil ved henting av inntektsmeldinger fra k9-inntektsmelding for orgnr: {}. Feilmelding var {}",
                 filter.orgnr(),
                 e.getMessage());
             throw feilVedKallTilK9inntektsmelding();
         }
    }

    private static TekniskException feilVedKallTilK9inntektsmelding() {
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

