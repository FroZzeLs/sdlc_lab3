package by.frozzel.springreviewer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TeacherCreateDto {

    @NotBlank(message = "Teacher surname cannot be blank")
    @Size(max = 50, message = "Surname must be no longer than 50 characters")
    private String surname;

    @NotBlank(message = "Teacher name cannot be blank")
    @Size(max = 50, message = "Name must be no longer than 50 characters")
    private String name;

    @Size(max = 50, message = "Patronym must be no longer than 50 characters")
    private String patronym;
}