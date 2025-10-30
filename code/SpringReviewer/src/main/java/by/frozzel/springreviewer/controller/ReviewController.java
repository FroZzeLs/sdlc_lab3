package by.frozzel.springreviewer.controller;

import by.frozzel.springreviewer.dto.ReviewCreateDto;
import by.frozzel.springreviewer.dto.ReviewDisplayDto;
import by.frozzel.springreviewer.service.ReviewService;
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
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Validated
@Tag(name = "Reviews", description = "API для управления отзывами")
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать новый отзыв")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Отзыв успешно создан",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ReviewDisplayDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные в запросе (ошибки валидации)",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Пользователь, преподаватель или предмет не найдены",
                    content = @Content)
    })
    public ReviewDisplayDto createReview(
            @RequestBody(description = "Данные для создания нового отзыва", required = true,
                    content = @Content(schema = @Schema(implementation = ReviewCreateDto.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody ReviewCreateDto reviewCreateDto) {
        return reviewService.saveReview(reviewCreateDto);
    }

    @GetMapping
    @Operation(summary = "Получить все отзывы")
    @ApiResponse(responseCode = "200", description = "Список всех отзывов",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ReviewDisplayDto.class))))
    public List<ReviewDisplayDto> getAllReviews() {
        return reviewService.getAllReviews();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить отзыв по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Отзыв найден",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ReviewDisplayDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректный ID (не положительное число)",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Отзыв с указанным ID не найден",
                    content = @Content)
    })
    public ReviewDisplayDto getReviewById(
            @Parameter(description = "ID отзыва", required = true, example = "1")
            @PathVariable @Min(value = 1, message = "Review ID must be positive") Integer id) {
        return reviewService.getReviewById(id);
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "Получить все отзывы пользователя по его имени (логину)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список отзывов пользователя",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ReviewDisplayDto.class)))),
            @ApiResponse(responseCode = "400", description = "Имя пользователя не указано",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Пользователь с таким именем не найден (или у него нет отзывов)",
                    content = @Content)
    })
    public List<ReviewDisplayDto> getReviewsByUsername(
            @Parameter(description = "Имя пользователя (логин)", required = true, example = "frozzel")
            @PathVariable @NotBlank(message = "Username cannot be blank") String username) {
        return reviewService.getReviewsByUserUsername(username);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить отзыв по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Отзыв успешно удален"),
            @ApiResponse(responseCode = "400", description = "Некорректный ID (не положительное число)",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Отзыв с указанным ID не найден",
                    content = @Content)
    })
    public void deleteReview(
            @Parameter(description = "ID отзыва для удаления", required = true, example = "1")
            @PathVariable @Min(value = 1, message = "Review ID must be positive") Integer id) {
        reviewService.deleteReview(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить существующий отзыв")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Отзыв успешно обновлен",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ReviewDisplayDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные в запросе или неверный ID",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Отзыв, пользователь, преподаватель или предмет не найдены",
                    content = @Content)
    })
    public ReviewDisplayDto updateReview(
            @Parameter(description = "ID обновляемого отзыва", required = true, example = "1")
            @PathVariable @Min(value = 1, message = "Review ID must be positive") Integer id,
            @RequestBody(description = "Данные для обновления отзыва", required = true,
                    content = @Content(schema = @Schema(implementation = ReviewCreateDto.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody ReviewCreateDto reviewCreateDto) {
        return reviewService.updateReview(id, reviewCreateDto);
    }

    @GetMapping("/teacher/{teacherId}")
    @Operation(summary = "Получить все отзывы о преподавателе по его ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список отзывов о преподавателе",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ReviewDisplayDto.class)))),
            @ApiResponse(responseCode = "400", description = "Некорректный ID преподавателя",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Преподаватель с указанным ID не найден (или о нем нет отзывов)",
                    content = @Content)
    })
    public List<ReviewDisplayDto> getReviewsByTeacherId(
            @Parameter(description = "ID преподавателя", required = true, example = "10")
            @PathVariable @Min(value = 1, message = "Teacher ID must be positive") Integer teacherId) {
        return reviewService.getReviewsByTeacherId(teacherId);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Получить все отзывы пользователя по его ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список отзывов пользователя",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ReviewDisplayDto.class)))),
            @ApiResponse(responseCode = "400", description = "Некорректный ID пользователя",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Пользователь с указанным ID не найден (или у него нет отзывов)",
                    content = @Content)
    })
    public List<ReviewDisplayDto> getReviewsByUserId(
            @Parameter(description = "ID пользователя", required = true, example = "5")
            @PathVariable @Min(value = 1, message = "User ID must be positive") Integer userId) {
        return reviewService.getReviewsByUserId(userId);
    }

    @GetMapping("/stats/teacher-counts")
    @Operation(summary = "Получить статистику: количество отзывов по каждому преподавателю",
            description = "Возвращает список массивов Object[], где каждый подмассив содержит информацию о преподавателе и количество отзывов о нем.")
    @ApiResponse(responseCode = "200", description = "Статистика успешно получена",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(type = "array", implementation = Object[].class)))
    public List<Object[]> getReviewCountsPerTeacher() {
        return reviewService.getReviewCountsPerTeacher();
    }

    @GetMapping("/search")
    @Operation(summary = "Поиск отзывов по различным критериям")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список найденных отзывов",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ReviewDisplayDto.class)))),
            @ApiResponse(responseCode = "400", description = "Некорректное значение одного из параметров поиска",
                    content = @Content)
    })
    public List<ReviewDisplayDto> searchReviews(
            @Parameter(description = "Начальная дата поиска (YYYY-MM-DD)", required = false, example = "2024-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "Конечная дата поиска (YYYY-MM-DD)", required = false, example = "2024-03-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @Parameter(description = "Фамилия преподавателя (частичное совпадение)", required = false, example = "Иванов")
            @RequestParam(required = false) String teacherSurname,

            @Parameter(description = "Название предмета (частичное совпадение)", required = false, example = "Математика")
            @RequestParam(required = false) String subjectName,

            @Parameter(description = "Минимальная оценка (включительно)", required = false, example = "8")
            @RequestParam(required = false) @Min(value = 1, message = "Minimum grade must be at least 1") Integer minGrade) {
        return reviewService.searchReviews(startDate,
                endDate, teacherSurname, subjectName, minGrade);
    }
}