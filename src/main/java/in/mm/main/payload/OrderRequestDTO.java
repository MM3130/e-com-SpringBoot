package in.mm.main.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequestDTO {

    private Long addressId;
    private String paymentMethod;
    private String pgName; //pg -> payment gateway
    private String pgPaymentId;
    private String pgStatus;
    private String pgResponseMessage;
}
