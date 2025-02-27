// User.java
package com.ll.a20250227.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {
    @Id
    private Long id; // 카카오에서 제공하는 ID를 그대로 사용

    private String nickname;
    private String email;
    private String profileImageUrl;

    // OAuth2 제공자 정보 (카카오, 구글 등)
    private String provider;
}