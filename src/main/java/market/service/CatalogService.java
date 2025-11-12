package market.service;

import market.domain.Category;
import market.domain.Product;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Сервис бизнес-логики каталога товаров.
 * <p>
 * Отвечает за операции создания, изменения, удаления и поиска товаров,
 * а также за фильтрацию, пагинацию и сохранение данных.
 * <p>
 * Данный интерфейс изолирует слой бизнес-логики от конкретных реализаций хранилища
 * (например, от файлового или in-memory репозитория).
 */
public interface CatalogService {
    /**
     * Создаёт новый товар в каталоге.
     *
     * @param p объект {@link Product}, содержащий данные нового товара
     * @return сохранённый товар с присвоенным уникальным идентификатором
     * @throws IllegalArgumentException если данные товара некорректны
     */
    Product create(Product p);

    /**
     * Возвращает товар по идентификатору.
     *
     * @param id уникальный идентификатор товара
     * @return {@link Optional} с найденным товаром или {@link Optional#empty()},
     *         если товар с таким идентификатором не найден
     */
    Optional<Product> get(long id);

    /**
     * Обновляет существующий товар.
     *
     * @param p объект {@link Product} с изменёнными данными
     * @return обновлённый товар
     * @throws IllegalArgumentException если товар с указанным идентификатором не существует
     */
    Product update(Product p);

    /**
     * Удаляет товар по идентификатору.
     *
     * @param id идентификатор удаляемого товара
     * @return {@code true}, если товар был успешно удалён; {@code false}, если не найден
     */
    boolean delete(long id);

    /**
     * Возвращает список всех товаров, доступных в каталоге.
     *
     * @return список всех товаров (может быть пустым, но не {@code null})
     */
    List<Product> listAll();

    /**
     * Выполняет поиск товаров по заданным фильтрам.
     * <p>
     * Все параметры необязательны. Если параметр равен {@code null} или пустой строке —
     * он не участвует в фильтрации.
     *
     * @param q          часть названия или описания товара
     * @param brand      название бренда для фильтрации
     * @param category   категория товара ({@link Category})
     * @param minPrice   минимальная цена
     * @param maxPrice   максимальная цена
     * @param onlyActive если {@code true} — возвращаются только активные товары
     * @return список товаров, удовлетворяющих заданным критериям
     */
    List<Product> search(String q, String brand, Category category,
                         Double minPrice, Double maxPrice, Boolean onlyActive);

    /**
     * Разбивает список товаров на страницы указанного размера и возвращает содержимое страницы.
     * <p>
     * Если номер страницы выходит за пределы диапазона, возвращается пустой список.
     *
     * @param list список всех товаров
     * @param page номер страницы (начиная с 0)
     * @param size количество товаров на странице
     * @return подсписок товаров, соответствующий указанной странице
     */
    List<Product> paginate(List<Product> list, int page, int size);

    /**
     * Сохраняет текущее состояние каталога в постоянное хранилище (например, CSV-файл).
     *
     * @throws IOException если произошла ошибка при записи данных
     */
    void persist() throws IOException;
}
