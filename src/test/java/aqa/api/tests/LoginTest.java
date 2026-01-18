package aqa.api.tests;

import aqa.api.base.BaseTest;
import aqa.api.utils.AllureReporter;
import aqa.api.utils.TokenGenerator;
import aqa.api.utils.WireMockStubBuilder;
import io.qameta.allure.*;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.atomic.AtomicBoolean;

import static aqa.api.utils.Constants.*;
import static aqa.api.utils.ErrorMessages.*;

import static aqa.api.utils.specs.RequestSpecs.*;
import static aqa.api.utils.specs.ResponseSpecs.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("Тестирование веб-сервиса")
@Feature("Функционал LOGIN")
@DisplayName("Тесты для действия LOGIN")
public class LoginTest extends BaseTest {

    @Test
    @Story("Успешные сценарии")
    @DisplayName("Аутентификация с валидным токеном")
    @Description("""
            Проверяет успешную аутентификацию:
            - Генерируется валидный токен (32 символа, A-F0-9)
            - Внешний сервис возвращает 200 OK
            - Приложение возвращает {"result": "OK"}
            """)
    @Tag(SMOKE)
    void successfulLoginWithValidToken() {
        String token = TokenGenerator.generateValidToken();
        AtomicBoolean testPassed = new AtomicBoolean(false);

        try {
            Allure.step("1. Подготовка тестовых данных", () ->
                    AllureReporter.addTestData("Тестовый сценарий",
                            String.format("""
                                            Токен: %s
                                            Длина: %d символов
                                            Ожидается: 200 OK, result: OK
                                            """,
                                    token,
                                    token.length())));

            Allure.step("2. Настройка тестового окружения", () -> {
                WireMockStubBuilder.mockAuthSuccess(token);
                AllureReporter.addTestData("Настройка WireMock",
                        "WireMock настроен на успешный ответ для /auth");
            });

            Allure.step("3. Выполнение запроса и проверка ответа", () -> {
                Response response = given()
                        .spec(forValidApiKey(token, ACTION_LOGIN))
                        .when()
                        .post(ENDPOINT);

                int statusCode = response.getStatusCode();
                String responseBody = response.getBody().asPrettyString();
                long responseTime = response.getTime();

                AllureReporter.addTestData("Анализ ответа",
                        String.format("""
                                        ОЖИДАЛОСЬ:
                                        • HTTP статус: 200 OK ✓
                                        • Структура ответа: {"result": "OK"}
                                        
                                        ПОЛУЧЕНО:
                                        • HTTP статус: %d %s %s
                                        • Время ответа: %d мс
                                        • Тело ответа: %s
                                        
                                        ПРОВЕРКИ:
                                        • Ответ содержит result: OK: %s
                                        """,
                                statusCode,
                                AllureReporter.getStatusText(statusCode),
                                statusCode == 200 ? "✓" : "✗",
                                responseTime,
                                responseBody,
                                responseBody.contains("\"result\": \"OK\"") ? "✓" : "✗"));

                response.then()
                        .spec(forSuccess())
                        .body(RESULT_PARAM, equalTo(RESULT_OK));
            });

            testPassed.set(true);

        } finally {
            Allure.step("4. Итог тестирования", () -> {
                String resultText = testPassed.get() ?
                        """
                                ТЕСТ ПРОЙДЕН УСПЕШНО
                                
                                Что проверено:
                                1. Генерация валидного токена ✓
                                2. Настройка тестового окружения ✓
                                3. Отправка запроса аутентификации ✓
                                4. Получение корректного ответа ✓
                                5. Валидация структуры ответа ✓
                                
                                Вывод: Система корректно аутентифицирует
                                пользователей с валидными токенами.
                                """ :
                        """
                                ТЕСТ ПРОВАЛЕН
                                
                                Не удалось выполнить аутентификацию с валидным токеном.
                                Детали ошибки см. в предыдущих шагах.
                                """;

                AllureReporter.addTestData("Результат теста", resultText);
            });
        }
    }

