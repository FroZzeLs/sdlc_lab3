package by.frozzel.springreviewer.controller;

import by.frozzel.springreviewer.dto.SubjectCreateDto;
import by.frozzel.springreviewer.dto.SubjectDisplayDto;
import by.frozzel.springreviewer.service.SubjectService;
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
@RequestMapping("/subjects")
@RequiredArgsConstructor
@Validated
@Tag(name = "Subjects", description = "API для управления учебными предметами")
public class SubjectController {
    private final SubjectService subjectService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать новый учебный предмет")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Предмет успешно создан",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SubjectDisplayDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные в запросе (ошибка валидации)",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Предмет с таким названием уже существует",
                    content = @Content)
    })
    public SubjectDisplayDto createSubject(
            @RequestBody(description = "Название нового предмета", required = true,
                    content = @Content(schema = @Schema(implementation = SubjectCreateDto.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody SubjectCreateDto dto) {
        return subjectService.createSubject(dto);
    }

    @GetMapping
    @Operation(summary = "Получить список всех учебных предметов")
    @ApiResponse(responseCode = "200", description = "Список всех предметов",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = SubjectDisplayDto.class))))
    public List<SubjectDisplayDto> getAllSubjects() {
        return subjectService.getAllSubjects();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить учебный предмет по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Предмет найден",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SubjectDisplayDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректный ID (не положительное число)",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Предмет с указанным ID не найден",
                    content = @Content)
    })
    public SubjectDisplayDto getSubjectById(
            @Parameter(description = "ID предмета", required = true, example = "5")
            @PathVariable @Min(value = 1, message = "Subject ID must be positive") Integer id) {
        return subjectService.getSubjectById(id);
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Получить учебный предмет по названию")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Предмет найден",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SubjectDisplayDto.class))),
            @ApiResponse(responseCode = "400", description = "Название предмета не указано",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Предмет с указанным названием не найден",
                    content = @Content)
    })
    public SubjectDisplayDto getSubjectByName(
            @Parameter(description = "Название предмета", required = true, example = "Физика")
            @PathVariable @NotBlank(message = "Subject name cannot be blank") String name) {
        return subjectService.getSubjectByName(name);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить существующий учебный предмет")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Предмет успешно обновлен",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SubjectDisplayDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные в запросе или неверный ID",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Предмет с указанным ID не найден",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Предмет с новым названием уже существует",
                    content = @Content)
    })
    public SubjectDisplayDto updateSubject(
            @Parameter(description = "ID обновляемого предмета", required = true, example = "5")
            @PathVariable @Min(value = 1, message = "Subject ID must be positive") Integer id,
            @RequestBody(description = "Новые данные для предмета", required = true,
                    content = @Content(schema = @Schema(implementation = SubjectCreateDto.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody SubjectCreateDto dto) {
        return subjectService.updateSubject(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить учебный предмет по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Предмет успешно удален"),
            @ApiResponse(responseCode = "400", description = "Некорректный ID (не положительное число)",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Предмет с указанным ID не найден",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Невозможно удалить предмет, так как он связан с преподавателями или отзывами",
                    content = @Content)
    })
    public void deleteSubject(
            @Parameter(description = "ID предмета для удаления", required = true, example = "5")
            @PathVariable @Min(value = 1, message = "Subject ID must be positive") Integer id) {
        subjectService.deleteSubject(id);
    }
}