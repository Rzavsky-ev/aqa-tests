package aqa.api.utils;

import aqa.api.exceptions.UtilityClassException;

import static aqa.api.utils.Constants.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Утилитарный класс для настройки стабов (заглушек) WireMock.
 * Предоставляет методы для мокирования эндпоинтов API с различными сценариями ответов.
 */
public class WireMockStubBuilder {

    /**
     * Настраивает стаб для успешной аутентификации.
     *
     * @param token токен аутентификации для включения в тело запроса
     * @throws IllegalArgumentException если {@code token} равен {@code null} или пустой строке
     */
    public static void mockAuthSuccess(String token) {
        buildAuthStub(token, HTTP_OK, "{\"status\":\"success\"}");
    }

    /**
     * Настраивает стаб для ошибочной аутентификации с указанным HTTP-статусом.
     *
     * @param token      токен аутентификации для включения в тело запроса
     * @param statusCode HTTP-статус код для возврата в ответе
     * @throws IllegalArgumentException если {@code token} равен {@code null} или пустой строке,
     *                                  или {@code statusCode} не является валидным HTTP-статусом
     */
    public static void mockAuthError(String token, int statusCode) {
        buildAuthStub(token, statusCode, "{\"error\":\"authentication failed\"}");
    }

    /**
     * Настраивает стаб для успешного выполнения действия.
     *
     * @param token токен аутентификации для включения в тело запроса
     * @throws IllegalArgumentException если {@code token} равен {@code null} или пустой строке
     */
    public static void mockDoActionSuccess(String token) {
        buildDoActionStub(token, HTTP_OK, "{\"action\":\"completed\"}");
    }

    /**
     * Настраивает стаб для ошибочного выполнения действия с указанным HTTP-статусом.
     *
     * @param token      токен аутентификации для включения в тело запроса
     * @param statusCode HTTP-статус код для возврата в ответе
     * @throws IllegalArgumentException если {@code token} равен {@code null} или пустой строке,
     *                                  или {@code statusCode} не является валидным HTTP-статусом
     */
    public static void mockDoActionError(String token, int statusCode) {
        buildDoActionStub(token, statusCode, "{\"error\":\"action failed\"}");
    }

    /**
     * Создает стаб для эндпоинта аутентификации.
     *
     * @param token      значение токена для проверки в теле запроса
     * @param statusCode HTTP-статус для возврата в ответе
     * @param body       тело JSON-ответа
     */
    private static void buildAuthStub(String token, int statusCode, String body) {
        stubFor(post(urlEqualTo(MOCK_AUTH))
                .withHeader(CONTENT_TYPE, containing(APPLICATION_URLENCODED))
                .withHeader(ACCEPT, containing(APPLICATION_JSON))
                .withRequestBody(containing("token=" + token))
                .willReturn(aResponse()
                        .withStatus(statusCode)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(body)));
    }

    /**
     * Создает стаб для эндпоинта выполнения действий.
     *
     * @param token      значение токена для проверки в теле запроса
     * @param statusCode HTTP-статус для возврата в ответе
     * @param body       тело JSON-ответа
     */
    private static void buildDoActionStub(String token, int statusCode, String body) {
        stubFor(post(urlEqualTo(MOCK_DO_ACTION))
                .withHeader(CONTENT_TYPE, containing(APPLICATION_URLENCODED))
                .withHeader(ACCEPT, containing(APPLICATION_JSON))
                .withRequestBody(containing("token=" + token))
                .willReturn(aResponse()
                        .withStatus(statusCode)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(body)));
    }

    private WireMockStubBuilder() {
        throw new UtilityClassException(getClass());
    }
}
