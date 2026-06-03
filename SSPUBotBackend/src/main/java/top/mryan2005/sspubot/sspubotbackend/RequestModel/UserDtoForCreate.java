package top.mryan2005.sspubot.sspubotbackend.RequestModel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserDtoForCreate {
    @NotBlank
    private String username;

    @NotBlank
    private String displayName;

    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;
}
