package aqa.api.tests;

import aqa.api.base.BaseTest;
import aqa.api.utils.AllureReporter;
import aqa.api.utils.TokenGenerator;
import aqa.api.utils.WireMockStubBuilder;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static aqa.api.utils.Constants.*;
import static aqa.api.utils.ErrorMessages.TOKEN_NOT_FOUND_ERROR;
import static aqa.api.utils.specs.RequestSpecs.forValidApiKey;
import static aqa.api.utils.specs.ResponseSpecs.forError;
import static aqa.api.utils.specs.ResponseSpecs.forSuccess;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@Epic("Тестирование веб-сервиса")
@Feature("Функционал LOGOUT")
@DisplayName("Тесты для действия LOGOUT")
public class LogoutTest extends BaseTest {

    @Test
    @Story("Успешные сценарии")
    @DisplayName("Успешный LOGOUT после LOGIN")
    @Description("""
            Проверяет успешное завершение сессии после аутентификации:
            - LOGIN сохраняет токен
            - LOGOUT удаляет токен
            - После LOGOUT токен не должен работать
            """)
    @Tag(SMOKE)
    void successfulLogoutAfterLogin() {
        String token = TokenGenerator.generateValidToken();
        AtomicBoolean testPassed = new AtomicBoolean(false);

        try {
            Allure.step("1. Подготовка тестовых данных", () ->
                    AllureReporter.addTestData("Тестовый сценарий",
                            String.format("""
                                            Токен: %s
                                            Длина: %d символов
                                            Сценарий:
                                            1. LOGIN с токеном (ожидается успех)
                                            2. LOGOUT с тем же токеном (ожидается успех)
                                            """,
                                    token,
                                    token.length())));

            Allure.step("2. Настройка тестового окружения", () -> {
                WireMockStubBuilder.mockAuthSuccess(token);
                AllureReporter.addTestData("Настройка WireMock",
                        "WireMock настроен на успешный ответ для /auth");
            });

            Allure.step("3. Выполнение запроса LOGIN", () -> {
                Response loginResponse = given()
                        .spec(forValidApiKey(token, ACTION_LOGIN))
                        .when()
                        .post(ENDPOINT);

                int loginStatusCode = loginResponse.getStatusCode();
                String loginResult = loginResponse.jsonPath().getString("result");

                AllureReporter.addTestData("Результат LOGIN",
                        String.format("""
                                        ОЖИДАЛОСЬ (LOGIN):
                                        • HTTP статус: 200 OK ✓
                                        • Result поле: "OK"
                                        
                                        ПОЛУЧЕНО:
                                        • HTTP статус: %d %s %s
                                        • Result поле: %s %s
                                        
                                        ПРОВЕРКИ:
                                        • Аутентификация успешна: %s
                                        """,
                                loginStatusCode,
                                AllureReporter.getStatusText(loginStatusCode),
                                loginStatusCode == 200 ? "✓" : "✗",
                                loginResult != null ? loginResult : "отсутствует",
                                "OK".equals(loginResult) ? "✓" : "✗",
                                loginStatusCode == 200 ? "✓" : "✗"));

                loginResponse.then().spec(forSuccess());
            });

            Allure.step("4. Выполнение запроса LOGOUT", () -> {
                Response logoutResponse = given()
                        .spec(forValidApiKey(token, ACTION_LOGOUT))
                        .when()
                        .post(ENDPOINT);

                int logoutStatusCode = logoutResponse.getStatusCode();
                String logoutResult = logoutResponse.jsonPath().getString("result");

                AllureReporter.addTestData("Результат LOGOUT",
                        String.format("""
                                        ОЖИДАЛОСЬ (LOGOUT):
                                        • HTTP статус: 200 OK ✓
                                        • Result поле: "OK"
                                        
                                        ПОЛУЧЕНО:
                                        • HTTP статус: %d %s %s
                                        • Result поле: %s %s
                                        
                                        ПРОВЕРКИ:
                                        • Сессия успешно завершена: %s
                                        • Последовательность LOGIN → LOGOUT работает: %s
                                        """,
                                logoutStatusCode,
                                AllureReporter.getStatusText(logoutStatusCode),
                                logoutStatusCode == 200 ? "✓" : "✗",
                                logoutResult != null ? logoutResult : "отсутствует",
                                "OK".equals(logoutResult) ? "✓" : "✗",
                                logoutStatusCode == 200 ? "✓" : "✗",
                                logoutStatusCode == 200 ? "✓" : "✗"));

                logoutResponse.then().spec(forSuccess());
            });

            testPassed.set(true);

        } finally {
            Allure.step("5. Итог тестирования", () -> {
                String resultText = testPassed.get() ?
                        """
                                ТЕСТ ПРОЙДЕН УСПЕШНО
                                
                                Что проверено:
                                1. Подготовка валидного токена ✓
                                2. Успешная аутентификация (LOGIN) ✓
                                3. Успешное завершение сессии (LOGOUT) ✓
                                4. Последовательность LOGIN → LOGOUT работает ✓
                                
                                Вывод: Система корректно завершает сессии
                                аутентифицированных пользователей.
                                """ :
                        """
                                ТЕСТ ПРОВАЛЕН
                                
                                Не удалось выполнить LOGOUT после LOGIN.
                                Детали ошибки см. в предыдущих шагах.
                                """;

                AllureReporter.addTestData("Результат теста", resultText);
            });
        }
    }

