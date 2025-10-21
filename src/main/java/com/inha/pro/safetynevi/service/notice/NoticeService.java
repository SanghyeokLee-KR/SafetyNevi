package com.inha.pro.safetynevi.service.notice;

import com.inha.pro.safetynevi.dao.notice.NoticeRepository;
import com.inha.pro.safetynevi.dto.notice.NoticeDTO;
import com.inha.pro.safetynevi.entity.notice.NoticeEntity;
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
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository nrepo;

    @Value("${file.upload.notice}")
    private String uploadDir;

    @Transactional
    public void saveNotice(NoticeDTO dto, String writerId) {

        dto.setWriterId(writerId);
        dto.setWriterName("관리자");

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

                dto.setAttachmentUrl("/upload/notice/" + savedFileName);

            } catch (IOException e) {
                log.error("공지 첨부파일 저장 실패", e);
            }
        }

        NoticeEntity notice = NoticeEntity.toEntity(dto);
        nrepo.save(notice);
    }

    @Transactional(readOnly = true)
    public Page<NoticeDTO> getNoticeList(Pageable pageable, String keyword) {
        Page<NoticeEntity> noticeEntities = nrepo.findNoticeListWithCustomSort(keyword, pageable);
        return noticeEntities.map(NoticeDTO::toDto);
    }

    // 조회수 올리고 상세 반환
    @Transactional
    public NoticeDTO getNoticeDetail(Long id) {
        nrepo.updateViewCount(id);
        NoticeEntity notice = nrepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 공지가 없습니다. id=" + id));
        return NoticeDTO.toDto(notice);
    }

    @Transactional
    public void deleteNotice(Long id) {
        NoticeEntity notice = nrepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 공지가 없습니다."));

        // 첨부파일도 같이 지움
        if (notice.getAttachmentUrl() != null) {
            try {
                String fileName = notice.getAttachmentUrl().substring("/upload/notice/".length());

                // 한글 파일명 깨지지 않게 디코딩
                fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);

                Path filePath = Paths.get(uploadDir, fileName);

                Files.deleteIfExists(filePath);

            } catch (Exception e) {
                log.warn("공지 첨부파일 삭제 실패: {}", e.getMessage());
            }
        }

        nrepo.delete(notice);
    }
}