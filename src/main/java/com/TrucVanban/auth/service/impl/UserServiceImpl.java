package com.TrucVanban.auth.service.impl;

import com.TrucVanban.auth.dto.request.CreateUserRequest;
import com.TrucVanban.auth.dto.request.UpdateUserRequest;
import com.TrucVanban.auth.dto.response.UserResponse;
import com.TrucVanban.auth.entity.Role;
import com.TrucVanban.auth.entity.User;
import com.TrucVanban.auth.mapper.UserMapper;
import com.TrucVanban.auth.repository.RoleRepository;
import com.TrucVanban.auth.repository.UserRepository;
import com.TrucVanban.auth.service.UserService;
import com.TrucVanban.shared.exception.DuplicateResourceException;
import com.TrucVanban.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new DuplicateResourceException("Tên đăng nhập đã tồn tại");
        }

        if (request.getEmail() != null && userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Email đã tồn tại");
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        
        Set<Role> roles = new HashSet<>();
        if (request.getRoles() != null) {
            for (String roleCode : request.getRoles()) {
                roleRepository.findByCode(roleCode).ifPresent(roles::add);
            }
        }
        user.setRoles(roles);

        user = userRepository.save(user);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        userMapper.updateEntityFromRequest(request, user);

        //request.getRoles() = null -> giữ nguyên db
        if (request.getRoles() != null) {
            Set<Role> roles = new HashSet<>();
            for (String roleCode : request.getRoles()) {
                roleRepository.findByCode(roleCode).ifPresent(roles::add);
            }
            user.setRoles(roles);
        }

        user = userRepository.save(user);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        userRepository.delete(user);
    }
}
