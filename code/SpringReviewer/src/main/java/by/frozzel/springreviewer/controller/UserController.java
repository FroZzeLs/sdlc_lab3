package by.frozzel.springreviewer.controller;

import by.frozzel.springreviewer.dto.UserCreateDto;
import by.frozzel.springreviewer.dto.UserDisplayDto;
import by.frozzel.springreviewer.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "Users", description = "API для управления пользователями")
public class UserController {
    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать нового пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Пользователь успешно создан",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserDisplayDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные в запросе (ошибка валидации)",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Пользователь с таким именем уже существует",
                    content = @Content)
    })
    public UserDisplayDto createUser(
            @RequestBody(description = "Данные для создания нового пользователя", required = true,
                    content = @Content(schema = @Schema(implementation = UserCreateDto.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody UserCreateDto dto) {
        return userService.createUser(dto);
    }

    @GetMapping
    @Operation(summary = "Получить список всех пользователей")
    @ApiResponse(responseCode = "200", description = "Список всех пользователей",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = UserDisplayDto.class))))
    public List<UserDisplayDto> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить пользователя по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь найден",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserDisplayDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректный ID (не положительное число)",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Пользователь с указанным ID не найден",
                    content = @Content)
    })
    public UserDisplayDto getUserById(
            @Parameter(description = "ID пользователя", required = true, example = "1")
            @PathVariable @Min(value = 1, message = "User ID must be positive") Integer id) {
        return userService.getUserById(id);
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "Получить пользователя по имени (логину)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь найден",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserDisplayDto.class))),
            @ApiResponse(responseCode = "400", description = "Имя пользователя не указано",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Пользователь с указанным именем не найден",
                    content = @Content)
    })
    public UserDisplayDto getUserByUsername(
            @Parameter(description = "Имя пользователя (логин)", required = true, example = "frozzel")
            @PathVariable @NotBlank(message = "Username cannot be blank") String username) {
        return userService.getUserByUsername(username);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить данные существующего пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь успешно обновлен",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserDisplayDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные в запросе или неверный ID",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Пользователь с указанным ID не найден",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Пользователь с новым именем уже существует",
                    content = @Content)
    })
    public UserDisplayDto updateUser(
            @Parameter(description = "ID обновляемого пользователя", required = true, example = "1")
            @PathVariable @Min(value = 1, message = "User ID must be positive") Integer id,
            @RequestBody(description = "Новые данные для пользователя", required = true,
                    content = @Content(schema = @Schema(implementation = UserCreateDto.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody UserCreateDto dto) {
        return userService.updateUser(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить пользователя по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Пользователь успешно удален"),
            @ApiResponse(responseCode = "400", description = "Некорректный ID (не положительное число)",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Пользователь с указанным ID не найден",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Невозможно удалить пользователя, так как он связан с отзывами",
                    content = @Content)
    })
    public void deleteUser(
            @Parameter(description = "ID пользователя для удаления", required = true, example = "1")
            @PathVariable @Min(value = 1, message = "User ID must be positive") Integer id) {
        userService.deleteUser(id);
    }
}