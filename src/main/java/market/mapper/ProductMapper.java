package market.mapper;

import market.domain.Category;
import market.domain.Product;
import market.dto.product.ProductRequestDto;
import market.dto.product.ProductResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

/**
 * Маппер между сущностью Product и DTO.
 */
@Mapper(imports = Category.class)
public interface ProductMapper {

    ProductMapper INSTANCE = Mappers.getMapper(ProductMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(
            target = "category",
            expression = "java(dto.getCategory() == null ? null : " +
                    "Category.valueOf(dto.getCategory().toUpperCase()))"
    )
    @Mapping(
            target = "active",
            expression = "java(dto.getActive() == null ? true : dto.getActive())"
    )
    Product toEntity(ProductRequestDto dto);

    ProductResponseDto toDto(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(
            target = "category",
            expression = "java(dto.getCategory() == null ? target.getCategory() : " +
                    "Category.valueOf(dto.getCategory().toUpperCase()))"
    )
    @Mapping(
            target = "active",
            expression = "java(dto.getActive() == null ? target.isActive() : dto.getActive())"
    )
    void updateEntity(ProductRequestDto dto, @MappingTarget Product target);
}