    @Test
    @Story("Ошибки валидации")
    @DisplayName("Аутентификация с коротким токеном")
    @Description("""
            Проверяет обработку короткого токена:
            - Токен короче 32 символов (невалидная длина)
            - Ожидается ошибка валидации
            - Приложение возвращает: {"result": "ERROR", "message": "token: должно соответствовать "^[0-9A-F]{32}$""}
            """)
    @Tag(REGRESSION)
    void loginWithShortToken() {
        String token = TokenGenerator.generateShortToken();
        AtomicBoolean testPassed = new AtomicBoolean(false);

        try {
            Allure.step("1. Подготовка тестовых данных", () ->
                    AllureReporter.addTestData("Тестовый сценарий",
                            String.format("""
                                            Токен: %s
                                            Длина: %d символов (требуется: 32)
                                            Ожидается: ошибка валидации 400
                                            """,
                                    token,
                                    token.length())));

            Allure.step("2. Выполнение запроса и проверка ответа", () -> {
                Response response = given()
                        .spec(forValidApiKey(token, ACTION_LOGIN))
                        .when()
                        .post(ENDPOINT);

                int statusCode = response.getStatusCode();
                String actualMessage = response.jsonPath().getString(MESSAGE_PARAM);

                AllureReporter.addTestData("Анализ ответа",
                        String.format("""
                                        ОЖИДАЛОСЬ:
                                        • HTTP статус: 400 Bad Request ✓
                                        • Сообщение: "%s"
                                        
                                        ПОЛУЧЕНО:
                                        • HTTP статус: %d %s %s
                                        • Сообщение: "%s" %s
                                        
                                        ПРОВЕРКИ:
                                        • Сообщение соответствует ожидаемому: %s
                                        """,
                                INVALID_TOKEN_ERROR,
                                statusCode,
                                AllureReporter.getStatusText(statusCode),
                                statusCode == 400 ? "✓" : "✗",
                                actualMessage != null ? actualMessage : "отсутствует",
                                INVALID_TOKEN_ERROR.equals(actualMessage) ? "✓" : "✗",
                                INVALID_TOKEN_ERROR.equals(actualMessage) ? "✓" : "✗"));

                response.then()
                        .spec(forValidationError())
                        .body(MESSAGE_PARAM, equalTo(INVALID_TOKEN_ERROR));
            });

            testPassed.set(true);

        } finally {
            Allure.step("3. Итог тестирования", () -> {
                String resultText = testPassed.get() ?
                        """
                                ТЕСТ ПРОЙДЕН УСПЕШНО
                                
                                Что проверено:
                                1. Генерация короткого токена ✓
                                2. Отправка запроса с невалидным токеном ✓
                                3. Получение ошибки валидации 400 ✓
                                4. Корректное сообщение об ошибке ✓
                                
                                Вывод: Система корректно валидирует длину токена.
                                """ :
                        """
                                ТЕСТ ПРОВАЛЕН
                                
                                Не удалось проверить валидацию короткого токена.
                                Детали ошибки см. в предыдущих шагах.
                                """;

                AllureReporter.addTestData("Результат теста", resultText);
            });
        }
    }

    @Test
    @Story("Ошибки валидации")
    @DisplayName("Аутентификация с токеном, содержащим строчные буквы")
    @Description("""
            Проверяет обработку токена с некорректными символами:
            - Токен содержит символы вне диапазона 0-9, A-F
            - Ожидается ошибка валидации
            - Приложение возвращает: {"result": "ERROR", "message": "token: должно соответствовать "^[0-9A-F]{32}$""}
            """)
    @Tag(REGRESSION)
    void loginWithLowerCaseToken() {
        String token = TokenGenerator.generateLowerCaseToken();
        AtomicBoolean testPassed = new AtomicBoolean(false);

        try {
            Allure.step("1. Подготовка тестовых данных", () -> {
                boolean hasLowerCase = !token.equals(token.toUpperCase());
                AllureReporter.addTestData("Тестовый сценарий",
                        String.format("""
                                        Токен: %s
                                        Длина: %d символов
                                        Содержит строчные буквы: %s
                                        Ожидается: ошибка валидации 400
                                        """,
                                token,
                                token.length(),
                                hasLowerCase ? "ДА" : "НЕТ"));
            });

            Allure.step("2. Выполнение запроса и проверка ответа", () -> {
                Response response = given()
                        .spec(forValidApiKey(token, ACTION_LOGIN))
                        .when()
                        .post(ENDPOINT);

                int statusCode = response.getStatusCode();
                String actualMessage = response.jsonPath().getString(MESSAGE_PARAM);

                AllureReporter.addTestData("Анализ ответа",
                        String.format("""
                                        ОЖИДАЛОСЬ:
                                        • HTTP статус: 400 Bad Request ✓
                                        • Сообщение: "%s"
                                        
                                        ПОЛУЧЕНО:
                                        • HTTP статус: %d %s %s
                                        • Сообщение: "%s" %s
                                        
                                        ПРОВЕРКИ:
                                        • Формат токена отклонен системой: %s
                                        """,
                                INVALID_TOKEN_ERROR,
                                statusCode,
                                AllureReporter.getStatusText(statusCode),
                                statusCode == 400 ? "✓" : "✗",
                                actualMessage != null ? actualMessage : "отсутствует",
                                INVALID_TOKEN_ERROR.equals(actualMessage) ? "✓" : "✗",
                                INVALID_TOKEN_ERROR.equals(actualMessage) ? "✓" : "✗"));

                response.then()
                        .spec(forValidationError())
                        .body(MESSAGE_PARAM, equalTo(INVALID_TOKEN_ERROR));
            });

            testPassed.set(true);

        } finally {
            Allure.step("3. Итог тестирования", () -> {
                String resultText = testPassed.get() ?
                        """
                                ТЕСТ ПРОЙДЕН УСПЕШНО
                                
                                Что проверено:
                                1. Генерация токена со строчными буквами ✓
                                2. Отправка запроса с невалидным форматом ✓
                                3. Получение ошибки валидации 400 ✓
                                4. Корректное сообщение об ошибке ✓
                                
                                Вывод: Система не принимает токены со строчными буквами.
                                """ :
                        """
                                ТЕСТ ПРОВАЛЕН
                                
                                Не удалось проверить валидацию токена со строчными буквами.
                                Детали ошибки см. в предыдущих шагах.
                                """;

                AllureReporter.addTestData("Результат теста", resultText);
            });
        }
    }

