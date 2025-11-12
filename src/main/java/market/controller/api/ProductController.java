package market.controller.api;

import market.domain.Category;
import market.domain.Product;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Контроллер управления товарами.
 * <p>
 * Определяет операции для создания, изменения, удаления и поиска товаров.
 * Является абстракцией над источником данных и может иметь различные реализации:
 * консольную, REST API и т.п.
 */
public interface ProductController {
    /**
     * Создаёт новый товар.
     *
     * @param p товар для сохранения
     * @return сохранённый товар с присвоенным идентификатором
     */
    Product create(Product p);

    /**
     * Обновляет существующий товар.
     *
     * @param p изменённые данные товара
     * @return обновлённый товар
     */
    Product update(Product p);

    /**
     * Удаляет товар по идентификатору.
     *
     * @param id идентификатор товара
     * @return {@code true}, если товар был успешно удалён
     */
    boolean delete(long id);

    /**
     * Возвращает товар по идентификатору.
     *
     * @param id идентификатор товара
     * @return найденный товар или {@code null}, если не найден
     */
    Optional<Product> get(long id);

    /**
     * Возвращает список товаров с постраничным выводом.
     *
     * @param page номер страницы (начиная с 0)
     * @param size количество элементов на странице
     * @return список товаров на указанной странице
     */
    List<Product> list(int page, int size);

    /**
     * Ищет товары по заданным критериям.
     *
     * @param q          часть названия или описания
     * @param brand      фильтр по бренду
     * @param category   фильтр по категории
     * @param min        минимальная цена
     * @param max        максимальная цена
     * @param onlyActive фильтр по активности
     * @param page       номер страницы (начиная с 0)
     * @param size       количество элементов на странице
     * @return список найденных товаров
     */
    List<Product> search(String q, String brand,
                         Category category,
                         Double min, Double max,
                         Boolean onlyActive,
                         int page, int size);

    /**
     * Сохраняет текущее состояние данных (например, в CSV).
     */
    void persist() throws IOException;

    /**
     * Возвращает часть списка товаров (страницу) на основе заданных параметров пагинации.
     * <p>
     * Метод используется для разбиения общего списка товаров на страницы фиксированного размера.
     * Например, если страница = 1 и размер = 10, то будут возвращены товары с 11-го по 20-й.
     *
     * @param list исходный список товаров
     * @param page номер страницы (начиная с 0)
     * @param size количество элементов на странице
     * @return подсписок товаров, соответствующий указанной странице;
     *         пустой список, если страница выходит за пределы общего списка
     */
    List<Product> paginate(List<Product> list, int page, int size);
}
