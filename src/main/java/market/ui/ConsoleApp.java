package market.ui;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import market.controller.api.AuthController;
import market.controller.api.ProductController;
import market.controller.api.impl.console.ConsoleAuthController;
import market.controller.api.impl.console.ConsoleProductController;
import market.db.MigrationRunner;
import market.domain.*;
import market.exception.AuthorizationException;
import market.exception.EntityNotFoundException;
import market.exception.PersistenceException;
import market.exception.ValidationException;
import market.repo.jdbc.AuditRepositoryJdbc;
import market.repo.jdbc.ProductRepositoryJdbc;
import market.repo.jdbc.UserRepositoryJdbc;
import market.service.AuditService;
import market.service.MetricsService;
import market.service.MetricsServiceImpl;
import market.service.jdbc.AuditServiceJdbc;
import market.service.jdbc.ProductServiceJdbc;
import market.service.jdbc.UserServiceJdbc;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

public class ConsoleApp {

    private final AuthController auth;
    private final ProductController products;
    private final AuditService audit;
    private final MetricsService metrics;
    private final Scanner in = new Scanner(System.in);

    public ConsoleApp() throws IOException {

        // 1. Загружаем конфиг
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (in == null) {
                throw new RuntimeException("application.properties not found in classpath");
            }
            props.load(in);
        }

        // 2. Метрики
        this.metrics = new MetricsServiceImpl();

        // 3. DataSource
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(props.getProperty("db.url"));
        cfg.setUsername(props.getProperty("db.username"));
        cfg.setPassword(props.getProperty("db.password"));
        cfg.setMaximumPoolSize(5);

        DataSource ds = new HikariDataSource(cfg);

        // 4. Liquibase
        MigrationRunner.runMigrations(
                props.getProperty("db.url"),
                props.getProperty("db.username"),
                props.getProperty("db.password"),
                props.getProperty("liquibase.changelog"),
                props.getProperty("db.schema"),
                props.getProperty("db.liquibaseSchema")
        );

        System.out.println("Миграции БД применены успешно.\n");

        // 5. Репозитории
        var productRepo = new ProductRepositoryJdbc(ds);
        var userRepo    = new UserRepositoryJdbc(ds);
        var auditRepo   = new AuditRepositoryJdbc(ds);

        // 6. Сервисы
        int cacheSize = Integer.parseInt(props.getProperty("cache.size", "100"));
        var productService = new ProductServiceJdbc(productRepo, metrics, cacheSize);
        var authService    = new UserServiceJdbc(userRepo);
        this.audit         = new AuditServiceJdbc(auditRepo);

