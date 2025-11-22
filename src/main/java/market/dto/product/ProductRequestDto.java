package market.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class ProductRequestDto {

    @NotBlank(message = "Название товара не должно быть пустым")
    private String name;

    @NotBlank(message = "Бренд не должен быть пустым")
    private String brand;

    @NotBlank(message = "Категория не должна быть пустой")
    private String category;

    @Positive(message = "Цена должна быть больше нуля")
    private double price;

    @Size(max = 1000, message = "Описание не должно быть длиннее 1000 символов")
    private String description;

    private Boolean active;

    public ProductRequestDto() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getActive() {
        return active;
    }
    public void setActive(Boolean active) {
        this.active = active;
    }
}
