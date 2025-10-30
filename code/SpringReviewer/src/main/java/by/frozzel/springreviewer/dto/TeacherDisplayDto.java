package by.frozzel.springreviewer.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class TeacherDisplayDto {
    private Integer id;
    private String surname;
    private String name;
    private String patronym;

    @Builder.Default
    private List<String> subjects = new ArrayList<>();
}

