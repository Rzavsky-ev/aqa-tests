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
import java.util.stream.IntStream;

import static aqa.api.utils.Constants.*;

import static aqa.api.utils.ErrorMessages.*;
import static aqa.api.utils.specs.RequestSpecs.*;
import static aqa.api.utils.specs.ResponseSpecs.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("Тестирование веб-сервиса")
@Feature("Функционал ACTION")
@DisplayName("Тесты для действия ACTION")
public class ActionTest extends BaseTest {

    @Test
    @Story("Успешные сценарии")
    @DisplayName("Выполнение ACTION после успешного LOGIN")
    @Description("""
            Проверяет успешное выполнение действия после аутентификации:
            - Генерируется валидный токен
            - Выполняется успешный LOGIN (токен сохраняется)
            - Выполняется ACTION с тем же токеном
            - Ожидается успешный ответ
            """)
    @Tag(SMOKE)
    void performActionAfterSuccessfulLogin() {
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
                                            2. ACTION с тем же токеном (ожидается успех)
                                            """,
                                    token,
                                    token.length())));

            Allure.step("2. Настройка тестового окружения", () -> {
                WireMockStubBuilder.mockAuthSuccess(token);
                WireMockStubBuilder.mockDoActionSuccess(token);
                AllureReporter.addTestData("Настройка WireMock",
                        "WireMock настроен на успешные ответы для /auth и /doAction");
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

            Allure.step("4. Выполнение запроса ACTION", () -> {
                Response actionResponse = given()
                        .spec(forValidApiKey(token, ACTION_ACTION))
                        .when()
                        .post(ENDPOINT);

                int actionStatusCode = actionResponse.getStatusCode();
                String actionResult = actionResponse.jsonPath().getString("result");

                AllureReporter.addTestData("Результат ACTION",
                        String.format("""
                                        ОЖИДАЛОСЬ (ACTION):
                                        • HTTP статус: 200 OK ✓
                                        • Result поле: "OK"
                                        
                                        ПОЛУЧЕНО:
                                        • HTTP статус: %d %s %s
                                        • Result поле: %s %s
                                        
                                        ПРОВЕРКИ:
                                        • Действие выполнено успешно: %s
                                        • Последовательность LOGIN → ACTION работает: %s
                                        """,
                                actionStatusCode,
                                AllureReporter.getStatusText(actionStatusCode),
                                actionStatusCode == 200 ? "✓" : "✗",
                                actionResult != null ? actionResult : "отсутствует",
                                "OK".equals(actionResult) ? "✓" : "✗",
                                actionStatusCode == 200 ? "✓" : "✗",
                                actionStatusCode == 200 ? "✓" : "✗"));

                actionResponse.then().spec(forSuccess());
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
                                3. Успешная аутентификация (LOGIN) ✓
                                4. Успешное выполнение действия (ACTION) ✓
                                5. Последовательность LOGIN → ACTION работает ✓
                                
                                Вывод: Система корректно позволяет выполнять действия
                                после успешной аутентификации пользователя.
                                """ :
                        """
                                ТЕСТ ПРОВАЛЕН
                                
                                Не удалось выполнить ACTION после LOGIN.
                                Детали ошибки см. в предыдущих шагах.
                                """;

                AllureReporter.addTestData("Результат теста", resultText);
            });
        }
    }

    @Test
    @Story("Ошибки авторизации")
    @DisplayName("ACTION без предварительного LOGIN")
    @Description("""
            Проверяет обработку ACTION без предварительной аутентификации:
            - Токен никогда не проходил LOGIN
            - Отправляется запрос ACTION
            - Ожидается ошибка: токен не найден
            """)
    @Tag(REGRESSION)
    void performActionWithoutLogin() {
        String token = TokenGenerator.generateValidToken();
        AtomicBoolean testPassed = new AtomicBoolean(false);

        try {
            Allure.step("1. Подготовка тестовых данных", () ->
                    AllureReporter.addTestData("Тестовый сценарий",
                            String.format("""
                                            Токен: %s (валидный)
                                            Длина: %d символов
                                            Сценарий: Прямой запрос ACTION без LOGIN
                                            Ожидается: ошибка 403 Forbidden
                                            """,
                                    token,
                                    token.length())));

            Allure.step("2. Выполнение запроса ACTION без LOGIN", () -> {
                Response response = given()
                        .spec(forValidApiKey(token, ACTION_ACTION))
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
                                        • Запрос отклонен из-за отсутствия аутентификации: %s
                                        • Сообщение соответствует ожидаемому: %s
                                        • Result поле равно ERROR: %s
                                        """,
                                expectedMessage,
                                statusCode,
                                AllureReporter.getStatusText(statusCode),
                                statusCode == 403 ? "✓" : "✗",
                                actualMessage != null ? actualMessage : "отсутствует",
                                expectedMessage.equals(actualMessage) ? "✓" : "✗",
                                statusCode == 403 ? "✓" : "✗",
                                expectedMessage.equals(actualMessage) ? "✓" : "✗",
                                "ERROR".equals(response.jsonPath().getString("result")) ? "✓" : "✗"));

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
                                2. Отправка ACTION без предварительного LOGIN ✓
                                3. Получение ошибки 403 Forbidden ✓
                                4. Корректное сообщение об ошибке ✓
                                
                                Вывод: Система корректно защищает от выполнения действий
                                без предварительной аутентификации пользователя.
                                """ :
                        """
                                ТЕСТ ПРОВАЛЕН
                                
                                Не удалось проверить защиту от ACTION без LOGIN.
                                Детали ошибки см. в предыдущих шагах.
                                """;

                AllureReporter.addTestData("Результат теста", resultText);
            });
        }
    }

    @Test
    @Story("Ошибки авторизации")
    @DisplayName("ACTION после LOGOUT")
    @Description("""
            Проверяет обработку ACTION после завершения сессии:
            - Выполняется успешный LOGIN
            - Выполняется LOGOUT (токен удаляется)
            - Отправляется запрос ACTION
            - Ожидается ошибка: токен не найден
            """)
    @Tag(REGRESSION)
    void performActionAfterLogout() {
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
                                            2. LOGOUT с токеном (ожидается успех)
                                            3. ACTION с тем же токеном (ожидается ошибка)
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
                                        • Сессия завершена успешно: %s
                                        """,
                                logoutStatusCode,
                                AllureReporter.getStatusText(logoutStatusCode),
                                logoutStatusCode == 200 ? "✓" : "✗",
                                logoutResult != null ? logoutResult : "отсутствует",
                                "OK".equals(logoutResult) ? "✓" : "✗",
                                logoutStatusCode == 200 ? "✓" : "✗"));

                logoutResponse.then().spec(forSuccess());
            });

            Allure.step("5. Попытка выполнения ACTION после LOGOUT", () -> {
                Response actionResponse = given()
                        .spec(forValidApiKey(token, ACTION_ACTION))
                        .when()
                        .post(ENDPOINT);

                int actionStatusCode = actionResponse.getStatusCode();
                String actualMessage = actionResponse.jsonPath().getString(MESSAGE_PARAM);
                String expectedMessage = String.format(TOKEN_NOT_FOUND_ERROR, token);

                AllureReporter.addTestData("Результат ACTION после LOGOUT",
                        String.format("""
                                        ОЖИДАЛОСЬ (ACTION после LOGOUT):
                                        • HTTP статус: 403 Forbidden ✓
                                        • Сообщение: "%s"
                                        
                                        ПОЛУЧЕНО:
                                        • HTTP статус: %d %s %s
                                        • Сообщение: "%s" %s
                                        
                                        ПРОВЕРКИ:
                                        • Действие отклонено после завершения сессии: %s
                                        • Сообщение соответствует ожидаемому: %s
                                        • Result поле равно ERROR: %s
                                        """,
                                expectedMessage,
                                actionStatusCode,
                                AllureReporter.getStatusText(actionStatusCode),
                                actionStatusCode == 403 ? "✓" : "✗",
                                actualMessage != null ? actualMessage : "отсутствует",
                                expectedMessage.equals(actualMessage) ? "✓" : "✗",
                                actionStatusCode == 403 ? "✓" : "✗",
                                expectedMessage.equals(actualMessage) ? "✓" : "✗",
                                "ERROR".equals(actionResponse.jsonPath().getString("result")) ? "✓" : "✗"));

                actionResponse.then()
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
                                3. Успешное завершение сессии (LOGOUT) ✓
                                4. Отказ в выполнении ACTION после LOGOUT ✓
                                5. Корректный код ошибки 403 ✓
                                6. Понятное сообщение об ошибке ✓
                                
                                Вывод: Система корректно завершает сессии пользователей
                                и предотвращает выполнение действий после выхода.
                                """ :
                        """
                                ТЕСТ ПРОВАЛЕН
                                
                                Не удалось проверить ACTION после LOGOUT.
                                Детали ошибки см. в предыдущих шагах.
                                """;

                AllureReporter.addTestData("Результат теста", resultText);
            });
        }
    }

    @ParameterizedTest(name = "ACTION при ошибке внешнего сервиса ({0})")
    @ValueSource(ints = {HTTP_FORBIDDEN, HTTP_NOT_FOUND, HTTP_INTERNAL_ERROR})
    @Story("Ошибки внешнего сервиса")
    @DisplayName("ACTION при ошибках внешнего сервиса")
    @Description("""
            Проверяет обработку различных ошибок от внешнего сервиса /doAction:
            - Токен успешно аутентифицирован
            - Внешний сервис /doAction возвращает ошибку
            - Проверяется ответ приложения на внешнюю ошибку
            """)
    @Tag(NEEDS_CLARIFICATION)
    @Tag(REGRESSION)
    void performActionWhenExternalServiceReturnsError(int statusCode) {
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
                                            Сценарий:
                                            1. LOGIN с токеном (ожидается успех)
                                            2. ACTION при ошибке внешнего сервиса /doAction
                                            """,
                                    statusCode, statusName, token)));

            Allure.step("2. Настройка тестового окружения", () -> {
                WireMockStubBuilder.mockAuthSuccess(token);
                WireMockStubBuilder.mockDoActionError(token, statusCode);
                AllureReporter.addTestData("Настройка WireMock",
                        String.format("WireMock настроен на ошибку %d для /doAction", statusCode));
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

            Allure.step("4. Выполнение ACTION при ошибке внешнего сервиса", () -> {
                Response actionResponse = given()
                        .spec(forValidApiKey(token, ACTION_ACTION))
                        .when()
                        .post(ENDPOINT);

                int actionStatusCode = actionResponse.getStatusCode();
                String actualResult = actionResponse.jsonPath().getString(RESULT_PARAM);
                String actualMessage = actionResponse.jsonPath().getString(MESSAGE_PARAM);

                boolean isAlways500 = actionStatusCode == 500;
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
                                actionStatusCode,
                                AllureReporter.getStatusText(actionStatusCode),
                                actionStatusCode >= 400 && actionStatusCode < 600 ? "✓" : "✗",
                                actualResult != null ? actualResult : "отсутствует",
                                RESULT_ERROR.equals(actualResult) ? "✓" : "✗",
                                actualMessage != null ? actualMessage : "отсутствует",
                                statusCode,
                                actionStatusCode,
                                statusComparison,
                                isAlways500 ? "Приложение всегда возвращает 500 независимо от кода ошибки внешнего сервиса"
                                        : "Поведение соответствует ожиданиям",
                                ContentType.JSON.toString().equals(actionResponse.getContentType()) ? "✓" : "✗",
                                RESULT_ERROR.equals(actualResult) ? "✓" : "✗",
                                actualMessage != null && !actualMessage.trim().isEmpty() ? "✓" : "✗"));

                actionResponse.then()
                        .contentType(ContentType.JSON)
                        .body(RESULT_PARAM, equalTo(RESULT_ERROR))
                        .body(MESSAGE_PARAM, not(blankOrNullString()));
            });

            testPassed.set(true);

        } finally {
            Allure.step("5. Итог тестирования с рекомендацией", () -> {
                String resultText = testPassed.get() ?
                        String.format("""
                                ТЕСТ ВЫПОЛНЕН (с замечанием)
                                
                                Что проверено (код ошибки %d):
                                1. Генерация валидного токена ✓
                                2. Настройка ошибки внешнего сервиса ✓
                                3. Успешная аутентификация (LOGIN) ✓
                                4. Попытка ACTION при сбое внешнего сервиса ✓
                                5. Обработка ошибки внешнего сервиса
                                
                                НАБЛЮДЕНИЕ:
                                Независимо от кода ошибки внешнего сервиса /doAction
                                приложение возвращает структурированный ответ.
                                
                                ВОЗМОЖНЫЕ ИНТЕРПРЕТАЦИИ:
                                • Ожидаемое поведение: упрощенная обработка внешних ошибок
                                • Требуется доработка: нужно различать типы ошибок
                                • Особенность реализации: все внешние ошибки маппятся в 500
                                
                                РЕКОМЕНДАЦИЯ:
                                Уточнить логику обработки разных типов ошибок
                                внешнего сервиса выполнения действий.
                                
                                ТЕКУЩИЙ ВЫВОД:
                                Приложение обрабатывает сбои во внешнем сервисе действий,
                                но требуется уточнение по деталям обработки.
                                """, statusCode) :
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
    @Story("Многократные операции")
    @DisplayName("Несколько ACTION подряд с одним токеном")
    @Description("""
            Проверяет возможность выполнения нескольких ACTION подряд:
            - Токен успешно аутентифицирован
            - Выполняется несколько запросов ACTION подряд
            - Все запросы должны быть успешными
            """)
    @Tag(REGRESSION)
    void performMultipleActionsWithSameToken() {
        String token = TokenGenerator.generateValidToken();
        AtomicBoolean testPassed = new AtomicBoolean(false);

        try {
            Allure.step("1. Подготовка тестовых данных", () ->
                    AllureReporter.addTestData("Тестовый сценарий",
                            String.format("""
                                            Токен: %s
                                            Длина: %d символов
                                            Сценарий:
                                            1. LOGIN с токеном
                                            2. ACTION #1 (ожидается успех)
                                            3. ACTION #2 (ожидается успех)
                                            4. ACTION #3 (ожидается успех)
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

                AllureReporter.addTestData("Результат LOGIN",
                        String.format("""
                                        ОЖИДАЛОСЬ (LOGIN):
                                        • HTTP статус: 200 OK ✓
                                        • Result поле: "OK"
                                        
                                        ПОЛУЧЕНО:
                                        • HTTP статус: %d %s %s
                                        • Result поле: %s %s
                                        
                                        ПРОВЕРКА:
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

            Allure.step("4. Выполнение 3 запросов ACTION подряд", () -> {
                boolean allActionsSuccessful = IntStream.range(0, 3)
                        .mapToObj(i -> {
                            String actionNumber = "ACTION #" + (i + 1);

                            return Allure.step(actionNumber, () -> {
                                Response actionResponse = given()
                                        .spec(forValidApiKey(token, ACTION_ACTION))
                                        .when()
                                        .post(ENDPOINT);

                                int actionStatusCode = actionResponse.getStatusCode();
                                String actionResult = actionResponse.jsonPath().getString("result");

                                AllureReporter.addTestData(actionNumber,
                                        String.format("""
                                                        ОЖИДАЛОСЬ (%s):
                                                        • HTTP статус: 200 OK ✓
                                                        • Result поле: "OK"
                                                        
                                                        ПОЛУЧЕНО:
                                                        • HTTP статус: %d %s %s
                                                        • Result поле: %s %s
                                                        """,
                                                actionNumber,
                                                actionStatusCode,
                                                AllureReporter.getStatusText(actionStatusCode),
                                                actionStatusCode == 200 ? "✓" : "✗",
                                                actionResult != null ? actionResult : "отсутствует",
                                                "OK".equals(actionResult) ? "✓" : "✗"));

                                actionResponse.then().spec(forSuccess());
                                return actionStatusCode == 200;
                            });
                        })
                        .allMatch(success -> success);

                AllureReporter.addTestData("Итог выполнения",
                        allActionsSuccessful ?
                                "✓ Все 3 действия ACTION выполнены успешно" :
                                "✗ Не все действия выполнены успешно");
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
                                3. Выполнение ACTION #1 ✓
                                4. Выполнение ACTION #2 ✓
                                5. Выполнение ACTION #3 ✓
                                6. Все действия выполнены успешно ✓
                                
                                Вывод: Система позволяет многократно выполнять действия
                                с одним аутентифицированным токеном.
                                Токен сохраняет свою валидность для повторных операций.
                                """ :
                        """
                                ТЕСТ ПРОВАЛЕН
                                
                                Не удалось выполнить несколько ACTION подряд.
                                Детали ошибки см. в предыдущих шагах.
                                """;

                AllureReporter.addTestData("Результат теста", resultText);
            });
        }
    }

    @Test
    @Story("Ошибки валидации")
    @DisplayName("ACTION без параметра action")
    @Description("""
            Проверяет обработку запроса ACTION без параметра action:
            - Токен успешно аутентифицирован
            - Отправляется запрос без параметра action
            - Ожидается ошибка валидации
            """)
    @Tag(REGRESSION)
    void performActionWithoutActionParameter() {
        String token = TokenGenerator.generateValidToken();
        AtomicBoolean testPassed = new AtomicBoolean(false);

        try {
            Allure.step("1. Подготовка тестовых данных", () ->
                    AllureReporter.addTestData("Тестовый сценарий",
                            String.format("""
                                            Токен: %s (валидный)
                                            Длина: %d символов
                                            Сценарий:
                                            1. LOGIN с токеном (ожидается успех)
                                            2. ACTION без параметра action (ожидается ошибка)
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

            Allure.step("4. Попытка выполнения ACTION без параметра", () -> {
                Response actionResponse = given()
                        .spec(forValidApiKey(token, ""))
                        .when()
                        .post(ENDPOINT);

                int actionStatusCode = actionResponse.getStatusCode();
                String actualMessage = actionResponse.jsonPath().getString(MESSAGE_PARAM);

                AllureReporter.addTestData("Анализ ответа",
                        String.format("""
                                        ОЖИДАЛОСЬ (ACTION без параметра):
                                        • HTTP статус: 400 Bad Request ✓
                                        • Сообщение: "%s"
                                        
                                        ПОЛУЧЕНО:
                                        • HTTP статус: %d %s %s
                                        • Сообщение: "%s" %s
                                        
                                        ПРОВЕРКИ:
                                        • Параметр action обязателен: %s
                                        • Сообщение об ошибке корректно: %s
                                        • Result поле равно ERROR: %s
                                        • Валидация входных параметров работает: %s
                                        """,
                                INVALID_ACTION_ERROR,
                                actionStatusCode,
                                AllureReporter.getStatusText(actionStatusCode),
                                actionStatusCode == 400 ? "✓" : "✗",
                                actualMessage != null ? actualMessage : "отсутствует",
                                INVALID_ACTION_ERROR.equals(actualMessage) ? "✓" : "✗",
                                actionStatusCode == 400 ? "✓" : "✗",
                                INVALID_ACTION_ERROR.equals(actualMessage) ? "✓" : "✗",
                                "ERROR".equals(actionResponse.jsonPath().getString("result")) ? "✓" : "✗",
                                actionStatusCode == 400 ? "✓" : "✗"));

                actionResponse.then()
                        .spec(forError(HTTP_BAD_REQUEST))
                        .body(MESSAGE_PARAM, equalTo(INVALID_ACTION_ERROR));
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
                                3. Формирование запроса ACTION без параметра ✓
                                4. Отправка запроса с неполными данными ✓
                                5. Получение ошибки валидации 400 ✓
                                6. Корректное сообщение об ошибке ✓
                                
                                Вывод: Система требует обязательный параметр action
                                в запросах на выполнение действий.
                                Отсутствие параметра корректно обрабатывается как ошибка.
                                """ :
                        """
                                ТЕСТ ПРОВАЛЕН
                                
                                Не удалось проверить ACTION без параметра.
                                Детали ошибки см. в предыдущих шагах.
                                """;

                AllureReporter.addTestData("Результат теста", resultText);
            });
        }
    }
}