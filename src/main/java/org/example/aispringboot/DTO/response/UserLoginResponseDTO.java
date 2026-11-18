package org.example.aispringboot.DTO.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserLoginResponseDTO {

    private String token;
    private String roleType;
    private UserDetailResponseDTO userInfo;


    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public  static  class  UserDetailResponseDTO {
        private Long id;
        private String username;
        private String email;
        private String nickname;
        private String avatar;
        private String phone;
        private Integer gender;
        private String genderDisplayName;
        private LocalDate birthday;
        private String userTypeDisplayName;
        private Integer userType;
        private Integer status;
        private String statusDisplayName;
        private String displayName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;



    }
}
