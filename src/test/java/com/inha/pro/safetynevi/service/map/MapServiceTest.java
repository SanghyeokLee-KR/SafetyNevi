package com.inha.pro.safetynevi.service.map;

import com.inha.pro.safetynevi.dao.map.FavoritePlaceRepository;
import com.inha.pro.safetynevi.dao.member.FamilyRepository;
import com.inha.pro.safetynevi.entity.map.FavoritePlace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapServiceTest {

    @Mock FavoritePlaceRepository favoritePlaceRepository;
    @Mock FamilyRepository familyRepository;
    @InjectMocks MapService mapService;

    @Test
    void 남의_장소는_삭제할_수_없다() {
        FavoritePlace place = FavoritePlace.builder().userId("owner").build();
        when(favoritePlaceRepository.findById(1L)).thenReturn(Optional.of(place));

        assertThatThrownBy(() -> mapService.deletePlace("attacker", 1L))
                .isInstanceOf(SecurityException.class);
        verify(favoritePlaceRepository, never()).delete(any());
    }

    @Test
    void 본인_장소는_삭제된다() {
        FavoritePlace place = FavoritePlace.builder().userId("owner").build();
        when(favoritePlaceRepository.findById(1L)).thenReturn(Optional.of(place));

        assertThatCode(() -> mapService.deletePlace("owner", 1L)).doesNotThrowAnyException();
        verify(favoritePlaceRepository).delete(place);
    }
}
