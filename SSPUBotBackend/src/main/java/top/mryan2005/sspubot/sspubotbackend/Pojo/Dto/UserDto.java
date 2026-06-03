package top.mryan2005.sspubot.sspubotbackend.Pojo.Dto;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserDto {
    @NotBlank
    private Long id;

    @NotBlank
    private String username;

    @NotBlank
    private String displayName;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    private String avatarUrl;

    /**
     * User role, possible values: "USER", "MODERATOR", "ADMIN"
     */
    @NotBlank
    private String role;

    /**
     * VIP level, default is 0 (no VIP)
     */
    @NotBlank
    @Size(min = 0, max = 2)
    private int VIPLevel;

    /**
     * 0 - inactive <br/>
     * 1 - active <br/>
     * 2 - banned <br/>
     * 3 - muted
     */
    @NotBlank
    @Size(min = 0, max = 2)
    private int Status;

    @NotBlank
    private String createdAt;

    @NotBlank
    private String updatedAt;

    @NotBlank
    private String email;
}
