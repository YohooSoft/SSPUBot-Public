package top.mryan2005.sspubot.sspubotbackend.Pojo;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    // AI 可访问的用户信息权限设置
    @Column(name = "allow_birth_date")
    private Boolean allowBirthDate = false;

    @Column(name = "allow_is_school_student")
    private Boolean allowIsSchoolStudent = false;

    @Column(name = "allow_enrollment_date")
    private Boolean allowEnrollmentDate = false;

    @Column(name = "allow_graduation_date")
    private Boolean allowGraduationDate = false;

    @Column(name = "allow_education_level")
    private Boolean allowEducationLevel = false;

    @Column(name = "allow_graduated_school")
    private Boolean allowGraduatedSchool = false;

    @Column(name = "allow_hobbies")
    private Boolean allowHobbies = false;

    @Column(name = "allow_from_location")
    private Boolean allowFromLocation = false;

    @Column(name = "allow_want_to_go")
    private Boolean allowWantToGo = false;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
