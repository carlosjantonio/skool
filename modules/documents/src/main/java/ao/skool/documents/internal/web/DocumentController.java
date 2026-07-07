package ao.skool.documents.internal.web;

import ao.skool.common.security.Roles;
import ao.skool.documents.api.DocumentType;
import ao.skool.documents.internal.service.DocumentService;
import ao.skool.documents.internal.web.dto.DocumentDtos.DocumentResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('" + Roles.ADMIN + "','" + Roles.DIRECTOR + "','" + Roles.SECRETARY + "','" + Roles.TEACHER + "','" + Roles.STUDENT + "','" + Roles.GUARDIAN + "')")
    public DocumentResponse upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") DocumentType type,
            @RequestParam("ownerType") String ownerType,
            @RequestParam("ownerId") UUID ownerId) {
        return service.upload(file, type, ownerType, ownerId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID id) {
        var download = service.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.metadata().contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + download.metadata().filename() + "\"")
                .contentLength(download.metadata().sizeBytes())
                .body(new InputStreamResource(download.stream()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('" + Roles.ADMIN + "','" + Roles.DIRECTOR + "','" + Roles.SECRETARY + "')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<DocumentResponse> listForOwner(@RequestParam String ownerType, @RequestParam UUID ownerId) {
        return service.listForOwner(ownerType, ownerId);
    }
}