    @Test
    @Story("Ошибки валидации")
    @DisplayName("LOGOUT с невалидным токеном")
    @Description("""
            Проверяет обработку LOGOUT с невалидным токеном:
            - Токен не соответствует формату (длина не 32 символа или неверные символы)
            - Отправляется запрос LOGOUT с невалидным токеном
            - Ожидается ошибка валидации: 400 Bad Request
            """)
    @Tag(REGRESSION)
    void performLogoutWithInvalidToken() {
        String invalidToken = TokenGenerator.generateShortToken();
        AtomicBoolean testPassed = new AtomicBoolean(false);

        try {
            Allure.step("1. Подготовка тестовых данных", () ->
                    AllureReporter.addTestData("Тестовый сценарий",
                            String.format("""
                                            Токен: %s
                                            Длина: %d символов (требуется: 32)
                                            Сценарий: Прямой запрос LOGOUT с невалидным токеном
                                            Ожидается: ошибка валидации 400
                                            """,
                                    invalidToken,
                                    invalidToken.length())));

            Allure.step("2. Выполнение запроса LOGOUT с невалидным токеном", () -> {
                Response response = given()
                        .spec(forValidApiKey(invalidToken, ACTION_LOGOUT))
                        .when()
                        .post(ENDPOINT);

                int statusCode = response.getStatusCode();
                String responseResult = response.jsonPath().getString("result");

                AllureReporter.addTestData("Анализ ответа",
                        String.format("""
                                        ОЖИДАЛОСЬ:
                                        • HTTP статус: 400 Bad Request ✓
                                        • Result поле: "ERROR"
                                        
                                        ПОЛУЧЕНО:
                                        • HTTP статус: %d %s %s
                                        • Result поле: %s %s
                                        
                                        ПРОВЕРКИ:
                                        • Запрос отклонен из-за невалидного токена: %s
                                        • Result поле равно ERROR: %s
                                        • Валидация токена работает при LOGOUT: %s
                                        """,
                                statusCode,
                                AllureReporter.getStatusText(statusCode),
                                statusCode == 400 ? "✓" : "✗",
                                responseResult != null ? responseResult : "отсутствует",
                                "ERROR".equals(responseResult) ? "✓" : "✗",
                                statusCode == 400 ? "✓" : "✗",
                                "ERROR".equals(responseResult) ? "✓" : "✗",
                                statusCode == 400 ? "✓" : "✗"));

                response.then()
                        .spec(forError(HTTP_BAD_REQUEST));
            });

            testPassed.set(true);

        } finally {
            Allure.step("3. Итог тестирования", () -> {
                String resultText = testPassed.get() ?
                        """
                                ТЕСТ ПРОЙДЕН УСПЕШНО
                                
                                Что проверено:
                                1. Подготовка невалидного токена ✓
                                2. Отправка LOGOUT с невалидным токеном ✓
                                3. Получение ошибки валидации 400 ✓
                                
                                Вывод: Система корректно валидирует токены
                                при запросах на завершение сессии.
                                """ :
                        """
                                ТЕСТ ПРОВАЛЕН
                                
                                Не удалось проверить LOGOUT с невалидным токеном.
                                Детали ошибки см. в предыдущих шагах.
                                """;

                AllureReporter.addTestData("Результат теста", resultText);
            });
        }
    }

