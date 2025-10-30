package by.frozzel.springreviewer.service;

import by.frozzel.springreviewer.dto.UserCreateDto;
import by.frozzel.springreviewer.dto.UserDisplayDto;
import by.frozzel.springreviewer.exception.ConflictException;
import by.frozzel.springreviewer.exception.ResourceNotFoundException;
import by.frozzel.springreviewer.mapper.UserMapper;
import by.frozzel.springreviewer.model.User;
import by.frozzel.springreviewer.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    private static final String USER_RESOURCE = "User";
    private static final String ID_FIELD = "id";
    private static final String USERNAME_FIELD = "username";

    @Transactional
    public UserDisplayDto createUser(UserCreateDto dto) {
        if (userRepository.findByUsernameIgnoreCase(dto.getUsername()).isPresent()) {
            throw new ConflictException("Username already exists: " + dto.getUsername());
        }
        User user = userMapper.toEntity(dto);
        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    @Transactional(readOnly = true)
    public List<UserDisplayDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDisplayDto getUserById(Integer id) {
        return userRepository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(USER_RESOURCE, ID_FIELD, id));
    }

    @Transactional(readOnly = true)
    public UserDisplayDto getUserByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .map(userMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(USER_RESOURCE,
                        USERNAME_FIELD, username));
    }

    @Transactional
    public UserDisplayDto updateUser(Integer id, UserCreateDto dto) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_RESOURCE, ID_FIELD, id));

        if (!existingUser.getUsername().equals(dto.getUsername())
                && userRepository.findByUsernameIgnoreCase(dto.getUsername()).isPresent()) {
            throw new ConflictException("Username already exists: " + dto.getUsername());
        }
        existingUser.setUsername(dto.getUsername());
        User updatedUser = userRepository.save(existingUser);
        return userMapper.toDto(updatedUser);
    }

    @Transactional
    public void deleteUser(Integer id) {
        userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_RESOURCE, ID_FIELD, id));
        userRepository.deleteById(id);
    }
}