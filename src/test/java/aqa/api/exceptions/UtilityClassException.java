package aqa.api.exceptions;

/**
 * Исключение, выбрасываемое при попытке создания экземпляра утилитного (статического) класса.
 */
public class UtilityClassException extends RuntimeException {

    /**
     * Создает исключение с сообщением, содержащим имя утилитного класса.
     *
     * @param utilityClass утилитный класс, для которого запрещено создание экземпляров.
     *                     Не может быть {@code null}.
     * @throws NullPointerException если {@code utilityClass} равен {@code null}
     */
    public UtilityClassException(Class<?> utilityClass) {
        super(String.format(
                "Класс '%s' является утилитным. Создание экземпляров запрещено.",
                utilityClass.getSimpleName()
        ));
    }
}