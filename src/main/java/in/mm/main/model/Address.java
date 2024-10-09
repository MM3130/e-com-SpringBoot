package in.mm.main.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long addressId;

    @NotBlank
    @Size(min = 5,message = "street name must be al least 5 character")
    private String street;

    @NotBlank
    @Size(min = 5,message = "building name must be al least 5 character")
    private String buildingName;

    @NotBlank
    @Size(min = 4,message = "city name must be al least 4 character")
    private String city;

    @NotBlank
    @Size(min = 3,message = "state name must be al least 3 character")
    private String state;

    @NotBlank
    @Size(min = 2,message = "country name must be al least 2 character")
    private String country;

    @NotBlank
    @Size(min = 6,message = "pincode must be al least 4 character")
    private String pincode;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Address(String street, String buildingName, String city, String state, String country, String pincode) {
        this.street = street;
        this.buildingName = buildingName;
        this.city = city;
        this.state = state;
        this.country = country;
        this.pincode = pincode;
    }
}