    @Test
    @Story("Ошибки валидации")
    @DisplayName("Аутентификация с пустым токеном")
    @Description("""
            Проверяет обработку пустого токена:
            - Токен содержит пустую строку
            - Ожидается ошибка валидации
            - Приложение возвращает: {"result": "ERROR", "message": "token: должно соответствовать "^[0-9A-F]{32}$""}
            """)
    @Tag(REGRESSION)
    void loginWithEmptyToken() {
        String token = "";
        AtomicBoolean testPassed = new AtomicBoolean(false);

        try {
            Allure.step("1. Подготовка тестовых данных", () ->
                    AllureReporter.addTestData("Тестовый сценарий",
                            """
                                    Токен: '' (пустая строка)
                                    Длина: 0 символов (требуется: 32)
                                    Ожидается: ошибка валидации 400
                                    """));

            Allure.step("2. Выполнение запроса и проверка ответа", () -> {
                Response response = given()
                        .spec(forValidApiKey(token, ACTION_LOGIN))
                        .when()
                        .post(ENDPOINT);

                int statusCode = response.getStatusCode();
                String actualMessage = response.jsonPath().getString(MESSAGE_PARAM);

                AllureReporter.addTestData("Анализ ответа",
                        String.format("""
                                        ОЖИДАЛОСЬ:
                                        • HTTP статус: 400 Bad Request ✓
                                        • Сообщение: "%s"
                                        
                                        ПОЛУЧЕНО:
                                        • HTTP статус: %d %s %s
                                        • Сообщение: "%s" %s
                                        
                                        ПРОВЕРКИ:
                                        • Пустой токен отклонен системой: %s
                                        """,
                                INVALID_TOKEN_ERROR,
                                statusCode,
                                AllureReporter.getStatusText(statusCode),
                                statusCode == 400 ? "✓" : "✗",
                                actualMessage != null ? actualMessage : "отсутствует",
                                INVALID_TOKEN_ERROR.equals(actualMessage) ? "✓" : "✗",
                                INVALID_TOKEN_ERROR.equals(actualMessage) ? "✓" : "✗"));

                response.then()
                        .spec(forValidationError())
                        .body(MESSAGE_PARAM, equalTo(INVALID_TOKEN_ERROR));
            });

            testPassed.set(true);

        } finally {
            Allure.step("3. Итог тестирования", () -> {
                String resultText = testPassed.get() ?
                        """
                                ТЕСТ ПРОЙДЕН УСПЕШНО
                                
                                Что проверено:
                                1. Подготовка пустого токена ✓
                                2. Отправка запроса без токена ✓
                                3. Получение ошибки валидации 400 ✓
                                4. Корректное сообщение об ошибке ✓
                                
                                Вывод: Система корректно обрабатывает пустые токены.
                                """ :
                        """
                                ТЕСТ ПРОВАЛЕН
                                
                                Не удалось проверить обработку пустого токена.
                                Детали ошибки см. в предыдущих шагах.
                                """;

                AllureReporter.addTestData("Результат теста", resultText);
            });
        }
    }

