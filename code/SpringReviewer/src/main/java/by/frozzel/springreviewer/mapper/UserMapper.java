package by.frozzel.springreviewer.mapper;

import by.frozzel.springreviewer.dto.ReviewDisplayDto; // Добавить импорт
import by.frozzel.springreviewer.dto.UserCreateDto;
import by.frozzel.springreviewer.dto.UserDisplayDto;
import by.frozzel.springreviewer.model.User;
import java.util.Collections; // Добавить импорт
import java.util.List;
import java.util.Objects; // Добавить импорт
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {
    private final ReviewMapper reviewMapper;

    public User toEntity(UserCreateDto dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        return user;
    }

    public UserDisplayDto toDto(User user) {
        if (user == null) {
            return null;
        }

        List<ReviewDisplayDto> reviewDtos = (user.getReviews() != null)
                ? user.getReviews().stream()
                .filter(Objects::nonNull)
                .map(reviewMapper::toDto)
                .filter(Objects::nonNull)
                .toList()
                : Collections.emptyList();

        return new UserDisplayDto(
                user.getId(),
                user.getUsername(),
                reviewDtos
        );
    }
}