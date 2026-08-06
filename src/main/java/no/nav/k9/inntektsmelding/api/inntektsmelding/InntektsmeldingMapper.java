package no.nav.k9.inntektsmelding.api.inntektsmelding;

import no.nav.k9.inntektsmelding.api.tjenester.eksterne.responses.InntektsmeldingDto;
import no.nav.vedtak.konfig.Tid;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class InntektsmeldingMapper {
    private InntektsmeldingMapper() {
        // Skjuler default
    }

    public static InntektsmeldingDto mapTilDto(Inntektsmelding inntektsmelding) {
        var kontakpersonDto = new InntektsmeldingDto.Kontaktperson(inntektsmelding.kontaktperson().navn(),
            inntektsmelding.kontaktperson().telefonnummer());
        var inntektEndringsårsaker = mapEndringsårsaker(inntektsmelding);
        var inntekt = new InntektsmeldingDto.Inntekt(inntektsmelding.månedInntekt(), inntektsmelding.skjæringstidspunkt(), inntektEndringsårsaker);
        var avsendersystemDto = new InntektsmeldingDto.AvsenderSystem(inntektsmelding.avsenderSystem().navn(),
            inntektsmelding.avsenderSystem().versjon());
        var alleRefusjonsendringer = mapRefusjon(inntektsmelding);
        var naturalytelser = mapNaturalytelser(inntektsmelding);
        var refusjon = new InntektsmeldingDto.Refusjon(inntektsmelding.månedRefusjon(), alleRefusjonsendringer);
        var omsorgspengerInfo = mapOmsorgspengerInfo(inntektsmelding.omsorgspengerInfo());
        return new InntektsmeldingDto(inntektsmelding.inntektsmeldingUuid(),
            inntektsmelding.fnr(),
            inntektsmelding.ytelse(),
            new InntektsmeldingDto.InntektsmeldingArbeidsgiver(inntektsmelding.orgnr().orgnr(), kontakpersonDto),
            inntektsmelding.startdato(),
            inntekt,
            inntektsmelding.innsendtTidspunkt(),
            avsendersystemDto,
            refusjon,
            naturalytelser,
            omsorgspengerInfo);
    }

    private static List<InntektsmeldingDto.Naturalytelse> mapNaturalytelser(Inntektsmelding inntektsmelding) {
        if (inntektsmelding.bortfaltNaturalytelsePerioder() == null) {
            return List.of();
        }
        return inntektsmelding.bortfaltNaturalytelsePerioder()
            .stream()
            .map(n -> new InntektsmeldingDto.Naturalytelse(n.beløp(), n.fom(), n.naturalytelsetype()))
            .toList();
    }

    private static List<InntektsmeldingDto.InntektEndringsårsaker> mapEndringsårsaker(Inntektsmelding inntektsmelding) {
        if (inntektsmelding.endringAvInntektÅrsaker() == null) {
            return List.of();
        }
        return inntektsmelding.endringAvInntektÅrsaker()
            .stream()
            .map(e -> new InntektsmeldingDto.InntektEndringsårsaker(e.årsak(), e.fom(), e.tom(), e.bleKjentFom()))
            .toList();
    }

    private static List<InntektsmeldingDto.RefusjonEndring> mapRefusjon(Inntektsmelding inntektsmelding) {
        var listeMedEndringer = inntektsmelding.refusjon() == null ? new java.util.ArrayList<InntektsmeldingDto.RefusjonEndring>() :
            inntektsmelding.refusjon()
                .stream()
                .map(r -> new InntektsmeldingDto.RefusjonEndring(r.beløp(), r.fom()))
                .collect(Collectors.toCollection(java.util.ArrayList::new));
        if (inntektsmelding.opphørsdatoRefusjon() != null && !inntektsmelding.opphørsdatoRefusjon().equals(Tid.TIDENES_ENDE)) {
            listeMedEndringer.add(new InntektsmeldingDto.RefusjonEndring(BigDecimal.ZERO, inntektsmelding.opphørsdatoRefusjon()));
        }
        return listeMedEndringer;
    }

    private static InntektsmeldingDto.OmsorgspengerInfo mapOmsorgspengerInfo(Inntektsmelding.OmsorgspengerInfo omsorgspengerInfo) {
        if (omsorgspengerInfo == null) {
            return null;
        }
        List<InntektsmeldingDto.OmsorgspengerInfo.FraværHeleDagenPeriode> fraværHeleDagenPerioder = omsorgspengerInfo.fraværHeleDagenPerioder() == null ? List.of() :
                                                                                                    omsorgspengerInfo.fraværHeleDagenPerioder().stream()
                .map(f -> new InntektsmeldingDto.OmsorgspengerInfo.FraværHeleDagenPeriode(f.fom(), f.tom()))
                .toList();
        List<InntektsmeldingDto.OmsorgspengerInfo.FraværDelerAvDagen> fraværDelerAvDager = omsorgspengerInfo.fraværDelerAvDager() == null ? List.of() :
                                                                                           omsorgspengerInfo.fraværDelerAvDager().stream()
                .map(f -> new InntektsmeldingDto.OmsorgspengerInfo.FraværDelerAvDagen(f.dato(), f.timer()))
                .toList();
        return new InntektsmeldingDto.OmsorgspengerInfo(omsorgspengerInfo.harUtbetaltPliktigeDager(), fraværHeleDagenPerioder, fraværDelerAvDager);
    }

}