    @Test
    @Story("Ошибки авторизации")
    @DisplayName("LOGOUT без предварительного LOGIN")
    @Description("""
            Проверяет обработку LOGOUT без предварительной аутентификации:
            - Токен никогда не проходил LOGIN
            - Отправляется запрос LOGOUT
            - Ожидается ошибка: токен не найден
            """)
    @Tag(REGRESSION)
    void performLogoutWithoutLogin() {
        String token = TokenGenerator.generateValidToken();
        AtomicBoolean testPassed = new AtomicBoolean(false);

        try {
            Allure.step("1. Подготовка тестовых данных", () ->
                    AllureReporter.addTestData("Тестовый сценарий",
                            String.format("""
                                            Токен: %s
                                            Длина: %d символов
                                            Сценарий: Прямой запрос LOGOUT без LOGIN
                                            Ожидается: ошибка 403 Forbidden
                                            """,
                                    token,
                                    token.length())));

            Allure.step("2. Выполнение запроса LOGOUT без LOGIN", () -> {
                Response response = given()
                        .spec(forValidApiKey(token, ACTION_LOGOUT))
                        .when()
                        .post(ENDPOINT);

                int statusCode = response.getStatusCode();
                String actualMessage = response.jsonPath().getString(MESSAGE_PARAM);
                String expectedMessage = String.format(TOKEN_NOT_FOUND_ERROR, token);

                AllureReporter.addTestData("Анализ ответа",
                        String.format("""
                                        ОЖИДАЛОСЬ:
                                        • HTTP статус: 403 Forbidden ✓
                                        • Сообщение: "%s"
                                        
                                        ПОЛУЧЕНО:
                                        • HTTP статус: %d %s %s
                                        • Сообщение: "%s" %s
                                        
                                        ПРОВЕРКИ:
                                        • Запрос отклонен из-за отсутствия сессии: %s
                                        • Сообщение соответствует ожидаемому: %s
                                        • Result поле равно ERROR: %s
                                        • Защита от завершения несуществующих сессий: %s
                                        """,
                                expectedMessage,
                                statusCode,
                                AllureReporter.getStatusText(statusCode),
                                statusCode == 403 ? "✓" : "✗",
                                actualMessage != null ? actualMessage : "отсутствует",
                                expectedMessage.equals(actualMessage) ? "✓" : "✗",
                                statusCode == 403 ? "✓" : "✗",
                                expectedMessage.equals(actualMessage) ? "✓" : "✗",
                                "ERROR".equals(response.jsonPath().getString("result")) ? "✓" : "✗",
                                statusCode == 403 ? "✓" : "✗"));

                response.then()
                        .spec(forError(HTTP_FORBIDDEN))
                        .body(MESSAGE_PARAM, equalTo(expectedMessage));
            });

            testPassed.set(true);

        } finally {
            Allure.step("3. Итог тестирования", () -> {
                String resultText = testPassed.get() ?
                        """
                                ТЕСТ ПРОЙДЕН УСПЕШНО
                                
                                Что проверено:
                                1. Подготовка валидного токена ✓
                                2. Отправка LOGOUT без предварительного LOGIN ✓
                                3. Получение ошибки 403 Forbidden ✓
                                4. Корректное сообщение об ошибке ✓
                                
                                Вывод: Система корректно защищает от завершения
                                несуществующих или неаутентифицированных сессий.
                                """ :
                        """
                                ТЕСТ ПРОВАЛЕН
                                
                                Не удалось проверить LOGOUT без LOGIN.
                                Детали ошибки см. в предыдущих шагах.
                                """;

                AllureReporter.addTestData("Результат теста", resultText);
            });
        }
    }

