package aqa.api.base;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;


import static aqa.api.utils.Constants.WIREMOCK_PORT;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Базовый класс для всех тестов API.
 */
public class BaseTest {
    protected static WireMockServer wireMockServer;

    /**
     * Настраивает тестовое окружение перед выполнением всех тестов в классе.
     * <p>
     * Выполняет следующие действия:
     * <ol>
     *   <li>Запускает WireMock сервер на порту {@code WIREMOCK_PORT}</li>
     *   <li>Настраивает WireMock клиент для работы с localhost</li>
     *   <li>Добавляет Allure фильтр в RestAssured для логирования HTTP-трафика</li>     *
     * </ol>
     */
    @BeforeAll
    static void setUpAll() {

        wireMockServer = new WireMockServer(wireMockConfig().port(WIREMOCK_PORT));
        wireMockServer.start();

        WireMock.configureFor("localhost", WIREMOCK_PORT);
    }

    /**
     * Подготавливает чистое тестовое окружение перед выполнением каждого теста.
     * Гарантирует изоляцию тестовых сценариев путем сброса всех WireMock заглушек.
     */
    @BeforeEach
    void setUp() {
        if (wireMockServer != null) {
            wireMockServer.resetAll();
        }
        WireMock.reset();
    }

    /**
     * Очищает тестовое окружение после выполнения всех тестов в классе.
     * <p>
     * Останавливает WireMock сервер и освобождает используемые ресурсы.
     * Если сервер не был запущен (например, при ошибке инициализации), метод завершается без ошибки.
     */
    @AfterAll
    static void tearDownAll() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }
}