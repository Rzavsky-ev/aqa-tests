package aqa.api.utils;

import aqa.api.exceptions.UtilityClassException;
import com.github.javafaker.Faker;
import io.qameta.allure.Story;

import static aqa.api.utils.Constants.TOKEN_LENGTH;

/**
 * Утилитарный класс для генерации тестовых токенов аутентификации.
 * <p>
 * Предоставляет методы для создания токенов с различными характеристиками,
 * используемыми в тестировании API. Все токены генерируются в формате шестнадцатеричной
 * строки (HEX), состоящей из символов [0-9A-F], если не указано иное.
 */
@Story("Генерация токенов аутентификации")
public class TokenGenerator {
    private static final Faker faker = new Faker();

    /**
     * Генерирует валидный токен аутентификации.
     *
     * @return валидный токен в HEX-формате
     */
    public static String generateValidToken() {
        return faker.regexify("[A-F0-9]{" + TOKEN_LENGTH + "}");
    }

    /**
     * Генерирует слишком короткий токен аутентификации.
     *
     * @return токен недостаточной длины
     */
    public static String generateShortToken() {
        int length = faker.random().nextInt(1, TOKEN_LENGTH - 1);
        return faker.regexify("[A-F0-9]{" + length + "}");
    }

    /**
     * Генерирует токен с некорректным форматом.
     *
     * @return токен с некорректным символом в конце
     */
    public static String generateLowerCaseToken() {
        return faker.regexify("[A-F0-9]{" + (TOKEN_LENGTH - 1) + "}") + "a";
    }

    private TokenGenerator() {
        throw new UtilityClassException(getClass());
    }
}