        // 7. Контроллеры
        this.auth = new ConsoleAuthController(authService);
        this.products = new ConsoleProductController(productService);
    }

    public static void main(String[] args) throws IOException {
        Logger.getLogger("liquibase").setLevel(Level.WARNING);
        Logger.getLogger("liquibase.util").setLevel(Level.WARNING);
        Logger.getLogger("liquibase.command").setLevel(Level.WARNING);
        Logger.getLogger("liquibase.lockservice").setLevel(Level.WARNING);

        new ConsoleApp().run();
    }

    private void run() {
        println("Каталог товаров");
        while (true) {
            if (auth.current().isEmpty()) {
                authMenu();
            } else {
                mainMenu();
            }
        }
    }

    private void authMenu() {
        println("1) Войти\n2) Регистрация\n0) Выход\n");
        switch (askInt("Выберите пункт: ")) {
            case 1 -> {
                String u = ask("Имя пользователя: ");
                String p = ask("Пароль: ");
                auth.login(u, p).ifPresentOrElse(user -> {
                    println("Добро пожаловать, %s (%s)".formatted(user.getUsername(), user.getRole()));
                    audit.append(new AuditEvent(user.getUsername(), AuditAction.LOGIN, ""));
                }, () -> println("Неверное имя пользователя или пароль."));
            }
            case 2 -> registerUser();
            case 0 -> {
                println("До свидания!");
                System.exit(0);
            }
            default -> println("Неизвестная команда.");
        }
    }

    private void mainMenu() {
        var user = this.auth.current().get();
        println("--- МЕНЮ ---\n" +
                "1) Список товаров (с пагинацией)\n" +
                "2) Добавить товар (только админ)\n" +
                "3) Изменить товар (только админ)\n" +
                "4) Удалить товар (только админ)\n" +
                "5) Поиск / фильтрация (с пагинацией)\n" +
                "6) Метрики\n" +
                "7) Выйти из аккаунта\n" +
                "8) Сохранить данные\n");
        int c = askInt("Выберите пункт: ");
        try {
            switch (c) {
                case 1 -> listWithPagination();
                case 2 -> { requireAdmin(user); create(); }
                case 3 -> { requireAdmin(user); update(); }
                case 4 -> { requireAdmin(user); delete(); }
                case 5 -> searchWithPagination();
                case 6 -> println(metrics.snapshot());
                case 7 -> {
                    audit.append(new AuditEvent(user.getUsername(), AuditAction.LOGOUT, ""));
                    this.auth.logout();
                }
                case 8 -> {
                    products.persist();
                    println("Данные сохранены.");
                }
                default -> println("Неизвестная команда.");
            }
        } catch (AuthorizationException e) {
            println("Доступ запрещён: " + e.getMessage());
        } catch (EntityNotFoundException e) {
            println("Не найдено: " + e.getMessage());
        } catch (ValidationException e) {
            println("Ошибка валидации: " + e.getMessage());
        } catch (PersistenceException e) {
            println("Ошибка сохранения данных: " + e.getMessage());
        } catch (IOException e) {
            println("Ошибка записи данных в файл: " + e.getMessage());
        }
    }

    private void registerUser() {
        println("Регистрация нового пользователя");
        String username;
        while (true) {
            username = ask("Введите имя пользователя: ").trim();
            if (username.isEmpty()) {
                println("Имя не может быть пустым.");
                continue;
            }
            if (auth.exists(username)) {
                println("Пользователь с таким именем уже существует. Попробуйте другое имя.");
                continue;
            }
            break;
        }

        String password;
        while (true) {
            password = ask("Введите пароль: ").trim();
            if (password.isEmpty()) {
                println("Пароль не может быть пустым.");
                continue;
            }
            break;
        }

        auth.register(username, password, Role.USER);
        println("Пользователь '%s' успешно зарегистрирован.".formatted(username));

        auth.login(username, password);
        audit.append(new AuditEvent(username, AuditAction.CREATE, "новый пользователь"));
    }

    private void listWithPagination() {
        var data = products.list(0, Integer.MAX_VALUE);
        paginateAndShow(data);
    }

    private void searchWithPagination() {
        String q = askDef("Название / описание содержит", "");
        String brand = askDef("Бренд содержит", "");
        String cat = askDef("Категория (или пусто)", "");
        String min = askDef("Мин. цена (или пусто)", "");
        String max = askDef("Макс. цена (или пусто)", "");
        String onlyActive = askDef("Только активные? (true/false/пусто)", "");
        Category category = cat.isBlank() ? null : Category.valueOf(cat.toUpperCase());
        Double minP = min.isBlank() ? null : Double.parseDouble(min);
        Double maxP = max.isBlank() ? null : Double.parseDouble(max);
        Boolean act = onlyActive.isBlank() ? null : Boolean.parseBoolean(onlyActive);
        var res = products.search(
                q.isBlank() ? null : q,
                brand.isBlank() ? null : brand,
                category,
                minP,
                maxP,
                act, 0,
                Integer.MAX_VALUE
        );
        paginateAndShow(res);
        audit.append(new AuditEvent(currentUser(), AuditAction.SEARCH,
                "q=%s brand=%s cat=%s min=%s max=%s active=%s size=%d"
                        .formatted(q, brand, category, minP, maxP, act, res.size())));
    }

    private void paginateAndShow(List<Product> list) {
        if (list.isEmpty()) {
            println("(список пуст)");
            return;
        }
        int size = askInt("Размер страницы: ");
        int page = 0;
        while (true) {
            var slice = products.paginate(list, page, size);
            if (slice.isEmpty()) {
                println("Больше страниц нет.");
                break;
            }
            println(("--- Страница %d ---").formatted(page + 1));
            slice.forEach(p -> println(p.toString()));
            String nav = askDef("[N] — далее, [P] — назад, [Q] — выход", "N")
                    .trim().toUpperCase();
            if (nav.equals("N")) page++;
            else if (nav.equals("P")) page = Math.max(0, page - 1);
            else break;
        }
    }

    private void create() {
        Product p = new Product();
        p.setName(ask("Название: "));
        p.setBrand(ask("Бренд: "));
        p.setCategory(Category.valueOf(
                ask("Категория (ELECTRONICS/FASHION/HOME/BEAUTY/FOOD/SPORTS/BOOKS/OTHER): ").toUpperCase()
        ));
        p.setPrice(Double.parseDouble(ask("Цена: ")));
        p.setDescription(ask("Описание: "));
        p.setActive(true);
        var saved = products.create(p);
        audit.append(new AuditEvent(currentUser(), AuditAction.CREATE, "id=" + saved.getId()));
        println("Создан товар: " + saved);
    }

    private void update() {
        long id = askLong("ID товара: ");

        var opt = products.get(id);
        if (opt.isEmpty()) {
            println("Товар не найден.");
            throw new EntityNotFoundException("Товар не найден: id=" + id);
        }

        Product p = opt.get();
        String name = askDef("Название", p.getName());
        String brand = askDef("Бренд", p.getBrand());
        String cat = askDef("Категория", p.getCategory().name());

        if (p.getPrice() < 0) throw new ValidationException("Цена не может быть отрицательной");
        String price = askDef("Цена", Double.toString(p.getPrice()));

        String desc = askDef("Описание", p.getDescription());
        String act = askDef("Активен (true/false)", Boolean.toString(p.isActive()));
        p.setName(name);
        p.setBrand(brand);
        p.setCategory(Category.valueOf(cat.toUpperCase()));
        p.setPrice(Double.parseDouble(price));
        p.setDescription(desc);
        p.setActive(Boolean.parseBoolean(act));
        products.update(p);
        audit.append(new AuditEvent(currentUser(), AuditAction.UPDATE, "id=" + p.getId()));
        println("Товар обновлён.");
    }

    private void delete() {
        long id = askLong("ID товара: ");
        boolean ok = products.delete(id);
        if (ok) {
            audit.append(new AuditEvent(currentUser(), AuditAction.DELETE, "id=" + id));
            println("Товар удалён.");
        } else println("Товар не найден.");
    }

    private void requireAdmin(User u) {
        if (u.getRole() != Role.ADMIN)
            throw new AuthorizationException("Только для администратора");
    }

    private String currentUser() {
        return auth.current().map(User::getUsername).orElse("-");
    }

    private static void println(String s) {
        System.out.println(s);
    }

    private String ask(String prompt) {
        System.out.print(prompt);
        return in.nextLine();
    }

    private String askDef(String label, String def) {
        System.out.print(label + " [" + def + "]: ");
        String s = in.nextLine();
        return s.isBlank() ? def : s;
    }

    private int askInt(String p) {
        while (true) {
            try {
                return Integer.parseInt(ask(p));
            } catch (NumberFormatException  e) {
                println("Введите число.");
            }
        }
    }

    private long askLong(String p) {
        while (true) {
            try {
                return Long.parseLong(ask(p));
            } catch (NumberFormatException  e) {
                println("Введите число.");
            }
        }
    }
}
