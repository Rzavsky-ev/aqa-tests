package aqa.api.utils.specs;

import aqa.api.exceptions.UtilityClassException;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.ResponseSpecification;

import static aqa.api.utils.Constants.*;
import static org.hamcrest.Matchers.*;

/**
 * Утилитный класс для создания спецификаций проверки ответов (ResponseSpecification) от API.
 * <p>
 * Содержит фабричные методы для построения стандартизированных спецификаций валидации
 * HTTP-ответов от тестируемого веб-сервиса. Все методы возвращают настроенные экземпляры
 * {@link ResponseSpecification}, которые могут быть использованы для проверки ответов
 * в RestAssured тестах.
 */
public class ResponseSpecs {

    /**
     * Создает спецификацию для проверки успешного ответа от API.
     *
     * @return {@link ResponseSpecification} для проверки успешного ответа
     */
    public static ResponseSpecification forSuccess() {
        return new ResponseSpecBuilder()
                .expectStatusCode(HTTP_OK)
                .expectContentType(ContentType.JSON)
                .expectBody(RESULT_PARAM, equalTo(RESULT_OK))
                .expectBody(MESSAGE_PARAM, nullValue())
                .build();
    }

    /**
     * Создает спецификацию для проверки ошибочного ответа с указанным HTTP-статусом.
     *
     * @param expectedStatusCode ожидаемый HTTP статус код ошибки
     * @return {@link ResponseSpecification} для проверки ошибочного ответа
     */
    public static ResponseSpecification forError(int expectedStatusCode) {
        return new ResponseSpecBuilder()
                .expectStatusCode(expectedStatusCode)
                .expectContentType(ContentType.JSON)
                .expectBody(RESULT_PARAM, equalTo(RESULT_ERROR))
                .expectBody(MESSAGE_PARAM, not(emptyOrNullString()))
                .build();
    }

    /**
     * Создает спецификацию для проверки ошибки валидации (400 Bad Request).
     *
     * @return {@link ResponseSpecification} для проверки ошибки валидации
     */
    public static ResponseSpecification forValidationError() {
        return forError(HTTP_BAD_REQUEST);
    }

    private ResponseSpecs() {
        throw new UtilityClassException(getClass());
    }
}