# k9-inntektsmelding-api

Innsendingsapi for inntektsmelding for omsorgspenger, opplæringspenger, pleiepenger for sykt barn og pleiepenger i livets sluttfase.

Dette prosjektet inneholder et REST API for innsending av inntektsmeldinger til omsorgspenger, opplæringspenger, pleiepenger for sykt barn og
pleiepenger i livets sluttfase som skal benyttes av Lønns- og personalsystemer (LPS). Løsningen skal muliggjøre uthenting av data for forespurte og
innsendte inntektsmeldinger av en organisasjon.

Appen er en **tilstandsløs ekstern gateway**: den autentiserer og autoriserer LPS-kall og delegerer all lagring og forretningslogikk videre til den
interne backenden `k9-inntektsmelding`.

## Arkitektur

```
LPS (Lønns- og personalsystem)
  │  Maskinporten-token (scope: nav:inntektsmelding/sykdom-i-familien)
  ▼
k9-inntektsmelding-api  ──(Altinn token exchange + PDP authorize)──▶ Altinn 3
  │  Azure AD (maskin-til-maskin)
  ▼
k9-inntektsmelding  ─▶  k9-sak
```

## Autentisering og autorisasjon

| Steg         | Mekanisme                                                                                                                                          |
|--------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| Innkommende  | Maskinporten system-token, eksponert scope `nav:inntektsmelding/sykdom-i-familien`                                                                 |
| Autorisasjon | Altinn 3: token exchange (`altinn:authorization/authorize`) + PDP (XACML authorize) — sjekker at systemet har rettighet på vegne av organisasjonen |
| Til backend  | Azure AD client credentials mot `k9-inntektsmelding`                                                                                               |

Scopes (`exposes`/`consumes`) og Altinn-host er konfigurert i [`.deploy/naiserator.yaml`](.deploy/naiserator.yaml).

## API

Basesti: `/v1`. Alle endepunkter krever et gyldig Maskinporten-token (`Authorization: Bearer <JWT>`)
og at det kallende systemet har Altinn-rettighet for organisasjonen i forespørselen.

| Metode | Sti                                                    | Beskrivelse                                                                          |
|--------|--------------------------------------------------------|--------------------------------------------------------------------------------------|
| `GET`  | `/v1/forespoersel/{forespoerselId}`                    | Hent én forespørsel (UUID)                                                           |
| `POST` | `/v1/forespoersel/forespoersler`                       | Søk/filtrer forespørsler (orgnr, søker-fnr, forespørselId, status, ytelsetype, dato) |
| `POST` | `/v1/inntektsmelding/send-inn`                         | Send inn inntektsmelding for en forespørsel                                          |
| `POST` | `/v1/inntektsmelding/refusjonskrav-omsorgspenger/send` | Send inn refusjonskrav for omsorgspenger (krever ingen forespørsel)                  |
| `GET`  | `/v1/inntektsmelding/hent/{inntektsmeldingId}`         | Hent én inntektsmelding                                                              |
| `POST` | `/v1/inntektsmelding/hent/inntektsmeldinger`           | Søk/filtrer inntektsmeldinger                                                        |

DTO-kontrakten er definert i `inntektsmelding-kontrakt` (releaset av `k9-inntektsmelding`). Brudd på API-kontrakten krever versjonering — OpenAPI-spec
er kontrakten mot LPS-konsumenter.

### OpenAPI

OpenAPI 3.x-spesifikasjonen genereres fra koden og eksponeres på:

- `GET /v1/openapi.json`
- `GET /v1/openapi.yaml`

### Feilformat

Feil returneres som `ErrorResponse` med en kode fra `EksponertFeilmelding`. Vanlige statuskoder:
`400` (ugyldig input), `401` (manglende/ugyldig token eller scope), `403` (ingen tilgang til organisasjonen),
`404` (ikke funnet), `500` (intern feil).

