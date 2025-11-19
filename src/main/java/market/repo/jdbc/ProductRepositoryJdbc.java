package market.repo.jdbc;

import market.domain.Product;
import market.exception.PersistenceException;
import market.repo.ProductRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-реализация репозитория продуктов.
 * <p>
 * Хранит и извлекает сущности {@link Product} из PostgreSQL,
 */
public class ProductRepositoryJdbc implements ProductRepository {
    private final DataSource ds;

    public ProductRepositoryJdbc(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public Product save(Product p) {
        if (p.getId() == null || p.getId() == 0) {
            return insert(p);
        } else {
            return update(p);
        }
    }

    private Product insert(Product p) {
        String sql = """
                    INSERT INTO market.products (name, brand, category, price, description, active)
                    VALUES (?, ?, ?, ?, ?, ?)
                    RETURNING id
                """;
        try (Connection cn = ds.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getBrand());
            ps.setString(3, p.getCategory().name());
            ps.setBigDecimal(4, java.math.BigDecimal.valueOf(p.getPrice()));
            ps.setString(5, p.getDescription());
            ps.setBoolean(6, p.isActive());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) p.setId(rs.getLong(1));
            }
            return p;
        } catch (SQLException e) {
            throw wrap("Insert product failed: ", e);
        }
    }

    private Product update(Product p) {
        String sql = """
                    UPDATE market.products
                    SET name=?, brand=?, category=?, price=?, description=?, active=?
                    WHERE id=?
                """;
        try (Connection cn = ds.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getBrand());
            ps.setString(3, p.getCategory().name());
            ps.setBigDecimal(4, java.math.BigDecimal.valueOf(p.getPrice()));
            ps.setString(5, p.getDescription());
            ps.setBoolean(6, p.isActive());
            ps.setLong(7, p.getId());
            ps.executeUpdate();
            return p;
        } catch (SQLException e) {
            throw wrap("Update product failed: ", e);
        }
    }

    @Override
    public Optional<Product> findById(long id) {
        String sql = """
                    SELECT id,name,brand,category,price,description,active
                    FROM market.products WHERE id=?
                """;
        try (Connection cn = ds.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw wrap("Find product failed: ", e);
        }
    }

    @Override
    public boolean deleteById(long id) {
        String sql = "DELETE FROM market.products WHERE id=?";
        try (Connection cn = ds.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw wrap("Delete product failed: ", e);
        }
    }

    @Override
    public List<Product> findAll() {
        String sql = """
                    SELECT id,name,brand,category,price,description,active
                    FROM market.products
                    ORDER BY id
                """;

        try (Connection cn = ds.getConnection(); PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<Product> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        } catch (SQLException e) {
            throw wrap("FindAll products failed: ", e);
        }
    }

    @Override
    public long nextId() {
        throw new PersistenceException("Генерация ID недоступна: репозиторий использует PostgreSQL sequence через DEFAULT/RETURNING");
    }

    @Override
    public void load() {
        // not used: данные загружаются напрямую из PostgreSQL при запросах
    }

    @Override
    public void flush() {
        // not used: изменения сразу записываются в PostgreSQL
    }

    private Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getLong("id"));
        p.setName(rs.getString("name"));
        p.setBrand(rs.getString("brand"));
        p.setCategory(market.domain.Category.valueOf(rs.getString("category")));
        p.setPrice(rs.getBigDecimal("price").doubleValue());
        p.setDescription(rs.getString("description"));
        p.setActive(rs.getBoolean("active"));
        return p;
    }

    /**
     * Оборачивает SQLException в доменное PersistenceException
     * с более читаемым сообщением.
     */
    private PersistenceException wrap(String action, SQLException e) {
        return new PersistenceException(action + ". Причина: " + e.getMessage());
    }
}