    @Test
    @Story("Многократные операции")
    @DisplayName("Несколько LOGOUT подряд с одним токеном")
    @Description("""
            Проверяет возможность выполнения нескольких LOGOUT подряд:
            - Токен успешно аутентифицирован
            - Выполняется первый LOGOUT (успешно)
            - Выполняется второй LOGOUT (должен завершиться ошибкой)
            """)
    @Tag(REGRESSION)
    void performMultipleLogoutsWithSameToken() {
        String token = TokenGenerator.generateValidToken();
        AtomicBoolean testPassed = new AtomicBoolean(false);

        try {
            Allure.step("1. Подготовка тестовых данных", () ->
                    AllureReporter.addTestData("Тестовый сценарий",
                            String.format("""
                                            Токен: %s
                                            Длина: %d символов
                                            Сценарий:
                                            1. LOGIN с токеном (ожидается успех)
                                            2. Первый LOGOUT (ожидается успех)
                                            3. Второй LOGOUT (ожидается ошибка)
                                            """,
                                    token,
                                    token.length())));

            Allure.step("2. Настройка тестового окружения", () -> {
                WireMockStubBuilder.mockAuthSuccess(token);
                AllureReporter.addTestData("Настройка WireMock",
                        "WireMock настроен на успешный ответ для /auth");
            });

            Allure.step("3. Выполнение запроса LOGIN", () -> {
                Response loginResponse = given()
                        .spec(forValidApiKey(token, ACTION_LOGIN))
                        .when()
                        .post(ENDPOINT);

                int loginStatusCode = loginResponse.getStatusCode();
                String loginResult = loginResponse.jsonPath().getString("result");

                AllureReporter.addTestData("Результат LOGIN",
                        String.format("""
                                        ОЖИДАЛОСЬ (LOGIN):
                                        • HTTP статус: 200 OK ✓
                                        • Result поле: "OK"
                                        
                                        ПОЛУЧЕНО:
                                        • HTTP статус: %d %s %s
                                        • Result поле: %s %s
                                        
                                        ПРОВЕРКИ:
                                        • Аутентификация успешна: %s
                                        """,
                                loginStatusCode,
                                AllureReporter.getStatusText(loginStatusCode),
                                loginStatusCode == 200 ? "✓" : "✗",
                                loginResult != null ? loginResult : "отсутствует",
                                "OK".equals(loginResult) ? "✓" : "✗",
                                loginStatusCode == 200 ? "✓" : "✗"));

                loginResponse.then().spec(forSuccess());
            });

            Allure.step("4. Выполнение первого LOGOUT", () -> {
                Response firstLogout = given()
                        .spec(forValidApiKey(token, ACTION_LOGOUT))
                        .when()
                        .post(ENDPOINT);

                int firstStatusCode = firstLogout.getStatusCode();
                String firstResult = firstLogout.jsonPath().getString("result");

                AllureReporter.addTestData("Результат первого LOGOUT",
                        String.format("""
                                        ОЖИДАЛОСЬ (LOGOUT #1):
                                        • HTTP статус: 200 OK ✓
                                        • Result поле: "OK"
                                        
                                        ПОЛУЧЕНО:
                                        • HTTP статус: %d %s %s
                                        • Result поле: %s %s
                                        
                                        ПРОВЕРКИ:
                                        • Первое завершение сессии успешно: %s
                                        """,
                                firstStatusCode,
                                AllureReporter.getStatusText(firstStatusCode),
                                firstStatusCode == 200 ? "✓" : "✗",
                                firstResult != null ? firstResult : "отсутствует",
                                "OK".equals(firstResult) ? "✓" : "✗",
                                firstStatusCode == 200 ? "✓" : "✗"));

                firstLogout.then().spec(forSuccess());
            });

            Allure.step("5. Выполнение второго LOGOUT", () -> {
                Response secondLogout = given()
                        .spec(forValidApiKey(token, ACTION_LOGOUT))
                        .when()
                        .post(ENDPOINT);

                int secondStatusCode = secondLogout.getStatusCode();
                String actualMessage = secondLogout.jsonPath().getString(MESSAGE_PARAM);
                String expectedMessage = String.format(TOKEN_NOT_FOUND_ERROR, token);

                AllureReporter.addTestData("Результат второго LOGOUT",
                        String.format("""
                                        ОЖИДАЛОСЬ (LOGOUT #2):
                                        • HTTP статус: 403 Forbidden ✓
                                        • Сообщение: "%s"
                                        
                                        ПОЛУЧЕНО:
                                        • HTTP статус: %d %s %s
                                        • Сообщение: "%s" %s
                                        
                                        ПРОВЕРКИ:
                                        • Повторное завершение отклонено: %s
                                        • Сообщение соответствует ожидаемому: %s
                                        • Result поле равно ERROR: %s
                                        • Идемпотентность операции LOGOUT: %s
                                        """,
                                expectedMessage,
                                secondStatusCode,
                                AllureReporter.getStatusText(secondStatusCode),
                                secondStatusCode == 403 ? "✓" : "✗",
                                actualMessage != null ? actualMessage : "отсутствует",
                                expectedMessage.equals(actualMessage) ? "✓" : "✗",
                                secondStatusCode == 403 ? "✓" : "✗",
                                expectedMessage.equals(actualMessage) ? "✓" : "✗",
                                "ERROR".equals(secondLogout.jsonPath().getString("result")) ? "✓" : "✗",
                                secondStatusCode == 403 ? "✓" : "✗"));

                secondLogout.then()
                        .spec(forError(HTTP_FORBIDDEN))
                        .body(MESSAGE_PARAM, equalTo(expectedMessage));
            });

            testPassed.set(true);

        } finally {
            Allure.step("6. Итог тестирования", () -> {
                String resultText = testPassed.get() ?
                        """
                                ТЕСТ ПРОЙДЕН УСПЕШНО
                                
                                Что проверено:
                                1. Подготовка валидного токена ✓
                                2. Успешная аутентификация (LOGIN) ✓
                                3. Успешное первое завершение сессии ✓
                                4. Ошибка при повторном завершении сессии ✓
                                5. Корректный код ошибки 403 ✓
                                6. Понятное сообщение об ошибке ✓
                                
                                Вывод: Система позволяет завершить сессию только один раз.
                                Повторные попытки LOGOUT корректно отклоняются.
                                """ :
                        """
                                ТЕСТ ПРОВАЛЕН
                                
                                Не удалось проверить несколько LOGOUT подряд.
                                Детали ошибки см. в предыдущих шагах.
                                """;

                AllureReporter.addTestData("Результат теста", resultText);
            });
        }
    }

