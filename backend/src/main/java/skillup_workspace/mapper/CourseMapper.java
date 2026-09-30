package skillup_workspace.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import skillup_workspace.dto.request.*;
import skillup_workspace.dto.response.*;
import skillup_workspace.entity.*;

@Mapper(componentModel = "spring")
public interface CourseMapper {


    @Mapping(source = "courseId", target = "id")
    CourseResponseDTO toResponseDTO(Course course);

    @Mapping(source = "courseId", target = "id")
    @Mapping(source = "active", target = "isActive")
    CourseDetailResponseDTO toDetailResponseDTO(Course course);

    @Mapping(source = "startTime", target = "startTime", dateFormat = "HH:mm")
    @Mapping(source = "endTime", target = "endTime", dateFormat = "HH:mm")
    ScheduleResponseDTO toScheduleResponseDTO(Schedule schedule);

    ModalityResponseDTO toModalityResponseDTO(Modality modality);

    InstructorResponseDTO toInstructorResponseDTO(Instructor instructor);

    ContentModuleResponseDTO toContentModuleResponseDTO(ContentModule module);


    @Mapping(target = "courseId", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    Course toEntity(CourseRequestDTO requestDTO);

    @Mapping(source = "startTime", target = "startTime", dateFormat = "HH:mm")
    @Mapping(source = "endTime", target = "endTime", dateFormat = "HH:mm")
    Schedule toScheduleEntity(ScheduleRequestDTO scheduleDTO);

    Modality toModalityEntity(ModalityRequestDTO modalityDTO);

    @Mapping(target = "instructorId", ignore = true)
    Instructor toInstructorEntity(InstructorRequestDTO instructorDTO);

    @Mapping(target = "moduleId", ignore = true)
    @Mapping(target = "course", ignore = true)
    ContentModule toContentModuleEntity(ContentModuleRequestDTO moduleDTO);

    @AfterMapping
    default void linkContentModules(@MappingTarget Course course) {
        if (course.getContentModules() != null) {
            course.getContentModules().forEach(module -> module.setCourse(course));
        }
    }

    @Mapping(target = "courseId", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromDto(CourseRequestDTO requestDTO, @MappingTarget Course course);
}