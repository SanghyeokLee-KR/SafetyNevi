package com.inha.pro.safetynevi.service.inquiry;

import com.inha.pro.safetynevi.dao.inquiry.InquiryListRepository;
import com.inha.pro.safetynevi.dao.member.MemberRepository;
import com.inha.pro.safetynevi.dto.inquiry.InquiryDTO;
import com.inha.pro.safetynevi.entity.inquiry.InquiryEntity;
import com.inha.pro.safetynevi.entity.member.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryListRepository irepo;
    private final MemberRepository mrepo;

    @Value("${file.upload.inquiry}")
    private String uploadDir;

    @Transactional(readOnly = true)
    public Page<InquiryDTO> getInquiryList(Pageable pageable) {
        Page<InquiryEntity> inquiryEntities = irepo.findAll(pageable);
        return inquiryEntities.map(InquiryDTO::toDto);
    }

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
                // 경로조작(../) 막으려고 정규화 후 파일명만 추출
                String originalFilename = StringUtils.getFilename(
                        StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename()));
                String uuid = UUID.randomUUID().toString().substring(0, 8);
                String savedFileName = uuid + "_" + originalFilename;

                Path uploadPath = Paths.get(uploadDir);

                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                Path filePath = uploadPath.resolve(savedFileName);
                file.transferTo(filePath.toFile());

                // 접두사는 WebMvcConfig 정적 리소스 매핑과 맞춘 것
                dto.setImageUrl("/upload/inquiry/" + savedFileName);

            } catch (IOException e) {
                log.error("문의 첨부파일 저장 실패", e);
                throw new RuntimeException("첨부파일 저장에 실패했습니다.", e); // 조용히 넘기면 깨진 URL 저장됨 → 롤백
            }
        }

        InquiryEntity inquiry = InquiryEntity.toEntity(dto);
        irepo.save(inquiry);
    }

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
                throw new IllegalStateException("비밀글은 작성자만 확인할 수 있습니다.");
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
                String fileName = imageUrl.substring("/upload/inquiry/".length());
                // 한글 파일명 깨지지 않게 디코딩
                fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);

                Path filePath = Paths.get(uploadDir, fileName);

                Files.deleteIfExists(filePath);
            } catch (Exception e) {
                log.warn("문의 첨부파일 삭제 실패: {}", e.getMessage());
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
            // 새 파일을 먼저 저장하고(실패하면 롤백), 성공한 뒤에 기존 첨부를 지운다.
            // 반대 순서면 새 저장이 실패했을 때 기존 첨부가 영구 유실됨.
            try {
                String uuid = UUID.randomUUID().toString().substring(0, 8);
                // 경로조작(../) 막으려고 정규화 후 파일명만 추출
                String originalFilename = StringUtils.getFilename(
                        StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename()));
                String savedFileName = uuid + "_" + originalFilename;

                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

                Path filePath = uploadPath.resolve(savedFileName);
                file.transferTo(filePath.toFile());

                newImageUrl = "/upload/inquiry/" + savedFileName;
            } catch (IOException e) {
                log.error("문의 첨부파일 저장 실패", e);
                throw new RuntimeException("첨부파일 저장에 실패했습니다.", e);
            }

            // 새 파일 저장 성공 후 기존 첨부 삭제
            if (StringUtils.hasText(inquiry.getImageUrl())) {
                try {
                    String oldFileName = inquiry.getImageUrl().substring("/upload/inquiry/".length());
                    oldFileName = URLDecoder.decode(oldFileName, StandardCharsets.UTF_8);
                    Files.deleteIfExists(Paths.get(uploadDir, oldFileName));
                } catch (Exception e) {
                    log.warn("문의 기존 첨부파일 삭제 실패: {}", e.getMessage());
                }
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

    // 관리자 화면용 - 미답변 문의
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