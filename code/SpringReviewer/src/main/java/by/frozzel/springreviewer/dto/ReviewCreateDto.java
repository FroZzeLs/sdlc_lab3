package by.frozzel.springreviewer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

@Data
public class ReviewCreateDto {

    @NotNull(message = "User ID cannot be null")
    private Integer userId;

    @NotNull(message = "Teacher ID cannot be null")
    private Integer teacherId;

    @NotNull(message = "Subject ID cannot be null")
    private Integer subjectId;

    @PastOrPresent(message = "Review date must be in the past or present")
    private LocalDate date;

    @NotNull(message = "Grade cannot be null")
    @Min(value = 1, message = "Grade must be at least 1")
    @Max(value = 10, message = "Grade must be at most 10")
    private Integer grade;

    @Size(max = 5000, message = "Comment must be no longer than 5000 characters")
    private String comment;
}