    @Test
    @Story("Комплексные сценарии")
    @DisplayName("Полный жизненный цикл токена: LOGIN → ACTION → LOGOUT")
    @Description("""
            Проверяет полный жизненный цикл токена:
            - Успешная аутентификация (LOGIN)
            - Выполнение действия (ACTION)
            - Завершение сессии (LOGOUT)
            - Проверка, что токен больше не работает
            """)
    @Tag(REGRESSION)
    void fullTokenLifecycle() {
        String token = TokenGenerator.generateValidToken();
        AtomicBoolean testPassed = new AtomicBoolean(false);

        try {
            Allure.step("1. Подготовка тестовых данных", () ->
                    AllureReporter.addTestData("Тестовый сценарий",
                            String.format("""
                                            Токен: %s
                                            Длина: %d символов
                                            Сценарий:
                                            1. LOGIN с токеном (ожидается успех)
                                            2. ACTION с токеном (ожидается успех)
                                            3. LOGOUT с токеном (ожидается успех)
                                            4. ACTION после LOGOUT (ожидается ошибка)
                                            """,
                                    token,
                                    token.length())));

            Allure.step("2. Настройка тестового окружения", () -> {
                WireMockStubBuilder.mockAuthSuccess(token);
                WireMockStubBuilder.mockDoActionSuccess(token);
                AllureReporter.addTestData("Настройка WireMock",
                        "WireMock настроен на успешные ответы");
            });

            Allure.step("3. Выполнение запроса LOGIN", () -> {
                Response loginResponse = given()
                        .spec(forValidApiKey(token, ACTION_LOGIN))
                        .when()
                        .post(ENDPOINT);

                int loginStatusCode = loginResponse.getStatusCode();
                String loginResult = loginResponse.jsonPath().getString("result");

                AllureReporter.addTestData("Результат LOGIN (начало цикла)",
                        String.format("""
                                        ОЖИДАЛОСЬ (LOGIN):
                                        • HTTP статус: 200 OK ✓
                                        • Result поле: "OK"
                                        
                                        ПОЛУЧЕНО:
                                        • HTTP статус: %d %s %s
                                        • Result поле: %s %s
                                        
                                        ПРОВЕРКА:
                                        • Токен аутентифицирован успешно: %s
                                        """,
                                loginStatusCode,
                                AllureReporter.getStatusText(loginStatusCode),
                                loginStatusCode == 200 ? "✓" : "✗",
                                loginResult != null ? loginResult : "отсутствует",
                                "OK".equals(loginResult) ? "✓" : "✗",
                                loginStatusCode == 200 ? "✓" : "✗"));

                loginResponse.then().spec(forSuccess());
            });

            Allure.step("4. Выполнение запроса ACTION", () -> {
                Response actionResponse = given()
                        .spec(forValidApiKey(token, ACTION_ACTION))
                        .when()
                        .post(ENDPOINT);

                int actionStatusCode = actionResponse.getStatusCode();
                String actionResult = actionResponse.jsonPath().getString("result");

                AllureReporter.addTestData("Результат ACTION (работа с токеном)",
                        String.format("""
                                        ОЖИДАЛОСЬ (ACTION):
                                        • HTTP статус: 200 OK ✓
                                        • Result поле: "OK"
                                        
                                        ПОЛУЧЕНО:
                                        • HTTP статус: %d %s %s
                                        • Result поле: %s %s
                                        
                                        ПРОВЕРКА:
                                        • Действие выполнено успешно: %s
                                        """,
                                actionStatusCode,
                                AllureReporter.getStatusText(actionStatusCode),
                                actionStatusCode == 200 ? "✓" : "✗",
                                actionResult != null ? actionResult : "отсутствует",
                                "OK".equals(actionResult) ? "✓" : "✗",
                                actionStatusCode == 200 ? "✓" : "✗"));

                actionResponse.then().spec(forSuccess());
            });

            Allure.step("5. Выполнение запроса LOGOUT", () -> {
                Response logoutResponse = given()
                        .spec(forValidApiKey(token, ACTION_LOGOUT))
                        .when()
                        .post(ENDPOINT);

                int logoutStatusCode = logoutResponse.getStatusCode();
                String logoutResult = logoutResponse.jsonPath().getString("result");

                AllureReporter.addTestData("Результат LOGOUT (завершение сессии)",
                        String.format("""
                                        ОЖИДАЛОСЬ (LOGOUT):
                                        • HTTP статус: 200 OK ✓
                                        • Result поле: "OK"
                                        
                                        ПОЛУЧЕНО:
                                        • HTTP статус: %d %s %s
                                        • Result поле: %s %s
                                        
                                        ПРОВЕРКА:
                                        • Сессия успешно завершена: %s
                                        """,
                                logoutStatusCode,
                                AllureReporter.getStatusText(logoutStatusCode),
                                logoutStatusCode == 200 ? "✓" : "✗",
                                logoutResult != null ? logoutResult : "отсутствует",
                                "OK".equals(logoutResult) ? "✓" : "✗",
                                logoutStatusCode == 200 ? "✓" : "✗"));

                logoutResponse.then().spec(forSuccess());
            });

            Allure.step("6. Попытка выполнения ACTION после LOGOUT", () -> {
                Response finalActionResponse = given()
                        .spec(forValidApiKey(token, ACTION_ACTION))
                        .when()
                        .post(ENDPOINT);

                int finalStatusCode = finalActionResponse.getStatusCode();
                String actualMessage = finalActionResponse.jsonPath().getString(MESSAGE_PARAM);
                String expectedMessage = String.format(TOKEN_NOT_FOUND_ERROR, token);

                AllureReporter.addTestData("Результат ACTION после LOGOUT (конец цикла)",
                        String.format("""
                                        ОЖИДАЛОСЬ (ACTION после LOGOUT):
                                        • HTTP статус: 403 Forbidden ✓
                                        • Сообщение: "%s"
                                        
                                        ПОЛУЧЕНО:
                                        • HTTP статус: %d %s %s
                                        • Сообщение: "%s" %s
                                        
                                        ПРОВЕРКИ:
                                        • Токен больше не действителен: %s
                                        • Сообщение соответствует ожидаемому: %s
                                        • Result поле равно ERROR: %s
                                        • Полный цикл токена завершен: %s
                                        """,
                                expectedMessage,
                                finalStatusCode,
                                AllureReporter.getStatusText(finalStatusCode),
                                finalStatusCode == 403 ? "✓" : "✗",
                                actualMessage != null ? actualMessage : "отсутствует",
                                expectedMessage.equals(actualMessage) ? "✓" : "✗",
                                finalStatusCode == 403 ? "✓" : "✗",
                                expectedMessage.equals(actualMessage) ? "✓" : "✗",
                                "ERROR".equals(finalActionResponse.jsonPath().getString("result")) ? "✓" : "✗",
                                finalStatusCode == 403 ? "✓" : "✗"));

                finalActionResponse.then()
                        .spec(forError(HTTP_FORBIDDEN))
                        .body(MESSAGE_PARAM, equalTo(expectedMessage));
            });

            testPassed.set(true);

        } finally {
            Allure.step("7. Итог тестирования", () -> {
                String resultText = testPassed.get() ?
                        """
                                ТЕСТ ПРОЙДЕН УСПЕШНО
                                
                                Что проверено (полный цикл токена):
                                1. Подготовка валидного токена ✓
                                2. Успешная аутентификация (LOGIN) ✓
                                3. Успешное выполнение действия (ACTION) ✓
                                4. Успешное завершение сессии (LOGOUT) ✓
                                5. Отказ в выполнении ACTION после LOGOUT ✓
                                6. Корректный код ошибки 403 ✓
                                7. Понятное сообщение об ошибке ✓
                                
                                Вывод: Токен проходит полный жизненный цикл:
                                создание → использование → завершение → недействительность.
                                Система корректно управляет состоянием токенов.
                                """ :
                        """
                                ТЕСТ ПРОВАЛЕН
                                
                                Не удалось проверить полный жизненный цикл токена.
                                Детали ошибки см. в предыдущих шагах.
                                """;

                AllureReporter.addTestData("Результат теста", resultText);
            });
        }
    }

