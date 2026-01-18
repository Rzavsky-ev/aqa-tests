package aqa.api.utils.specs;

import aqa.api.exceptions.UtilityClassException;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import static aqa.api.utils.Constants.*;

/**
 * Утилитный класс для создания спецификаций запросов (RequestSpecification) к API.
 * <p>
 * Содержит фабричные методы для построения стандартизированных спецификаций HTTP-запросов
 * к тестируемому веб-сервису. Все методы возвращают настроенные экземпляры
 * {@link RequestSpecification}, которые могут быть использованы в RestAssured запросах.
 */
public class RequestSpecs {

    /**
     * Создает базовую спецификацию запроса с общими настройками.
     *
     * @return базовая {@link RequestSpecification} с общими настройками
     */
    public static RequestSpecification getBaseSpec() {
        return new RequestSpecBuilder()
                .setBaseUri(SUT_URL)
                .setContentType(ContentType.URLENC)
                .setAccept(ContentType.JSON)
                .build();
    }

    /**
     * Создает спецификацию запроса к эндпоинту с указанными параметрами.
     *
     * @param token  значение параметра "token" (32 символа A-F0-9)
     * @param action значение параметра "action" (LOGIN, ACTION или LOGOUT)
     * @param apiKey значение заголовка "X-Api-Key" (может быть null или пустой строкой)
     * @return настроенная {@link RequestSpecification} для запроса к эндпоинту
     */
    public static RequestSpecification forEndpoint(String token, String action, String apiKey) {
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .addRequestSpecification(getBaseSpec())
                .addFormParam(TOKEN_PARAM, token)
                .addFormParam(ACTION_PARAM, action);

        if (apiKey != null && !apiKey.trim().isEmpty()) {
            builder.addHeader(API_KEY_HEADER_NAME, apiKey);
        }
        return builder.build();
    }

    /**
     * Создает спецификацию запроса с валидным API-ключом.
     *
     * @param token  значение параметра "token"
     * @param action значение параметра "action"
     * @return {@link RequestSpecification} с валидным API-ключом
     */
    public static RequestSpecification forValidApiKey(String token, String action) {
        return forEndpoint(token, action, VALID_API_KEY);
    }

    /**
     * Создает спецификацию запроса с невалидным (неправильным) API-ключом.
     *
     * @param token  значение параметра "token"
     * @param action значение параметра "action"
     * @return {@link RequestSpecification} с невалидным API-ключом
     */
    public static RequestSpecification forWrongApiKey(String token, String action) {
        return forEndpoint(token, action, INVALID_API_KEY);
    }

    /**
     * Создает спецификацию запроса с пустым API-ключом.
     *
     * @param token  значение параметра "token"
     * @param action значение параметра "action"
     * @return {@link RequestSpecification} с пустым API-ключом
     */
    public static RequestSpecification forEmptyApiKey(String token, String action) {
        return forEndpoint(token, action, EMPTY_API_KEY);
    }

    private RequestSpecs() {
        throw new UtilityClassException(getClass());
    }
}