package top.mryan2005.sspubot.sspubotbackend.Pojo;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

@Data
@Entity
@Table(name = "users")
// 1. 实现 UserDetails 接口
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String password;

    @Column(columnDefinition = "VARCHAR(MAX)")
    private String avatarUrl;

    /**
     * User role, possible values: "USER", "MODERATOR", "ADMIN"
     */
    @Column(columnDefinition = "VARCHAR(MAX)")
    private String role = "USER";

    /**
     * VIP level, default is 0 (no VIP)
     */
    @Column(columnDefinition = "INT DEFAULT 0")
    private int VIPLevel;

    /**
     * 0 - inactive <br/>
     * 1 - active <br/>
     * 2 - banned <br/>
     * 3 - muted
     */
    @Column(columnDefinition = "INT DEFAULT 1")
    private int Status = 1;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String Salt;

    // ============================================================
    // User Profile Fields
    // ============================================================
    
    /**
     * 出生年月 (Birth date)
     */
    @Column(name = "birth_date")
    private LocalDateTime birthDate;
    
    /**
     * 是否是本校学生 (Is school student)
     */
    @Column(name = "is_school_student")
    private Boolean isSchoolStudent;
    
    /**
     * 入学时间 (Enrollment date)
     */
    @Column(name = "enrollment_date")
    private LocalDateTime enrollmentDate;
    
    /**
     * 毕业时间 (Graduation date)
     */
    @Column(name = "graduation_date")
    private LocalDateTime graduationDate;
    
    /**
     * 目前的最高学历 (Highest education level)
     */
    @Column(name = "education_level", columnDefinition = "VARCHAR(MAX)")
    private String educationLevel;
    
    /**
     * 毕业学校 (Graduated school)
     */
    @Column(name = "graduated_school", columnDefinition = "VARCHAR(MAX)")
    private String graduatedSchool;
    
    /**
     * 爱好 (Hobbies - comma separated)
     */
    @Column(name = "hobbies", columnDefinition = "VARCHAR(MAX)")
    private String hobbies;
    
    /**
     * 来自哪里 (From location)
     */
    @Column(name = "from_location", columnDefinition = "VARCHAR(MAX)")
    private String fromLocation;
    
    /**
     * 想去哪里 (Want to go - comma separated)
     */
    @Column(name = "want_to_go", columnDefinition = "VARCHAR(MAX)")
    private String wantToGo;

    // ============================================================
    // 以下是 UserDetails 接口的实现方法
    // ============================================================

    /**
     * 获取用户权限/角色
     * 将你的 String role 转换为 Spring Security 需要的 Authority 对象
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 如果你的角色名不带 "ROLE_" 前缀，Spring Security 默认也能识别，
        // 但为了规范，通常建议数据库存 "ROLE_USER" 或者在这里拼接
        return Collections.singletonList(new SimpleGrantedAuthority(this.role));
    }

    /**
     * 获取密码 (Spring Security 自动调用此方法进行比对)
     */
    @Override
    public String getPassword() {
        return this.password;
    }

    /**
     * 获取用户名
     */
    @Override
    public String getUsername() {
        return this.username;
    }

    /**
     * 账户是否未过期
     */
    @Override
    public boolean isAccountNonExpired() {
        return true; // 暂时设为 true，除非你有过期的业务逻辑
    }

    /**
     * 账户是否未被锁定
     * 对应你的 Status: 2 - banned (被封禁)
     */
    @Override
    public boolean isAccountNonLocked() {
        return this.Status != 2; // 如果 Status 不是 2，则未锁定
    }

    /**
     * 凭证(密码)是否未过期
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 暂时设为 true
    }

    /**
     * 账户是否可用
     * 对应你的 Status: 0 - inactive (未激活)
     */
    @Override
    public boolean isEnabled() {
        // 只有 Status 为 1 (active) 时才算启用
        // 或者你可以根据需求允许 Status != 0
        return this.Status == 1;
    }
}