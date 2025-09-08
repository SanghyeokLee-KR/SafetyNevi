package com.inha.pro.safetynevi.service.inquiry;

import com.inha.pro.safetynevi.dao.inquiry.InquiryListRepository;
import com.inha.pro.safetynevi.dao.member.MemberRepository;
import com.inha.pro.safetynevi.dto.inquiry.InquiryDTO;
import com.inha.pro.safetynevi.entity.inquiry.InquiryEntity;
import com.inha.pro.safetynevi.entity.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InquiryService {

    // Repository 이름이 InquiryListRepository라면 그걸로 유지하세요
    private final InquiryListRepository irepo;
    private final MemberRepository mrepo;

    // application.properties의 C:/safety_uploads/inquiry 값을 가져옴
    @Value("${file.upload.inquiry}")
    private String uploadDir;

    // 읽기 전용 트랜잭션 (성능 최적화)
    @Transactional(readOnly = true)
    public Page<InquiryDTO> getInquiryList(Pageable pageable) {
        Page<InquiryEntity> inquiryEntities = irepo.findAll(pageable);
        return inquiryEntities.map(InquiryDTO::toDto);
    }

    // [신규] 내가 쓴 문의 내역 조회
    @Transactional(readOnly = true)
    public List<InquiryDTO> getMyInquiries(String userId) {
        List<InquiryEntity> entities = irepo.findAllByWriterIdOrderByCreatedDateDesc(userId);
        return entities.stream()
                .map(InquiryDTO::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void writeInquiry(InquiryDTO dto, String userId) {

        Member member = mrepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("로그인 정보를 찾을 수 없습니다."));

        dto.setWriterId(member.getUserId());
        dto.setWriterName(member.getName());

        if (dto.getCategory() == null || dto.getCategory().isEmpty()) {
            dto.setCategory("기타");
        }

        MultipartFile file = dto.getFile();
        if (file != null && !file.isEmpty()) {
            try {
                // 경로 조작(../) 방지: 정규화 후 파일명만 추출
                String originalFilename = StringUtils.getFilename(
                        StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename()));
                String uuid = UUID.randomUUID().toString().substring(0, 8);
                String savedFileName = uuid + "_" + originalFilename;

                // 🌟 [수정 1] System.getProperty 제거! 절대 경로(uploadDir) 그대로 사용
                Path uploadPath = Paths.get(uploadDir);

                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                Path filePath = uploadPath.resolve(savedFileName);
                file.transferTo(filePath.toFile());

                // 🌟 [수정 2] DB URL 저장 시 '/upload/inquiry/' 경로 명시
                // (WebMvcConfig에서 /upload/** -> C:/safety_uploads/ 로 매핑했다고 가정)
                dto.setImageUrl("/upload/inquiry/" + savedFileName);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        InquiryEntity inquiry = InquiryEntity.toEntity(dto);
        irepo.save(inquiry);
    }

    // 해당 사용자가 관리자(ADMIN)인지 확인
    @Transactional(readOnly = true)
    public boolean isAdmin(String userId) {
        return userId != null && mrepo.findById(userId).map(Member::isAdmin).orElse(false);
    }

    @Transactional(readOnly = true)
    public InquiryDTO getInquiryDetail(Long id, String currentUserId) {
        InquiryEntity inquiry = irepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("글이 없습니다."));

        if (inquiry.getIsSecret() == 1 && !inquiry.getWriterId().equals(currentUserId)) {
            if (!isAdmin(currentUserId)) {
                throw new SecurityException("비밀글은 작성자만 확인할 수 있습니다.");
            }
        }
        return InquiryDTO.toDto(inquiry);
    }

    @Transactional
    public void deleteInquiry(Long id, String userId) {
        InquiryEntity inquiry = irepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        if (!inquiry.getWriterId().equals(userId)) {
            throw new SecurityException("삭제 권한이 없습니다.");
        }

        String imageUrl = inquiry.getImageUrl();
        if (StringUtils.hasText(imageUrl)) {
            try {
                // 🌟 [수정 3] URL 앞부분(/upload/inquiry/)을 잘라내야 실제 파일명만 남음
                // 예: /upload/inquiry/abc.jpg -> abc.jpg
                String fileName = imageUrl.substring("/upload/inquiry/".length());

                fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);

                // 🌟 [수정 4] 절대 경로(uploadDir) + 파일명 조합
                Path filePath = Paths.get(uploadDir, fileName);

                Files.deleteIfExists(filePath);
            } catch (Exception e) {
                System.err.println("파일 삭제 중 오류 발생: " + e.getMessage());
            }
        }
        irepo.delete(inquiry);
    }

    @Transactional
    public void modifyInquiry(Long id, InquiryDTO dto, String userId) {
        InquiryEntity inquiry = irepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        if (!inquiry.getWriterId().equals(userId)) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        String newImageUrl = null;
        MultipartFile file = dto.getFile();

        if (file != null && !file.isEmpty()) {
            // (1) 기존 파일 삭제
            if (StringUtils.hasText(inquiry.getImageUrl())) {
                try {
                    // 🌟 [수정 5] URL 자르기 로직 통일
                    String oldFileName = inquiry.getImageUrl().substring("/upload/inquiry/".length());
                    oldFileName = URLDecoder.decode(oldFileName, StandardCharsets.UTF_8);

                    Path oldFilePath = Paths.get(uploadDir, oldFileName);
                    Files.deleteIfExists(oldFilePath);
                } catch (Exception e) {
                    System.err.println("기존 파일 삭제 실패: " + e.getMessage());
                }
            }

            // (2) 새 파일 저장
            try {
                String uuid = UUID.randomUUID().toString().substring(0, 8);
                // 경로 조작(../) 방지: 정규화 후 파일명만 추출
                String originalFilename = StringUtils.getFilename(
                        StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename()));
                String savedFileName = uuid + "_" + originalFilename;

                // 🌟 [수정 6] 절대 경로 사용
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

                Path filePath = uploadPath.resolve(savedFileName);
                file.transferTo(filePath.toFile());

                // 🌟 [수정 7] URL 경로 명시
                newImageUrl = "/upload/inquiry/" + savedFileName;

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        inquiry.modifyInquiry(
                dto.getTitle(),
                dto.getContent(),
                dto.getCategory(),
                dto.getIsSecret(),
                newImageUrl
        );
    }

    // [관리자용]
    @Transactional(readOnly = true)
    public List<InquiryDTO> getUnansweredInquiries() {
        List<InquiryEntity> entities = irepo.findByStatusOrderByCreatedDateDesc(InquiryEntity.InquiryStatus.WAITING);
        return entities.stream().map(InquiryDTO::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<InquiryDTO> getRecentAnsweredInquiries() {
        List<InquiryEntity> entities = irepo.findTop5ByStatusOrderByAnswerDateDesc(InquiryEntity.InquiryStatus.COMPLETED);
        return entities.stream().map(InquiryDTO::toDto).toList();
    }

    @Transactional
    public void registerAnswer(Long id, String answerContent) {
        InquiryEntity inquiry = irepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
        inquiry.registerAnswer(answerContent);
    }
}