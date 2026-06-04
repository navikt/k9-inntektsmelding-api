package no.nav.k9.inntektsmelding.api.typer;

public class KodeverkMapper {

    private KodeverkMapper() {
        // Skjuler default konstruktør
    }

    public static no.nav.k9.inntektsmelding.felles.ForespørselStatusDto mapApiStatusTilForespørselStatus(StatusDto status) {
        return switch (status) {
            case AKTIV -> no.nav.k9.inntektsmelding.felles.ForespørselStatusDto.UNDER_BEHANDLING;
            case BESVART -> no.nav.k9.inntektsmelding.felles.ForespørselStatusDto.FERDIG;
            case FORKASTET -> no.nav.k9.inntektsmelding.felles.ForespørselStatusDto.UTGÅTT;
        };
    }

    public static StatusDto mapTilDto(ForespørselStatus forespørselStatus) {
        return switch (forespørselStatus) {
            case UTGÅTT -> StatusDto.FORKASTET;
            case UNDER_BEHANDLING -> StatusDto.AKTIV;
            case FERDIG -> StatusDto.BESVART;
        };
    }

    public static YtelseTypeDto mapTilDto(YtelseType ytelseType) {
        return switch (ytelseType) {
            case PLEIEPENGER_SYKT_BARN -> YtelseTypeDto.PLEIEPENGER_SYKT_BARN;
            case PLEIEPENGER_I_LIVETS_SLUTTFASE -> YtelseTypeDto.PLEIEPENGER_I_LIVETS_SLUTTFASE;
            case OPPLÆRINGSPENGER -> YtelseTypeDto.OPPLÆRINGSPENGER;
            case OMSORGSPENGER -> YtelseTypeDto.OMSORGSPENGER;
        };
    }

    public static YtelseType mapYtelseType(no.nav.k9.inntektsmelding.felles.YtelseTypeDto ytelseTypeDto) {
        return switch (ytelseTypeDto) {
            case PLEIEPENGER_SYKT_BARN -> YtelseType.PLEIEPENGER_SYKT_BARN;
            case PLEIEPENGER_I_LIVETS_SLUTTFASE -> YtelseType.PLEIEPENGER_I_LIVETS_SLUTTFASE;
            case OPPLÆRINGSPENGER -> YtelseType.OPPLÆRINGSPENGER;
            case OMSORGSPENGER -> YtelseType.OMSORGSPENGER;
        };
    }

    public static ForespørselStatus mapForespørselStatus(no.nav.k9.inntektsmelding.felles.ForespørselStatusDto status) {
        return switch (status) {
            case UNDER_BEHANDLING -> ForespørselStatus.UNDER_BEHANDLING;
            case FERDIG -> ForespørselStatus.FERDIG;
            case UTGÅTT -> ForespørselStatus.UTGÅTT;
        };
    }
}
