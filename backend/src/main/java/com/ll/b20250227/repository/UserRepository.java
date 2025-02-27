// UserRepository.java
package com.ll.b20250227.repository;

import com.ll.b20250227.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}