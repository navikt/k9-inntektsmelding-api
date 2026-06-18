package no.nav.k9.inntektsmelding.api.inntektsmelding;

import no.nav.k9.inntektsmelding.api.tjenester.eksterne.InntektsmeldingDto;
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
        var omsorgspenger = mapOmsorgspenger(inntektsmelding.omsorgspenger());
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
            omsorgspenger);
    }

    private static List<InntektsmeldingDto.Naturalytelse> mapNaturalytelser(Inntektsmelding inntektsmelding) {
        return inntektsmelding.bortfaltNaturalytelsePerioder()
            .stream()
            .map(n -> new InntektsmeldingDto.Naturalytelse(n.beløp(), n.fom(), n.naturalytelsetype()))
            .toList();
    }

    private static List<InntektsmeldingDto.InntektEndringsårsaker> mapEndringsårsaker(Inntektsmelding inntektsmelding) {
        return inntektsmelding.endringAvInntektÅrsaker()
            .stream()
            .map(e -> new InntektsmeldingDto.InntektEndringsårsaker(e.årsak(), e.fom(), e.tom(), e.bleKjentFom()))
            .toList();
    }

    private static List<InntektsmeldingDto.RefusjonEndring> mapRefusjon(Inntektsmelding inntektsmelding) {
        var listeMedEndringer = inntektsmelding.refusjon()
            .stream()
            .map(r -> new InntektsmeldingDto.RefusjonEndring(r.beløp(), r.fom()))
            .collect(Collectors.toList());
        if (inntektsmelding.opphørsdatoRefusjon() != null && !inntektsmelding.opphørsdatoRefusjon().equals(Tid.TIDENES_ENDE)) {
            listeMedEndringer.add(new InntektsmeldingDto.RefusjonEndring(BigDecimal.ZERO, inntektsmelding.opphørsdatoRefusjon()));
        }
        return listeMedEndringer;
    }

    private static InntektsmeldingDto.Omsorgspenger mapOmsorgspenger(Inntektsmelding.Omsorgspenger omsorgspenger) {
        if (omsorgspenger == null) {
            return null;
        }
        List<InntektsmeldingDto.Omsorgspenger.FraværHeleDagenPeriode> fraværHeleDagenPerioder = omsorgspenger.fraværHeleDagenPerioder() == null ? List.of() :
                                                                                                omsorgspenger.fraværHeleDagenPerioder().stream()
                .map(f -> new InntektsmeldingDto.Omsorgspenger.FraværHeleDagenPeriode(f.fom(), f.tom()))
                .toList();
        List<InntektsmeldingDto.Omsorgspenger.FraværDelerAvDagen> fraværDelerAvDager = omsorgspenger.fraværDelerAvDager() == null ? List.of() :
                                                                                       omsorgspenger.fraværDelerAvDager().stream()
                .map(f -> new InntektsmeldingDto.Omsorgspenger.FraværDelerAvDagen(f.dato(), f.timer()))
                .toList();
        return new InntektsmeldingDto.Omsorgspenger(omsorgspenger.harUtbetaltPliktigeDager(), fraværHeleDagenPerioder, fraværDelerAvDager);
    }

}