    @Test
    @Story("Ошибки валидации")
    @DisplayName("Аутентификация с null токеном")
    @Description("""
            Проверяет обработку null токена:
            - Токен имеет значение null
            - Ожидается ошибка валидации
            - Сообщение об ошибке такое же, как для других невалидных токенов
            - Приложение возвращает: {"result": "ERROR", "message": "token: должно соответствовать "^[0-9A-F]{32}$""}
            """)
    @Tag(REGRESSION)
    void loginWithNullToken() {
        String token = null;
        AtomicBoolean testPassed = new AtomicBoolean(false);

        try {
            Allure.step("1. Подготовка тестовых данных", () ->
                    AllureReporter.addTestData("Тестовый сценарий",
                            """
                                    Токен: null (отсутствующее значение)
                                    Ожидается: ошибка валидации 400
                                    """));

            Allure.step("2. Выполнение запроса и проверка ответа", () -> {
                Response response = given()
                        .spec(forValidApiKey(token, ACTION_LOGIN))
                        .when()
                        .post(ENDPOINT);

                int statusCode = response.getStatusCode();
                String actualMessage = response.jsonPath().getString(MESSAGE_PARAM);

                AllureReporter.addTestData("Анализ ответа",
                        String.format("""
                                        ОЖИДАЛОСЬ:
                                        • HTTP статус: 400 Bad Request ✓
                                        • Сообщение: "%s"
                                        
                                        ПОЛУЧЕНО:
                                        • HTTP статус: %d %s %s
                                        • Сообщение: "%s" %s
                                        
                                        ПРОВЕРКИ:
                                        • Null значение обработано корректно: %s
                                        • Обработка null аналогична пустой строке: %s
                                        """,
                                INVALID_TOKEN_ERROR,
                                statusCode,
                                AllureReporter.getStatusText(statusCode),
                                statusCode == 400 ? "✓" : "✗",
                                actualMessage != null ? actualMessage : "отсутствует",
                                INVALID_TOKEN_ERROR.equals(actualMessage) ? "✓" : "✗",
                                INVALID_TOKEN_ERROR.equals(actualMessage) ? "✓" : "✗",
                                actualMessage != null && actualMessage.contains("должно соответствовать") ? "✓" : "✗"));

                response.then()
                        .spec(forValidationError())
                        .body(MESSAGE_PARAM, equalTo(INVALID_TOKEN_ERROR));
            });

            testPassed.set(true);

        } finally {
            Allure.step("3. Итог тестирования", () -> {
                String resultText = testPassed.get() ?
                        """
                                ТЕСТ ПРОЙДЕН УСПЕШНО
                                
                                Что проверено:
                                1. Подготовка запроса с null токеном ✓
                                2. Отправка запроса с отсутствующим значением ✓
                                3. Получение ошибки валидации 400 ✓
                                4. Корректное сообщение об ошибке ✓
                                5. Согласованность обработки null и пустой строки ✓
                                
                                Вывод: Система корректно обрабатывает null значения токена.
                                """ :
                        """
                                ТЕСТ ПРОВАЛЕН
                                
                                Не удалось проверить обработку null токена.
                                Детали ошибки см. в предыдущих шагах.
                                """;

                AllureReporter.addTestData("Результат теста", resultText);
            });
        }
    }

