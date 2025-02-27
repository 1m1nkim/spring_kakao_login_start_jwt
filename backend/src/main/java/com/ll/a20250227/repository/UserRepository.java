// UserRepository.java
package com.ll.a20250227.repository;

import com.ll.a20250227.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}