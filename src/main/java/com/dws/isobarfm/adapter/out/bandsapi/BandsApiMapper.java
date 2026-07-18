package com.dws.isobarfm.adapter.out.bandsapi;

import com.dws.isobarfm.adapter.out.bandsapi.exception.BandsProviderInvalidResponseException;
import com.dws.isobarfm.domain.model.Band;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class BandsApiMapper {

    public Band toDomain(BandsApiResponse response) {
        try {
            UUID id = UUID.fromString(response.id());
            URI image = response.image() != null ? URI.create(response.image()) : null;
            List<UUID> albumIds = mapAlbumIds(response.albums());

            return new Band(id, response.name(), image, response.genre(),
                    response.biography(), response.numPlays(), albumIds);
        } catch (IllegalArgumentException e) {
            throw new BandsProviderInvalidResponseException(
                    "Provider returned a band with invalid data: " + e.getMessage(), e);
        }
    }

    private List<UUID> mapAlbumIds(List<String> albums) {
        if (albums == null) {
            return Collections.emptyList();
        }
        return albums.stream()
                .map(UUID::fromString)
                .toList();
    }
}