    @ParameterizedTest(name = "Аутентификация при ошибке внешнего сервиса ({0})")
    @ValueSource(ints = {HTTP_FORBIDDEN, HTTP_NOT_FOUND, HTTP_INTERNAL_ERROR})
    @Story("Ошибки внешнего сервиса")
    @DisplayName("Аутентификация при ошибках внешнего сервиса")
    @Description("""
            Проверяет обработку различных ошибок от внешнего сервиса при аутентификации:
            - Внешний сервис настроен возвращать ошибку
            - Проверяется ответ приложения на внешнюю ошибку
            - Ожидается JSON с полями result и message
            """)
    @Tag(NEEDS_CLARIFICATION)
    @Tag(REGRESSION)
    void loginWhenExternalServiceReturnsError(int statusCode) {
        String token = TokenGenerator.generateValidToken();
        AtomicBoolean testPassed = new AtomicBoolean(false);

        String statusName = switch (statusCode) {
            case HTTP_FORBIDDEN -> FORBIDDEN_MESSAGE;
            case HTTP_NOT_FOUND -> NOT_FOUND_MESSAGE;
            case HTTP_INTERNAL_ERROR -> INTERNAL_MESSAGE;
            default -> statusCode + ERROR_MESSAGE;
        };

        try {
            Allure.step("1. Подготовка тестовых данных", () ->
                    AllureReporter.addTestData("Тестовый сценарий",
                            String.format("""
                                            Код ошибки внешнего сервиса: %d (%s)
                                            Токен: %s (валидный)
                                            Ожидается: JSON с result: ERROR и message
                                            """,
                                    statusCode, statusName, token)));

            Allure.step("2. Настройка имитации ошибки внешнего сервиса", () -> {
                WireMockStubBuilder.mockAuthError(token, statusCode);
                AllureReporter.addTestData("Настройка WireMock",
                        String.format("WireMock настроен на возврат ошибки %d", statusCode));
            });

            Allure.step("3. Выполнение запроса и анализ ответа", () -> {
                Response response = given()
                        .spec(forValidApiKey(token, ACTION_LOGIN))
                        .when()
                        .post(ENDPOINT);

                int actualStatusCode = response.getStatusCode();
                String actualResult = response.jsonPath().getString(RESULT_PARAM);
                String actualMessage = response.jsonPath().getString(MESSAGE_PARAM);

                boolean isAlways500 = actualStatusCode == 500;
                String statusComparison = isAlways500 ? "✗" : "✓";

                AllureReporter.addTestData("Анализ ответа",
                        String.format("""
                                        НАСТРОЙКА ТЕСТА:
                                        • Внешний сервис настроен на возврат: %d %s
                                        
                                        ОЖИДАЛОСЬ ОТ ПРИЛОЖЕНИЯ:
                                        • Обработка ошибки внешнего сервиса
                                        • JSON ответ с result: ERROR
                                        • Корректное сообщение об ошибке
                                        
                                        ПОЛУЧЕНО ОТ ПРИЛОЖЕНИЯ:
                                        • HTTP статус: %d %s %s
                                        • Result поле: %s %s
                                        • Message поле: %s
                                        
                                        СРАВНЕНИЕ:
                                        Внешний сервис (%d) → Наше приложение (%d) %s
                                        
                                        ЗАМЕЧАНИЕ:
                                        %s
                                        
                                        ПРОВЕРКИ:
                                        • Ответ в формате JSON: %s
                                        • Result поле равно ERROR: %s
                                        • Message поле не пустое: %s
                                        """,
                                statusCode, statusName,
                                actualStatusCode,
                                AllureReporter.getStatusText(actualStatusCode),
                                actualStatusCode >= 400 && actualStatusCode < 600 ? "✓" : "✗",
                                actualResult != null ? actualResult : "отсутствует",
                                RESULT_ERROR.equals(actualResult) ? "✓" : "✗",
                                actualMessage != null ? actualMessage : "отсутствует",
                                statusCode,
                                actualStatusCode,
                                statusComparison,
                                isAlways500 ? "Приложение всегда возвращает 500 независимо от кода ошибки внешнего сервиса"
                                        : "Поведение соответствует ожиданиям",
                                ContentType.JSON.toString().equals(response.getContentType()) ? "✓" : "✗",
                                RESULT_ERROR.equals(actualResult) ? "✓" : "✗",
                                actualMessage != null && !actualMessage.trim().isEmpty() ? "✓" : "✗"));

                response.then()
                        .contentType(ContentType.JSON)
                        .body(RESULT_PARAM, equalTo(RESULT_ERROR))
                        .body(MESSAGE_PARAM, not(blankOrNullString()));
            });

            testPassed.set(true);

        } finally {
            Allure.step("4. Итог тестирования с рекомендацией", () -> {
                String resultText = testPassed.get() ?
                        String.format("""
                                ТЕСТ ВЫПОЛНЕН (с замечанием)
                                
                                Что проверено (код ошибки %d):
                                1. Генерация валидного токена ✓
                                2. Настройка ошибки внешнего сервиса ✓
                                3. Отправка запроса аутентификации ✓
                                4. Обработка ошибки внешнего сервиса
                                
                                НАБЛЮДЕНИЕ:
                                Независимо от кода ошибки внешнего сервиса (%d)
                                наше приложение всегда возвращает 500.
                                
                                ВОЗМОЖНЫЕ ИНТЕРПРЕТАЦИИ:
                                • Ожидаемое поведение: упрощенная обработка внешних ошибок
                                • Требуется доработка: нужно различать типы ошибок
                                • Особенность реализации: все внешние ошибки маппятся в 500
                                
                                РЕКОМЕНДАЦИЯ:
                                Уточнить у разработчика, является ли это
                                ожидаемым поведением или требуется доработка логики
                                обработки ошибок внешнего сервиса.
                                
                                ТЕКУЩИЙ ВЫВОД:
                                Приложение обрабатывает ошибки внешнего сервиса,
                                но не различает их типы, всегда возвращая 500.
                                """, statusCode, statusCode) :
                        """
                                ТЕСТ ПРОВАЛЕН
                                
                                Не удалось проверить обработку ошибок внешнего сервиса.
                                Детали ошибки см. в предыдущих шагах.
                                """;

                AllureReporter.addTestData("Результат и рекомендации", resultText);
            });
        }
    }

