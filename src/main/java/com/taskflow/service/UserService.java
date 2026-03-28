package com.taskflow.service;

import com.taskflow.exception.UserNotFoundException;
import com.taskflow.model.entity.User;
import com.taskflow.model.enums.UserRole;
import com.taskflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException(email));
    }

    @Transactional
    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already in use: " + user.getEmail());
        }
        if (user.getRole() == null) {
            user.setRole(UserRole.DEVELOPER);
        }
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, User updatedUser) {
        User existing = getUserById(id);
        existing.setName(updatedUser.getName());
        if (updatedUser.getRole() != null) {
            existing.setRole(updatedUser.getRole());
        }
        return userRepository.save(existing);
    }

    @Transactional
    public void deactivateUser(Long id) {
        User user = getUserById(id);
        user.setActive(false);
        userRepository.save(user);
    }
}
