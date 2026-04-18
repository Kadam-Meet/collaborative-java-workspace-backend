package com.collab.workspace.repository;

import com.collab.workspace.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmailIgnoreCase(String email);

	Optional<User> findByPasswordResetTokenHash(String passwordResetTokenHash);

	boolean existsByEmailIgnoreCase(String email);
}
