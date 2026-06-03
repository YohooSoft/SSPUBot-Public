package top.mryan2005.sspubot.sspubotbackend.RequestModel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserDtoForLogin {
    private String username;

    private String email;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;
}