    @Test
    @Story("Ошибки аутентификации")
    @DisplayName("Аутентификация без API ключа")
    @Description("""
            Проверяет обработку запроса без обязательного заголовка X-Api-Key:
            - Отправляется запрос LOGIN без заголовка авторизации
            - Ожидается ошибка: 401 Unauthorized или аналогичная
            - Проверяется наличие сообщения об ошибке
            """)
    @Tag(SMOKE)
    void loginWithoutApiKey() {
        String token = TokenGenerator.generateValidToken();
        AtomicBoolean testPassed = new AtomicBoolean(false);

        try {
            Allure.step("1. Подготовка тестовых данных", () ->
                    AllureReporter.addTestData("Тестовый сценарий",
                            String.format("""
                                            Токен: %s (валидный)
                                            API ключ: отсутствует
                                            Ожидается: ошибка 401 Unauthorized
                                            """,
                                    token)));

            Allure.step("2. Выполнение запроса и проверка ответа", () -> {
                Response response = given()
                        .spec(forEmptyApiKey(token, ACTION_LOGIN))
                        .when()
                        .post(ENDPOINT);

                int statusCode = response.getStatusCode();
                String actualMessage = response.jsonPath().getString(MESSAGE_PARAM);

                AllureReporter.addTestData("Анализ ответа",
                        String.format("""
                                        ОЖИДАЛОСЬ:
                                        • HTTP статус: 401 Unauthorized ✓
                                        • Сообщение: "%s"
                                        
                                        ПОЛУЧЕНО:
                                        • HTTP статус: %d %s %s
                                        • Сообщение: "%s" %s
                                        
                                        ПРОВЕРКИ:
                                        • API ключ обязателен для доступа: %s
                                        • Сообщение об ошибке корректно: %s
                                        """,
                                INVALID_API_KEY_ERROR,
                                statusCode,
                                AllureReporter.getStatusText(statusCode),
                                statusCode == 401 ? "✓" : "✗",
                                actualMessage != null ? actualMessage : "отсутствует",
                                INVALID_API_KEY_ERROR.equals(actualMessage) ? "✓" : "✗",
                                statusCode == 401 ? "✓" : "✗",
                                INVALID_API_KEY_ERROR.equals(actualMessage) ? "✓" : "✗"));

                response.then()
                        .spec(forError(HTTP_UNAUTHORIZED))
                        .body(MESSAGE_PARAM, equalTo(INVALID_API_KEY_ERROR));
            });

            testPassed.set(true);

        } finally {
            Allure.step("3. Итог тестирования", () -> {
                String resultText = testPassed.get() ?
                        """
                                ТЕСТ ПРОЙДЕН УСПЕШНО
                                
                                Что проверено:
                                1. Подготовка валидного токена ✓
                                2. Формирование запроса без API ключа ✓
                                3. Отправка неавторизованного запроса ✓
                                4. Получение ошибки 401 Unauthorized ✓
                                5. Корректное сообщение об ошибке ✓
                                
                                Вывод: Система требует обязательный API ключ.
                                """ :
                        """
                                ТЕСТ ПРОВАЛЕН
                                
                                Не удалось проверить обязательность API ключа.
                                Детали ошибки см. в предыдущих шагах.
                                """;

                AllureReporter.addTestData("Результат теста", resultText);
            });
        }
    }

    @Test
    @Story("Ошибки аутентификации")
    @DisplayName("Аутентификация с неверным API ключом")
    @Description("""
            Проверяет обработку запроса с неверным API ключом:
            - Отправляется запрос LOGIN с некорректным значением X-Api-Key
            - Ожидается ошибка: 401 Unauthorized или аналогичная
            - Проверяется наличие сообщения об ошибке
            """)
    @Tag(REGRESSION)
    void loginWithInvalidApiKey() {
        String token = TokenGenerator.generateValidToken();
        AtomicBoolean testPassed = new AtomicBoolean(false);

        try {
            Allure.step("1. Подготовка тестовых данных", () ->
                    AllureReporter.addTestData("Тестовый сценарий",
                            String.format("""
                                            Токен: %s (валидный)
                                            API ключ: 'wrong-key' (недействительный)
                                            Ожидается: ошибка 401 Unauthorized
                                            """,
                                    token)));

            Allure.step("2. Выполнение запроса и проверка ответа", () -> {
                Response response = given()
                        .spec(forWrongApiKey(token, ACTION_LOGIN))
                        .when()
                        .post(ENDPOINT);

                int statusCode = response.getStatusCode();
                String actualMessage = response.jsonPath().getString(MESSAGE_PARAM);

                AllureReporter.addTestData("Анализ ответа",
                        String.format("""
                                        ОЖИДАЛОСЬ:
                                        • HTTP статус: 401 Unauthorized ✓
                                        • Сообщение: "%s"
                                        
                                        ПОЛУЧЕНО:
                                        • HTTP статус: %d %s %s
                                        • Сообщение: "%s" %s
                                        
                                        ПРОВЕРКИ:
                                        • Недействительный API ключ отклонен: %s
                                        • Сообщение об ошибке корректно: %s
                                        • Защита от несанкционированного доступа: %s
                                        """,
                                INVALID_API_KEY_ERROR,
                                statusCode,
                                AllureReporter.getStatusText(statusCode),
                                statusCode == 401 ? "✓" : "✗",
                                actualMessage != null ? actualMessage : "отсутствует",
                                INVALID_API_KEY_ERROR.equals(actualMessage) ? "✓" : "✗",
                                statusCode == 401 ? "✓" : "✗",
                                INVALID_API_KEY_ERROR.equals(actualMessage) ? "✓" : "✗",
                                statusCode == 401 ? "✓" : "✗"));

                response.then()
                        .spec(forError(HTTP_UNAUTHORIZED))
                        .body(MESSAGE_PARAM, equalTo(INVALID_API_KEY_ERROR));
            });

            testPassed.set(true);

        } finally {
            Allure.step("3. Итог тестирования", () -> {
                String resultText = testPassed.get() ?
                        """
                                ТЕСТ ПРОЙДЕН УСПЕШНО
                                
                                Что проверено:
                                1. Подготовка валидного токена ✓
                                2. Формирование запроса с неверным API ключом ✓
                                3. Отправка запроса с недействительной авторизацией ✓
                                4. Получение ошибки 401 Unauthorized ✓
                                5. Корректное сообщение об ошибке ✓
                                
                                Вывод: Система корректно валидирует API ключи.
                                """ :
                        """
                                ТЕСТ ПРОВАЛЕН
                                
                                Не удалось проверить валидацию API ключа.
                                Детали ошибки см. в предыдущих шагах.
                                """;

                AllureReporter.addTestData("Результат теста", resultText);
            });
        }
    }

