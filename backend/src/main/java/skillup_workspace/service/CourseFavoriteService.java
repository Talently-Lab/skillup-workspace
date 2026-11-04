package skillup_workspace.service;

import skillup_workspace.dto.response.CourseFavoriteResponseDTO;

public interface CourseFavoriteService {
    CourseFavoriteResponseDTO getFavoriteStatus(Long courseId);

    void addFavorite(Long courseId);

    void removeFavorite(Long courseId);
}
