package by.frozzel.springreviewer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubjectCreateDto {

    @NotBlank(message = "Subject name cannot be blank")
    @Size(min = 2, max = 100, message = "Subject name must be between 2 and 100 characters")
    private String name;
}