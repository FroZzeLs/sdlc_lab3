package by.frozzel.springreviewer.controller;

import by.frozzel.springreviewer.dto.TeacherCreateDto;
import by.frozzel.springreviewer.dto.TeacherDisplayDto;
import by.frozzel.springreviewer.service.TeacherService;
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
import jakarta.validation.constraints.Size;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teachers")
@RequiredArgsConstructor
@Validated
@Tag(name = "Teachers", description = "API для управления преподавателями")
public class TeacherController {
    private final TeacherService teacherService;

    @GetMapping
    @Operation(summary = "Получить список всех преподавателей")
    @ApiResponse(responseCode = "200", description = "Список всех преподавателей",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = TeacherDisplayDto.class))))
    public List<TeacherDisplayDto> getAllTeachers() {
        return teacherService.getAllTeachers();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить преподавателя по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Преподаватель найден",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TeacherDisplayDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректный ID (не положительное число)",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Преподаватель с указанным ID не найден",
                    content = @Content)
    })
    public TeacherDisplayDto getTeacherById(
            @Parameter(description = "ID преподавателя", required = true, example = "10")
            @PathVariable @Min(value = 1, message = "Teacher ID must be positive") Integer id) {
        return teacherService.getTeacherById(id);
    }

    @GetMapping("/search/by-fullname")
    @Operation(summary = "Найти преподавателя по полному имени (Фамилия + Имя)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Преподаватель найден",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TeacherDisplayDto.class))),
            @ApiResponse(responseCode = "400", description = "Фамилия или имя не указаны или превышают допустимую длину",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Преподаватель с указанными фамилией и именем не найден",
                    content = @Content)
    })
    public TeacherDisplayDto getTeacherByFullName(
            @Parameter(description = "Фамилия преподавателя", required = true, example = "Иванов")
            @RequestParam @NotBlank(message = "Surname cannot be blank") @Size(max = 50) String surname,

            @Parameter(description = "Имя преподавателя", required = true, example = "Иван")
            @RequestParam @NotBlank(message = "Name cannot be blank") @Size(max = 50) String name) {
        return teacherService.getTeacherByFullName(surname, name);
    }

    @GetMapping("/search/by-subject")
    @Operation(summary = "Найти преподавателей по названию предмета")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список найденных преподавателей",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = TeacherDisplayDto.class)))),
            @ApiResponse(responseCode = "400", description = "Название предмета не указано",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Предмет с таким названием не найден (или нет преподавателей по этому предмету)",
                    content = @Content)
    })
    public List<TeacherDisplayDto> getTeachersBySubjectName(
            @Parameter(description = "Название предмета", required = true, example = "Математика")
            @RequestParam @NotBlank(message = "Subject name cannot be blank") String subjectName) {
        return teacherService.getTeachersBySubjectName(subjectName);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать нового преподавателя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Преподаватель успешно создан",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TeacherDisplayDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные в запросе (ошибка валидации)",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Преподаватель с таким ФИО уже существует",
                    content = @Content)
    })
    public TeacherDisplayDto createTeacher(
            @RequestBody(description = "Данные для создания нового преподавателя", required = true,
                    content = @Content(schema = @Schema(implementation = TeacherCreateDto.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody TeacherCreateDto teacherCreateDto) {
        return teacherService.createTeacher(teacherCreateDto);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать несколько преподавателей (bulk операция)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Преподаватели успешно созданы",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = TeacherDisplayDto.class)))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные в одном или нескольких объектах запроса (ошибка валидации)",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Один или несколько преподавателей с таким ФИО уже существуют",
                    content = @Content)
    })
    public List<TeacherDisplayDto> createTeachersBulk(
            @RequestBody(description = "Список данных для создания новых преподавателей", required = true,
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TeacherCreateDto.class))))
            @Valid @org.springframework.web.bind.annotation.RequestBody List<TeacherCreateDto> teacherCreateDtos) {
        return teacherService.createTeachersBulk(teacherCreateDtos);
    }


    @PutMapping("/{id}")
    @Operation(summary = "Обновить данные существующего преподавателя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Преподаватель успешно обновлен",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TeacherDisplayDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные в запросе или неверный ID",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Преподаватель с указанным ID не найден",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Преподаватель с новым ФИО уже существует",
                    content = @Content)
    })
    public TeacherDisplayDto updateTeacher(
            @Parameter(description = "ID обновляемого преподавателя", required = true, example = "10")
            @PathVariable @Min(value = 1, message = "Teacher ID must be positive") Integer id,
            @RequestBody(description = "Новые данные для преподавателя", required = true,
                    content = @Content(schema = @Schema(implementation = TeacherCreateDto.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody TeacherCreateDto teacherCreateDto) {
        return teacherService.updateTeacher(id, teacherCreateDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить преподавателя по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Преподаватель успешно удален"),
            @ApiResponse(responseCode = "400", description = "Некорректный ID (не положительное число)",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Преподаватель с указанным ID не найден",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Невозможно удалить преподавателя, так как он связан с отзывами",
                    content = @Content)
    })
    public void deleteTeacher(
            @Parameter(description = "ID преподавателя для удаления", required = true, example = "10")
            @PathVariable @Min(value = 1, message = "Teacher ID must be positive") Integer id) {
        teacherService.deleteTeacher(id);
    }

    @PostMapping("/{teacherId}/subjects/{subjectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Назначить предмет преподавателю")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Предмет успешно назначен"),
            @ApiResponse(responseCode = "400", description = "Некорректный ID преподавателя или предмета",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Преподаватель или предмет не найдены",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Данный предмет уже назначен этому преподавателю",
                    content = @Content)
    })
    public void assignSubjectToTeacher(
            @Parameter(description = "ID преподавателя", required = true, example = "10")
            @PathVariable @Min(value = 1, message = "Teacher ID must be positive") int teacherId,
            @Parameter(description = "ID предмета для назначения", required = true, example = "5")
            @PathVariable @Min(value = 1, message = "Subject ID must be positive") int subjectId) {
        teacherService.assignSubjectToTeacher(teacherId, subjectId);
    }

    @DeleteMapping("/{teacherId}/subjects/{subjectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Снять предмет с преподавателя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Предмет успешно снят с преподавателя"),
            @ApiResponse(responseCode = "400", description = "Некорректный ID преподавателя или предмета",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Преподаватель, предмет или их связь не найдены",
                    content = @Content)
    })
    public void removeSubjectFromTeacher(
            @Parameter(description = "ID преподавателя", required = true, example = "10")
            @PathVariable @Min(value = 1, message = "Teacher ID must be positive") int teacherId,
            @Parameter(description = "ID предмета для снятия", required = true, example = "5")
            @PathVariable @Min(value = 1, message = "Subject ID must be positive") int subjectId) {
        teacherService.removeSubjectFromTeacher(teacherId, subjectId);
    }
}