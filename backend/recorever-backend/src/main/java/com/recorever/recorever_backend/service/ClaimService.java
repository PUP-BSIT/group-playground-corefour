package com.recorever.recorever_backend.service;

import com.recorever.recorever_backend.model.Claim;
import com.recorever.recorever_backend.repository.ClaimRepository;
import com.recorever.recorever_backend.repository.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ClaimService {

    @Autowired
    private ClaimRepository repo;
    
    @Autowired
    private ReportRepository reportRepo; 
    
    @Autowired
    private NotificationService notificationService; 

    public Map<String, Object> create(int reportId, int userId, String proofDescription) {
        String itemName = reportRepo.getReportById(reportId).getItem_name();
        
        int id = repo.createClaim(reportId, userId, proofDescription, itemName);
        
        return Map.of(
            "claim_id", id,
            "report_id", reportId,
            "status", "pending",
            "message", "Claim submitted for administrative review."
        );
    }

    public List<Claim> listAllClaims() {
        return repo.getAllClaims();
    }
    
    public List<Claim> listClaimsByUserId(int userId) {
        return repo.getClaimsByUserId(userId);
    }

    public Claim getById(int claimId) {
        return repo.getClaimById(claimId);
    }

    public boolean updateStatus(int claimId, String status) {
        boolean updated = repo.updateClaimStatus(claimId, status);
        
        if (updated && status.equals("approved")) {
            String surrenderCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String claimCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            
            Claim claim = repo.getClaimById(claimId); 
            reportRepo.setClaimCodes(claim.getReport_id(), surrenderCode, claimCode);
            
            String msg = String.format("Claim #%d approved! Your claim code is: %s. Please use this to claim your item.", 
                                       claimId, claimCode);
            notificationService.create(claim.getUser_id(), claim.getReport_id(), msg);
        } else if (updated && status.equals("rejected")) {
             Claim claim = repo.getClaimById(claimId);
             String msg = String.format("Claim #%d for %s has been rejected by the administrator.", 
                                       claimId, claim.getItem_name());
             notificationService.create(claim.getUser_id(), claim.getReport_id(), msg);
        }
        
        return updated;
    }
}