package in.mm.main.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {

    private Long productId;
    @NotBlank
    @Size(min = 3, message = "Product name must contain at least 3 character")
    private String productName;
    @NotBlank
    @Size(min = 6, message = "Product Description must contain at least 6 character")
    private String description;
    private String image;
    private Integer quantity;
    private Double price;
    private Double discount;
    private Double specialPrice;

}