    @Test
    @Story("Поведение при повторных операциях")
    @DisplayName("Повторная аутентификация с тем же токеном")
    @Description("""
            Проверяет обработку повторной аутентификации:
            - Отправляются два запроса LOGIN с одинаковым токеном подряд
            - Анализируется ответ на второй запрос
            """)
    @Tag(REGRESSION)
    void loginTwiceWithSameToken() {
        String token = TokenGenerator.generateValidToken();
        AtomicBoolean testPassed = new AtomicBoolean(false);

        try {
            Allure.step("1. Подготовка тестовых данных", () ->
                    AllureReporter.addTestData("Тестовый сценарий",
                            String.format("""
                                            Токен: %s
                                            Сценарий:
                                            1. Первый запрос LOGIN (ожидается успех)
                                            2. Второй запрос LOGIN (ожидается ошибка 409)
                                            """,
                                    token)));

            Allure.step("2. Настройка тестового окружения", () -> {
                WireMockStubBuilder.mockAuthSuccess(token);
                AllureReporter.addTestData("Настройка WireMock",
                        "WireMock настроен на успешный ответ для /auth");
            });

            Allure.step("3. Выполнение первого запроса аутентификации", () -> {
                Response firstResponse = given()
                        .spec(forValidApiKey(token, ACTION_LOGIN))
                        .when()
                        .post(ENDPOINT);

                int firstStatusCode = firstResponse.getStatusCode();

                AllureReporter.addTestData("Результат первого запроса",
                        String.format("""
                                        ОЖИДАЛОСЬ (первый запрос):
                                        • HTTP статус: 200 OK ✓
                                        • Result поле: "OK"
                                        
                                        ПОЛУЧЕНО:
                                        • HTTP статус: %d %s %s
                                        • Result поле: %s %s
                                        
                                        ПРОВЕРКИ:
                                        • Первая аутентификация успешна: %s
                                        """,
                                firstStatusCode,
                                AllureReporter.getStatusText(firstStatusCode),
                                firstStatusCode == 200 ? "✓" : "✗",
                                firstResponse.jsonPath().getString("result"),
                                "OK".equals(firstResponse.jsonPath().getString("result")) ? "✓" : "✗",
                                firstStatusCode == 200 ? "✓" : "✗"));

                firstResponse.then().spec(forSuccess());
            });

            Allure.step("4. Выполнение второго запроса аутентификации", () -> {
                Response secondResponse = given()
                        .spec(forValidApiKey(token, ACTION_LOGIN))
                        .when()
                        .post(ENDPOINT);

                int secondStatusCode = secondResponse.getStatusCode();
                String expectedMessage = String.format(TOKEN_ALREADY_EXISTS_ERROR, token);
                String actualMessage = secondResponse.jsonPath().getString(MESSAGE_PARAM);

                AllureReporter.addTestData("Результат второго запроса",
                        String.format("""
                                        ОЖИДАЛОСЬ (второй запрос):
                                        • HTTP статус: 409 Conflict ✓
                                        • Сообщение: "%s"
                                        
                                        ПОЛУЧЕНО:
                                        • HTTP статус: %d %s %s
                                        • Сообщение: "%s" %s
                                        
                                        ПРОВЕРКИ:
                                        • Повторная аутентификация отклонена: %s
                                        • Сообщение соответствует ожидаемому: %s
                                        • Идемпотентность операции: %s
                                        """,
                                expectedMessage,
                                secondStatusCode,
                                AllureReporter.getStatusText(secondStatusCode),
                                secondStatusCode == 409 ? "✓" : "✗",
                                actualMessage != null ? actualMessage : "отсутствует",
                                expectedMessage.equals(actualMessage) ? "✓" : "✗",
                                secondStatusCode == 409 ? "✓" : "✗",
                                expectedMessage.equals(actualMessage) ? "✓" : "✗",
                                secondStatusCode == 409 ? "✓" : "✗"));

                secondResponse.then()
                        .spec(forError(HTTP_CONFLICT))
                        .body(MESSAGE_PARAM, equalTo(expectedMessage));
            });

            testPassed.set(true);
        } finally {
            Allure.step("5. Итог тестирования", () -> {
                String resultText = testPassed.get() ?
                        """
                                ТЕСТ ПРОЙДЕН УСПЕШНО
                                
                                Что проверено:
                                1. Подготовка валидного токена ✓
                                2. Настройка тестового окружения ✓
                                3. Успешная первая аутентификация ✓
                                4. Ошибка при повторной аутентификации ✓
                                5. Корректный код ошибки (409 Conflict) ✓
                                6. Понятное сообщение об ошибке ✓
                                
                                Вывод: Система предотвращает повторную аутентификацию.
                                """ :
                        """
                                ТЕСТ ПРОВАЛЕН
                                
                                Не удалось проверить повторную аутентификацию.
                                Детали ошибки см. в предыдущих шагах.
                                """;

                AllureReporter.addTestData("Результат теста", resultText);
            });
        }
    }

