package com.inha.pro.safetynevi.service.notice;

import com.inha.pro.safetynevi.dao.notice.NoticeRepository;
import com.inha.pro.safetynevi.dto.notice.NoticeDTO;
import com.inha.pro.safetynevi.entity.notice.NoticeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository nrepo;

    // application.properties의 C:/safety_uploads/notice 값을 가져옴
    @Value("${file.upload.notice}")
    private String uploadDir;

    // [공지 작성]
    @Transactional
    public void saveNotice(NoticeDTO dto, String writerId) {

        // 1. 작성자 정보 세팅
        dto.setWriterId(writerId);
        dto.setWriterName("관리자");

        // 2. 파일 업로드 처리
        MultipartFile file = dto.getFile();
        if (file != null && !file.isEmpty()) {
            try {
                String originalFilename = file.getOriginalFilename();
                String uuid = UUID.randomUUID().toString().substring(0, 8);
                String savedFileName = uuid + "_" + originalFilename;

                // 🌟 [수정] System.getProperty 제거, 절대 경로(uploadDir) 사용
                Path uploadPath = Paths.get(uploadDir);

                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                Path filePath = uploadPath.resolve(savedFileName);
                file.transferTo(filePath.toFile());

                // 🌟 [수정] DB URL 저장 시 '/upload/notice/' 경로 명시
                dto.setAttachmentUrl("/upload/notice/" + savedFileName);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 3. Entity 변환 및 저장
        NoticeEntity notice = NoticeEntity.toEntity(dto);
        nrepo.save(notice);
    }

    // [공지사항 목록 조회]
    @Transactional(readOnly = true)
    public Page<NoticeDTO> getNoticeList(Pageable pageable, String keyword) {
        // 검색+정렬 통합 메서드 호출
        Page<NoticeEntity> noticeEntities = nrepo.findNoticeListWithCustomSort(keyword, pageable);
        return noticeEntities.map(NoticeDTO::toDto);
    }

    // [상세 조회] 조회수 증가 + DTO 반환
    @Transactional
    public NoticeDTO getNoticeDetail(Long id) {
        nrepo.updateViewCount(id);
        NoticeEntity notice = nrepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 공지가 없습니다. id=" + id));
        return NoticeDTO.toDto(notice);
    }

    // [공지사항 삭제]
    @Transactional
    public void deleteNotice(Long id) {
        NoticeEntity notice = nrepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 공지가 없습니다."));

        // 첨부파일 삭제 로직
        if (notice.getAttachmentUrl() != null) {
            try {
                // 🌟 [수정] URL 앞부분(/upload/notice/)을 잘라내야 실제 파일명만 남음
                String fileName = notice.getAttachmentUrl().substring("/upload/notice/".length());

                // 한글 파일명 깨짐 방지 디코딩
                fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);

                // 🌟 [수정] 절대 경로(uploadDir) + 파일명 조합
                Path filePath = Paths.get(uploadDir, fileName);

                Files.deleteIfExists(filePath);

            } catch (Exception e) {
                System.err.println("파일 삭제 실패: " + e.getMessage());
            }
        }

        nrepo.delete(notice);
    }
}