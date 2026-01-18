package aqa.api.utils;

import aqa.api.exceptions.UtilityClassException;

/**
 * Утилитный класс, содержащий стандартные сообщения об ошибках API.
 * Предоставляет централизованное хранилище для всех текстовых сообщений об ошибках
 */
public class ErrorMessages {

    public static final String INVALID_TOKEN_ERROR = "token: должно соответствовать \"^[0-9A-F]{32}$\"";
    public static final String INVALID_API_KEY_ERROR = "Missing or invalid API Key";
    public static final String TOKEN_ALREADY_EXISTS_ERROR = "Token '%s' already exists";
    public static final String TOKEN_NOT_FOUND_ERROR = "Token '%s' not found";
    public static final String INVALID_ACTION_ERROR = "action: invalid action 'null'. Allowed: LOGIN, LOGOUT, ACTION";

    private ErrorMessages() {
        throw new UtilityClassException(getClass());
    }
}