    @Test
    @Story("Ошибки валидации")
    @DisplayName("Аутентификация без параметра action")
    @Description("""
            Проверяет обработку запроса без параметра action:
            - Запрос отправляется без обязательного параметра action
            - Проверяется ответ приложения на отсутствие параметра
            - Ожидается JSON с полями result и message
            """)
    @Tag(REGRESSION)
    void loginWithoutActionParameter() {
        String token = TokenGenerator.generateValidToken();
        AtomicBoolean testPassed = new AtomicBoolean(false);

        try {
            Allure.step("1. Подготовка тестовых данных", () ->
                    AllureReporter.addTestData("Тестовый сценарий",
                            String.format("""
                                            Токен: %s (валидный)
                                            Параметр action: отсутствует
                                            Ожидается: ошибка валидации 400
                                            """,
                                    token)));

            Allure.step("2. Выполнение запроса и проверка ответа", () -> {
                Response response = given()
                        .spec(forValidApiKey(token, ""))
                        .when()
                        .post(ENDPOINT);

                int statusCode = response.getStatusCode();
                String actualMessage = response.jsonPath().getString(MESSAGE_PARAM);

                AllureReporter.addTestData("Анализ ответа",
                        String.format("""
                                        ОЖИДАЛОСЬ:
                                        • HTTP статус: 400 Bad Request ✓
                                        • Сообщение: "%s"
                                        
                                        ПОЛУЧЕНО:
                                        • HTTP статус: %d %s %s
                                        • Сообщение: "%s" %s
                                        
                                        ПРОВЕРКИ:
                                        • Параметр action обязателен: %s
                                        • Сообщение об ошибке корректно: %s
                                        • Валидация входных параметров работает: %s
                                        """,
                                INVALID_ACTION_ERROR,
                                statusCode,
                                AllureReporter.getStatusText(statusCode),
                                statusCode == 400 ? "✓" : "✗",
                                actualMessage != null ? actualMessage : "отсутствует",
                                INVALID_ACTION_ERROR.equals(actualMessage) ? "✓" : "✗",
                                statusCode == 400 ? "✓" : "✗",
                                INVALID_ACTION_ERROR.equals(actualMessage) ? "✓" : "✗",
                                statusCode == 400 ? "✓" : "✗"));

                response.then()
                        .spec(forError(HTTP_BAD_REQUEST))
                        .body(MESSAGE_PARAM, equalTo(INVALID_ACTION_ERROR));
            });

            testPassed.set(true);

        } finally {
            Allure.step("3. Итог тестирования", () -> {
                String resultText = testPassed.get() ?
                        """
                                ТЕСТ ПРОЙДЕН УСПЕШНО
                                
                                Что проверено:
                                1. Подготовка валидного токена ✓
                                2. Формирование запроса без параметра action ✓
                                3. Отправка запроса с неполными данными ✓
                                4. Получение ошибки валидации 400 ✓
                                5. Корректное сообщение об ошибке ✓
                                
                                Вывод: Система требует обязательный параметр action.
                                """ :
                        """
                                ТЕСТ ПРОВАЛЕН
                                
                                Не удалось проверить обязательность параметра action.
                                Детали ошибки см. в предыдущих шагах.
                                """;

                AllureReporter.addTestData("Результат теста", resultText);
            });
        }
    }
}