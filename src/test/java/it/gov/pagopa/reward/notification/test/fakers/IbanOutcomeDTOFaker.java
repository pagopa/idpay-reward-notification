package it.gov.pagopa.reward.notification.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.reward.notification.dto.iban.IbanOutcomeDTO;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Locale;
import java.util.Random;

public class IbanOutcomeDTOFaker {
    private static final Random randomGenerator = new Random();

    public static Random getRandom(Integer bias) {
        return bias == null ? randomGenerator : new Random(bias);
    }

    public static int getRandomPositiveNumber(Integer bias) {
        return Math.abs(getRandom(bias).nextInt());
    }

    private static final FakeValuesService fakeValuesServiceGlobal = new FakeValuesService(new Locale("it"), new RandomService(null));

    public static FakeValuesService getFakeValuesService(Integer bias) {
        return bias == null ? fakeValuesServiceGlobal : new FakeValuesService(new Locale("it"), new RandomService(getRandom(bias)));
    }

    /** It will return an example of {@link IbanOutcomeDTO}. Providing a bias, it will return a pseudo-casual object */
    public static IbanOutcomeDTO mockInstance(Integer bias){
        return mockInstanceBuilder(bias).build();
    }

    /** It will return an example of builder to obtain a {@link IbanOutcomeDTO}. Providing a bias, it will return a pseudo-casual object */
    public static IbanOutcomeDTO.IbanOutcomeDTOBuilder<?, ?> mockInstanceBuilder(Integer bias){
        IbanOutcomeDTO.IbanOutcomeDTOBuilder<?, ?> out = IbanOutcomeDTO.builder();

        bias = ObjectUtils.firstNonNull(bias, getRandomPositiveNumber(null));

        FakeValuesService fakeValuesService = getFakeValuesService(bias);

        out.userId("USERID_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.initiativeId("INITIATIVEID_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.iban("IBAN_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.status("STATUS_%d_%s".formatted(bias, fakeValuesService.bothify("???")));

        return out;
    }
}