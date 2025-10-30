package by.frozzel.springreviewer.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewDisplayDto {
    private Integer id;
    private Integer authorId;
    private String author;
    private TeacherDisplayDto teacher;
    private Integer subjectId;
    private String subjectName;
    private LocalDate date;
    private Integer grade;
    private String comment;
}