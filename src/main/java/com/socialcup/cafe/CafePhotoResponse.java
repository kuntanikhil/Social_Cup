package com.socialcup.cafe;

public record CafePhotoResponse(
        Long id,
        String storagePath,
        Integer displayOrder
) {
}