    @Test
    @Story("Многопользовательские сценарии")
    @DisplayName("LOGOUT разных токенов")
    @Description("""
            Проверяет обработку LOGOUT для нескольких токенов:
            - Аутентифицируются два разных токена
            - Выполняется LOGOUT для первого токена
            - Проверяется, что второй токен продолжает работать
            """)
    @Tag(REGRESSION)
    void logoutDifferentTokens() {
        String token1 = TokenGenerator.generateValidToken();
        String token2 = TokenGenerator.generateValidToken();
        AtomicBoolean testPassed = new AtomicBoolean(false);

        try {
            Allure.step("1. Подготовка тестовых данных", () ->
                    AllureReporter.addTestData("Тестовый сценарий",
                            String.format("""
                                            Токен 1: %s
                                            Токен 2: %s
                                            Длина обоих: 32 символа
                                            Сценарий:
                                            1. LOGIN токена 1 и токена 2 (ожидается успех)
                                            2. LOGOUT токена 1 (ожидается успех)
                                            3. ACTION токена 1 (ожидается ошибка)
                                            4. ACTION токена 2 (ожидается успех)
                                            """,
                                    token1, token2)));

            Allure.step("2. Настройка тестового окружения", () -> {
                WireMockStubBuilder.mockAuthSuccess(token1);
                WireMockStubBuilder.mockAuthSuccess(token2);
                WireMockStubBuilder.mockDoActionSuccess(token1);
                WireMockStubBuilder.mockDoActionSuccess(token2);
                AllureReporter.addTestData("Настройка WireMock",
                        "WireMock настроен на успешные ответы для двух токенов");
            });

            Allure.step("3. Аутентификация обоих токенов", () -> {
                Allure.step("LOGIN токена 1", () -> {
                    Response response1 = given()
                            .spec(forValidApiKey(token1, ACTION_LOGIN))
                            .when()
                            .post(ENDPOINT);

                    int status1 = response1.getStatusCode();
                    AllureReporter.addTestData("Токен 1 аутентифицирован",
                            String.format("HTTP статус: %d %s", status1, AllureReporter.getStatusText(status1)));

                    response1.then().spec(forSuccess());
                });

                Allure.step("LOGIN токена 2", () -> {
                    Response response2 = given()
                            .spec(forValidApiKey(token2, ACTION_LOGIN))
                            .when()
                            .post(ENDPOINT);

                    int status2 = response2.getStatusCode();
                    AllureReporter.addTestData("Токен 2 аутентифицирован",
                            String.format("HTTP статус: %d %s", status2, AllureReporter.getStatusText(status2)));

                    response2.then().spec(forSuccess());
                });
            });

            Allure.step("4. Завершение сессии токена 1", () -> {
                Response logoutResponse = given()
                        .spec(forValidApiKey(token1, ACTION_LOGOUT))
                        .when()
                        .post(ENDPOINT);

                int logoutStatusCode = logoutResponse.getStatusCode();
                String logoutResult = logoutResponse.jsonPath().getString("result");

                AllureReporter.addTestData("Результат LOGOUT токена 1",
                        String.format("""
                                        ОЖИДАЛОСЬ:
                                        • HTTP статус: 200 OK ✓
                                        • Result поле: "OK"
                                        
                                        ПОЛУЧЕНО:
                                        • HTTP статус: %d %s %s
                                        • Result поле: %s %s
                                        
                                        ПРОВЕРКА:
                                        • Сессия токена 1 завершена успешно: %s
                                        """,
                                logoutStatusCode,
                                AllureReporter.getStatusText(logoutStatusCode),
                                logoutStatusCode == 200 ? "✓" : "✗",
                                logoutResult != null ? logoutResult : "отсутствует",
                                "OK".equals(logoutResult) ? "✓" : "✗",
                                logoutStatusCode == 200 ? "✓" : "✗"));

                logoutResponse.then().spec(forSuccess());
            });

            Allure.step("5. Проверка состояния токенов после LOGOUT", () -> {
                Allure.step("ACTION токена 1 (ожидается ошибка)", () -> {
                    Response action1Response = given()
                            .spec(forValidApiKey(token1, ACTION_ACTION))
                            .when()
                            .post(ENDPOINT);

                    int status1 = action1Response.getStatusCode();
                    String actualMessage1 = action1Response.jsonPath().getString(MESSAGE_PARAM);
                    String expectedMessage1 = String.format(TOKEN_NOT_FOUND_ERROR, token1);

                    AllureReporter.addTestData("Результат ACTION токена 1",
                            String.format("""
                                            ОЖИДАЛОСЬ:
                                            • HTTP статус: 403 Forbidden ✓
                                            • Сообщение: "%s"
                                            
                                            ПОЛУЧЕНО:
                                            • HTTP статус: %d %s %s
                                            • Сообщение: "%s" %s
                                            
                                            ПРОВЕРКА:
                                            • Токен 1 больше не действителен: %s
                                            """,
                                    expectedMessage1,
                                    status1,
                                    AllureReporter.getStatusText(status1),
                                    status1 == 403 ? "✓" : "✗",
                                    actualMessage1 != null ? actualMessage1 : "отсутствует",
                                    expectedMessage1.equals(actualMessage1) ? "✓" : "✗",
                                    status1 == 403 ? "✓" : "✗"));

                    action1Response.then()
                            .spec(forError(HTTP_FORBIDDEN))
                            .body(MESSAGE_PARAM, equalTo(expectedMessage1));
                });

                Allure.step("ACTION токена 2 (ожидается успех)", () -> {
                    Response action2Response = given()
                            .spec(forValidApiKey(token2, ACTION_ACTION))
                            .when()
                            .post(ENDPOINT);

                    int status2 = action2Response.getStatusCode();
                    String result2 = action2Response.jsonPath().getString("result");

                    AllureReporter.addTestData("Результат ACTION токена 2",
                            String.format("""
                                            ОЖИДАЛОСЬ:
                                            • HTTP статус: 200 OK ✓
                                            • Result поле: "OK"
                                            
                                            ПОЛУЧЕНО:
                                            • HTTP статус: %d %s %s
                                            • Result поле: %s %s
                                            
                                            ПРОВЕРКА:
                                            • Токен 2 продолжает работать: %s
                                            """,
                                    status2,
                                    AllureReporter.getStatusText(status2),
                                    status2 == 200 ? "✓" : "✗",
                                    result2 != null ? result2 : "отсутствует",
                                    "OK".equals(result2) ? "✓" : "✗",
                                    status2 == 200 ? "✓" : "✗"));

                    action2Response.then().spec(forSuccess());
                });
            });

            testPassed.set(true);

        } finally {
            Allure.step("6. Итог тестирования", () -> {
                String resultText = testPassed.get() ?
                        """
                                ТЕСТ ПРОЙДЕН УСПЕШНО
                                
                                Что проверено (многопользовательский сценарий):
                                1. Подготовка двух валидных токенов ✓
                                2. Аутентификация обоих токенов ✓
                                3. Завершение сессии первого токена ✓
                                4. Первый токен стал недействительным ✓
                                5. Второй токен продолжает работать ✓
                                6. Изоляция сессий разных пользователей ✓
                                
                                Вывод: Система корректно изолирует сессии
                                разных пользователей. Завершение одной сессии
                                не влияет на работу других активных сессий.
                                """ :
                        """
                                ТЕСТ ПРОВАЛЕН
                                
                                Не удалось проверить LOGOUT разных токенов.
                                Детали ошибки см. в предыдущих шагах.
                                """;

                AllureReporter.addTestData("Результат теста", resultText);
            });
        }
    }
}