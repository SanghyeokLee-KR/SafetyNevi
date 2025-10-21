package com.inha.pro.safetynevi.service.map;

import com.inha.pro.safetynevi.dao.map.FavoritePlaceRepository;
import com.inha.pro.safetynevi.dao.member.FamilyRepository;
import com.inha.pro.safetynevi.entity.map.FavoritePlace;
import com.inha.pro.safetynevi.entity.member.Family;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MapService {

    private final FavoritePlaceRepository favoritePlaceRepository;
    private final FamilyRepository familyRepository;

    // 집/회사는 사용자당 하나라 있으면 갱신, 없으면 새로 저장
    public void saveSpecialPlace(String userId, String type, String address, Double lat, Double lon) {
        FavoritePlace place = favoritePlaceRepository.findByUserIdAndPlaceType(userId, type)
                .orElse(null);

        if (place != null) {
            place.updateLocation(address, lat, lon);
        } else {
            String name = type.equals("HOME") ? "집" : "회사";
            favoritePlaceRepository.save(FavoritePlace.builder()
                    .userId(userId).placeType(type).name(name)
                    .address(address).latitude(lat).longitude(lon)
                    .build());
        }
    }

    public void addFavorite(String userId, String name, String address, Double lat, Double lon) {
        favoritePlaceRepository.save(FavoritePlace.builder()
                .userId(userId).placeType("FAVORITE").name(name)
                .address(address).latitude(lat).longitude(lon)
                .build());
    }

    // 집/회사/즐겨찾기 전부
    @Transactional(readOnly = true)
    public List<FavoritePlace> getMyAllPlaces(String userId) {
        return favoritePlaceRepository.findAllByUserId(userId);
    }

    public void deletePlace(String userId, Long placeId) {
        FavoritePlace place = favoritePlaceRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("장소를 찾을 수 없습니다."));
        if (!place.getUserId().equals(userId)) { // 남의 장소는 못 지움
            throw new SecurityException("삭제 권한이 없습니다.");
        }
        favoritePlaceRepository.delete(place);
    }

    @Transactional(readOnly = true)
    public List<Family> getFamilyList(String userId) {
        return familyRepository.findAllByUserId(userId);
    }

    public void addFamily(String userId, String name, String phone) {
        familyRepository.save(Family.builder()
                .userId(userId).name(name).phone(phone).build());
    }

    public void deleteFamily(String userId, Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new IllegalArgumentException("연락처를 찾을 수 없습니다."));
        if (!family.getUserId().equals(userId)) { // 남의 연락처는 못 지움
            throw new SecurityException("삭제 권한이 없습니다.");
        }
        familyRepository.delete(family);
    }

}