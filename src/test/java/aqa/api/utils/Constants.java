package aqa.api.utils;

import aqa.api.exceptions.UtilityClassException;

/**
 * Централизованный класс констант для тестирования REST API.
 * <p>
 * Содержит все настройки, параметры и значения, используемые в автоматизированных
 * тестах API.
 */
public class Constants {

    public static final String SUT_URL = "http://localhost:8080";
    public static final int WIREMOCK_PORT = 8888;

    public static final String ENDPOINT = "/endpoint";
    public static final String MOCK_AUTH = "/auth";
    public static final String MOCK_DO_ACTION = "/doAction";

    public static final String API_KEY_HEADER_NAME = "X-Api-Key";
    public static final String VALID_API_KEY = "qazWSXedc";
    public static final String INVALID_API_KEY = "invalid_key";
    public static final String EMPTY_API_KEY = "";

    public static final String TOKEN_PARAM = "token";
    public static final String ACTION_PARAM = "action";
    public static final String MESSAGE_PARAM = "message";
    public static final String RESULT_PARAM = "result";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String ACCEPT = "Accept";
    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_URLENCODED = "application/x-www-form-urlencoded";

    public static final int HTTP_OK = 200;
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_FORBIDDEN = 403;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_CONFLICT = 409;
    public static final int HTTP_INTERNAL_ERROR = 500;

    public static final String FORBIDDEN_MESSAGE = "Forbidden";
    public static final String NOT_FOUND_MESSAGE = "Not Found";
    public static final String BAD_REQUEST_MESSAGE = "Bad Request";
    public static final String UNAUTHORIZED_MESSAGE = "Unauthorized";
    public static final String CONFLICT_MESSAGE = "Conflict";
    public static final String INTERNAL_MESSAGE = "Internal Server Error";
    public static final String ERROR_MESSAGE = " Error";
    public static final String UNKNOWN_MESSAGE = "Unknown";

    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_ACTION = "ACTION";
    public static final String ACTION_LOGOUT = "LOGOUT";

    public static final String RESULT_OK = "OK";
    public static final String RESULT_ERROR = "ERROR";

    public static final int TOKEN_LENGTH = 32;

    public static final String SMOKE = "smoke";
    public static final String REGRESSION = "regression";
    public static final String NEEDS_CLARIFICATION = "needs-clarification";

    private Constants() {
        throw new UtilityClassException(getClass());
    }
}
