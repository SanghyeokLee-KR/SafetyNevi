package com.inha.pro.safetynevi.config;

import com.inha.pro.safetynevi.entity.FireStation;
import com.inha.pro.safetynevi.entity.Hospital;
import com.inha.pro.safetynevi.entity.Police;
import com.inha.pro.safetynevi.entity.Shelter;
import com.inha.pro.safetynevi.dao.map.FacilityRepository;
import com.inha.pro.safetynevi.dao.map.FireStationRepository;
import com.inha.pro.safetynevi.dao.map.HospitalRepository;
import com.inha.pro.safetynevi.dao.map.PoliceRepository;
import com.inha.pro.safetynevi.dao.map.ShelterRepository;

import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.InputStreamReader;
import java.util.List;

/**
 * 초기 데이터 적재 컴포넌트
 * - 애플리케이션 시작 시 CSV 파일을 읽어 DB에 초기 데이터를 저장함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataImporter implements CommandLineRunner {

    private final FacilityRepository facilityRepository;
    private final PoliceRepository policeRepository;
    private final FireStationRepository fireStationRepository;
    private final HospitalRepository hospitalRepository;
    private final ShelterRepository shelterRepository;

    @Override
    public void run(String... args) throws Exception {
        // 중복 적재 방지
        if (facilityRepository.count() > 0) {
            log.info("Facility data exists. Import skipped.");
            return;
        }
        log.info("Starting CSV data import process...");

        importPoliceData("data/police_data.csv", "UTF-8");
        importFireStationData("data/fire_station_data.csv", "EUC-KR");
        importHospitalData("data/hospital_data.csv", "EUC-KR");
        importShelterData("data/shelter_data.csv", "UTF-8");

        log.info("Data import completed successfully.");
    }

    // 경찰서 데이터 로드
    private void importPoliceData(String filePath, String encoding) {
        log.info("Loading Police Data ({})", filePath);
        try {
            List<String[]> records = readCsvFile(filePath, encoding);
            records.stream().skip(1).forEach(row -> {
                try {
                    String name = row[3];
                    if (name == null || name.isBlank()) return;

                    Police police = new Police();
                    police.setName(name);
                    police.setAddress(row[6]);
                    police.setLatitude(parseDoubleDefault(row[7]));
                    police.setLongitude(parseDoubleDefault(row[8]));
                    police.setPhoneNumber(row[5]);
                    police.setGubun(row[4]);
                    police.setSidoCheong(row[1]);

                    policeRepository.save(police);
                } catch (Exception e) {
                    log.warn("Skipping invalid police row: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Failed to import police data: ", e);
        }
    }

    // 소방서 데이터 로드
    private void importFireStationData(String filePath, String encoding) {
        log.info("Loading FireStation Data ({})", filePath);
        try {
            List<String[]> records = readCsvFile(filePath, encoding);
            records.stream().skip(1).forEach(row -> {
                try {
                    String name = row[1];
                    if (name == null || name.isBlank()) return;

                    FireStation station = new FireStation();
                    station.setName(name);
                    station.setAddress(row[2]);
                    station.setLatitude(parseDoubleDefault(row[5]));
                    station.setLongitude(parseDoubleDefault(row[6]));

                    // 상세 정보 매핑 (전화번호, 유형)
                    station.setAddressInPhoneColumn(row[4]);
                    station.setSubType(row[7]);

                    fireStationRepository.save(station);
                } catch (Exception e) {
                    log.warn("Skipping invalid fire station row: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Failed to import fire station data: ", e);
        }
    }

    // 병원 데이터 로드
    private void importHospitalData(String filePath, String encoding) {
        log.info("Loading Hospital Data ({})", filePath);
        try {
            List<String[]> records = readCsvFile(filePath, encoding);
            records.stream().skip(1).forEach(row -> {
                try {
                    String name = row[1];
                    if (name == null || name.isBlank()) return;

                    Hospital hospital = new Hospital();
                    hospital.setName(name);
                    hospital.setAddress(row[6]);
                    hospital.setLatitude(parseDoubleDefault(row[2]));
                    hospital.setLongitude(parseDoubleDefault(row[3]));
                    hospital.setPhoneNumber(row[5]);

                    hospital.setRoadAddress(row[7]);
                    hospital.setSubType(row[8]);
                    hospital.setOperatingStatus(row[4]);
                    hospital.setBedCount(parseIntegerDefault(row[10]));
                    hospital.setStaffCount(parseIntegerDefault(row[9]));

                    hospitalRepository.save(hospital);
                } catch (Exception e) {
                    log.warn("Skipping invalid hospital row: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Failed to import hospital data: ", e);
        }
    }

    // 대피소 데이터 로드
    private void importShelterData(String filePath, String encoding) {
        log.info("Loading Shelter Data ({})", filePath);
        try {
            List<String[]> records = readCsvFile(filePath, encoding);
            records.stream().skip(1).forEach(row -> {
                try {
                    String name = row[5];
                    if (name == null || name.isBlank()) return;

                    Shelter shelter = new Shelter();
                    shelter.setName(name);
                    shelter.setAddress(row[8]);
                    shelter.setLatitude(parseDoubleDefault(row[22]));
                    shelter.setLongitude(parseDoubleDefault(row[23]));

                    shelter.setOperatingStatus(row[4]);
                    shelter.setAreaM2(parseDoubleDefault(row[11]));
                    shelter.setMaxCapacity(parseIntegerDefault(row[12]));
                    shelter.setLocationType(row[10]);

                    shelterRepository.save(shelter);
                } catch (Exception e) {
                    log.warn("Skipping invalid shelter row: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Failed to import shelter data: ", e);
        }
    }

    // --- Utilities ---

    private List<String[]> readCsvFile(String filePath, String encoding) throws Exception {
        ClassPathResource resource = new ClassPathResource(filePath);
        try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), encoding);
             CSVReader csvReader = new CSVReader(reader)) {
            return csvReader.readAll();
        }
    }

    private Integer parseIntegerDefault(String value) {
        try {
            return (value == null || value.isBlank()) ? 0 : (int) Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Double parseDoubleDefault(String value) {
        try {
            return (value == null || value.isBlank()) ? 0.0 : Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}