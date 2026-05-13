package no.nav.k9.inntektsmelding.api.server.app.api;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.integration.GenericOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import no.nav.k9.inntektsmelding.api.server.auth.AutentiseringFilter;
import no.nav.k9.inntektsmelding.api.server.exceptions.ConstraintViolationMapper;
import no.nav.k9.inntektsmelding.api.server.exceptions.LokalRestExceptionMapper;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.ForespørselRest;
import no.nav.k9.inntektsmelding.api.tjenester.eksterne.InntektsmeldingRest;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.server.rest.FeilUtils;
import no.nav.vedtak.server.rest.jackson.Jackson3ContextResolver;
import no.nav.vedtak.server.rest.jackson.Jackson3ProviderFeature;

@ApplicationPath(ApiConfig.API_URI)
public class ApiConfig extends ResourceConfig {

    public static final String API_URI = "/v1";
    private static final Logger LOG = LoggerFactory.getLogger(ApiConfig.class);
    private static final Logger SECURE_LOG = LoggerFactory.getLogger("secureLogger");

    public ApiConfig() {
        LOG.info("Initialiserer: {}", API_URI);
        // Sikkerhet
        register(Jackson3ProviderFeature.class);
        register(Jackson3ContextResolver.class);
        register(Jackson3ExceptionMapper.class);
        register(AutentiseringFilter.class);
        registerExceptionMappers();

        registerOpenApi();
        // REST
        registerClasses(getApplicationClasses());

        setProperties(getApplicationProperties());
        LOG.info("Ferdig med initialisering av {}", API_URI);
    }

    private void registerOpenApi() {
        var oas = new OpenAPI().openapi("3.2.0");
        var info = new Info().title("K9 inntektsmelding API")
            .version("1.0.0")
            .description("API for inntektsmelding for pleiepenger, omsorgspenger og opplæringspenger");

        oas.info(info).addServersItem(new Server())
            .addTagsItem(new Tag().name("Forespørsel om inntektsmelding").description("Endepunkter for å hente forespørsler NAV har sendt til arbeidsgiver"))
            .addTagsItem(new Tag().name("Inntektsmelding").description("Endepunkter for å sende inn og hente inntektsmeldinger"))
            .schemaRequirement("bearer", new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT"))
            .addSecurityItem(new SecurityRequirement().addList("bearer"));
        var oasConfig = new SwaggerConfiguration().openAPI(oas)
            .prettyPrint(true)
            .resourceClasses(getApplicationClasses().stream().map(Class::getName).collect(Collectors.toSet()));
        try {
            new GenericOpenApiContextBuilder<>().openApiConfiguration(oasConfig).buildContext(true).read();
        } catch (OpenApiConfigurationException e) {
            throw new TekniskException("OPEN-API", e.getMessage(), e);
        }

        register(OpenApiRest.class);
    }

    void registerExceptionMappers() {
        // Behold lokale pga annet respons-body-objekt enn interne apps
        // Skal forbedre oppsett av sikkerlogging her
        FeilUtils.setSikkerlogg(SECURE_LOG);
        register(LokalRestExceptionMapper.class);
        register(ConstraintViolationMapper.class);
    }

    private Set<Class<?>> getApplicationClasses() {
        return Set.of(ForespørselRest.class, InntektsmeldingRest.class);
    }

    private Map<String, Object> getApplicationProperties() {
        Map<String, Object> properties = new HashMap<>();
        // Ref Jersey doc
        properties.put(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        properties.put(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, true);
        return properties;
    }
}
