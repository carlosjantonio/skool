package ao.skool.grading.internal.web;

import ao.skool.common.security.Roles;
import ao.skool.grading.internal.service.BoletimService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/grades/boletim")
public class BoletimController {

    private final BoletimService service;

    public BoletimController(BoletimService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('" + Roles.TEACHER + "','" + Roles.DIRECTOR + "','" + Roles.SECRETARY + "','" + Roles.ADMIN + "','" + Roles.GUARDIAN + "','" + Roles.STUDENT + "')")
    public ResponseEntity<byte[]> download(@RequestParam UUID studentId, @RequestParam UUID academicYearId) {
        byte[] pdf = service.generate(studentId, academicYearId);
        String filename = "boletim-" + studentId + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentLength(pdf.length)
                .body(pdf);
    }
}
