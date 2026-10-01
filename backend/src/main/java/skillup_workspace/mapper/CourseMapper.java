package skillup_workspace.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import skillup_workspace.dto.request.CourseRequestDTO;
import skillup_workspace.dto.response.CourseDetailResponseDTO;
import skillup_workspace.dto.response.CourseResponseDTO;
import skillup_workspace.entity.Course;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CourseMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    Course toEntity(CourseRequestDTO dto);

    CourseResponseDTO toResponseDTO(Course course);

    List<CourseResponseDTO> toResponseDTOList(List<Course> courses);

    CourseDetailResponseDTO toDetailResponseDTO(Course course);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    void updateEntityFromDto(CourseRequestDTO dto, @MappingTarget Course course);
}