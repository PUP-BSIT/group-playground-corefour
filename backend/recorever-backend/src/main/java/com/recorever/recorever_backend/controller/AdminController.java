package com.recorever.recorever_backend.controller;

import com.recorever.recorever_backend.model.Report;
import com.recorever.recorever_backend.service.ClaimService;
import com.recorever.recorever_backend.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')") // Global security: Only users with ROLE_ADMIN can access this controller
public class AdminController {

    @Autowired
    private ReportService reportService;
    
    @Autowired
    private ClaimService claimService;

    @GetMapping("/reports/pending")
    public ResponseEntity<List<Report>> getPendingReports() {
        return ResponseEntity.ok(reportService.listByStatus("pending")); 
    }

    @PutMapping("/report/{id}/approve")
    public ResponseEntity<?> approveFoundReport(@PathVariable int id) {
        // Sets status to 'approved' and records the date resolved
        boolean updated = reportService.update(id, "approved", java.time.LocalDate.now().toString());
        
        if (!updated) {
            return ResponseEntity.badRequest().body("Report not found or update failed.");
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Found report approved and ready for claim."));
    }

    @PutMapping("/claim/{id}/approve")
    public ResponseEntity<?> approveClaim(@PathVariable int id) {
        // Calls the business logic in ClaimService to update status, generate codes, and notify the user
        boolean updated = claimService.updateStatus(id, "approved");
        
        if (!updated) {
            return ResponseEntity.badRequest().body("Claim not found or approval failed.");
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Claim approved. Codes generated and user notified."));
    }

    @PutMapping("/claim/{id}/reject")
    public ResponseEntity<?> rejectClaim(@PathVariable int id) {
        // Calls the business logic in ClaimService to update status and notify the user
        boolean updated = claimService.updateStatus(id, "rejected");
        
        if (!updated) {
            return ResponseEntity.badRequest().body("Claim not found or rejection failed.");
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Claim rejected and user notified."));
    }
}