package market.domain;

import java.util.Objects;

public class Product {
    private Long id;
    private String name;
    private String brand;
    private Category category;
    private double price;
    private String description;
    private boolean active = true;

    public Product() {
    }

    public Product(Long id, String name, String brand, Category category, double price, String description) {
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.price = price;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getBrand() {
        return brand;
    }

    public Category getCategory() {
        return category;
    }

    public double getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "#%d | %s (%s) | %s | %.2f | %s%s".formatted(id, name, brand, category, price, description, active ? "" : " [INACTIVE]");
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Product p && Objects.equals(p.id, id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
