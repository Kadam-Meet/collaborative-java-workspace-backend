package com.collab.workspace.repository;

import com.collab.workspace.entity.User;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class UserRepository {

	private final Map<String, User> usersByEmail = new ConcurrentHashMap<>();
	private final AtomicLong idSequence = new AtomicLong(1);

	public Optional<User> findByEmail(String email) {
		if (email == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(usersByEmail.get(email.toLowerCase()));
	}

	public boolean existsByEmail(String email) {
		return findByEmail(email).isPresent();
	}

	public User save(User user) {
		if (user.getId() == null) {
			user.setId(idSequence.getAndIncrement());
		}
		usersByEmail.put(user.getEmail().toLowerCase(), user);
		return user;
	}
}
