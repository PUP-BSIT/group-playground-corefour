package com.recorever.recorever_backend.service;

import com.recorever.recorever_backend.model.Match;
import com.recorever.recorever_backend.model.Report;
import com.recorever.recorever_backend.repository.MatchRepository;
import com.recorever.recorever_backend.repository.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MatchService {

    private static final double MIN_KEYWORD_OVERLAP = 0.5; // 50% description similarity required for High Match
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "a", "an", "the", "in", "on", "at", "and", "or", "to", "for", "of", "with", "is", "was", "has", "i", "my", "me", "found", "lost"
    ));
    
    @Autowired
    private MatchRepository matchRepo;

    @Autowired
    private ReportRepository reportRepo;

    @Autowired
    private NotificationService notificationService;

    public void findAndCreateMatch(Report newReport) {
        List<Report> existingReports = reportRepo.getAllReports(); 

        String newType = newReport.getType();
        String matchType = newType.equals("lost") ? "found" : "lost";

        for (Report existingReport : existingReports) {
            if (existingReport.getReport_id() == newReport.getReport_id() || 
                !existingReport.getType().equals(matchType)) {
                continue;
            }

            boolean isNameMatch = checkNameSimilarity(newReport, existingReport);
            if (isNameMatch) {
                boolean isLocationMatch = checkLocationProximity(newReport, existingReport);
                boolean isDescriptionMatch = checkDescriptionSimilarity(newReport, existingReport);
                
                String confidenceLevel;
                String detailMessage;

                if (isLocationMatch && isDescriptionMatch) {
                    confidenceLevel = "High-Confidence Match";
                    detailMessage = "Name, Location, and Description are highly similar.";
                } else if (isLocationMatch || isDescriptionMatch) {
                    confidenceLevel = "Medium-Confidence Match";
                    detailMessage = "Name matched, and either Location or Description showed similarity.";
                } else {
                    confidenceLevel = "Low-Confidence Match (Location Conflict)";
                    detailMessage = "Name matched, but the reported location or item description is significantly different. Check carefully.";
                }
                
                int lostId = newType.equals("lost") ? newReport.getReport_id() : existingReport.getReport_id();
                int foundId = newType.equals("found") ? newReport.getReport_id() : existingReport.getReport_id();
                
                matchRepo.createMatch(lostId, foundId);

                String notificationMessage = String.format("%s found: Your %s has been linked to report #%d. Detail: %s", 
                                                        confidenceLevel, newReport.getItem_name(), lostId, detailMessage);

                notificationService.create(newReport.getUser_id(), newReport.getReport_id(), notificationMessage);
                notificationService.create(existingReport.getUser_id(), existingReport.getReport_id(), notificationMessage);
            }
        }
    }
    
    // checks if one item name contains the other.
    private boolean checkNameSimilarity(Report report1, Report report2) {
        String name1 = report1.getItem_name().toLowerCase();
        String name2 = report2.getItem_name().toLowerCase();
        return name1.contains(name2) || name2.contains(name1);
    }
    
    // checks if the location strings are exactly the same.
    private boolean checkLocationProximity(Report report1, Report report2) {
        String loc1 = report1.getLocation().trim().toLowerCase();
        String loc2 = report2.getLocation().trim().toLowerCase();
        return loc1.equals(loc2);
    }

    // checks if the descriptions have significant keyword overlap.
    private boolean checkDescriptionSimilarity(Report report1, Report report2) {
        String desc1 = report1.getDescription() != null ? report1.getDescription().toLowerCase() : "";
        String desc2 = report2.getDescription() != null ? report2.getDescription().toLowerCase() : "";
        
        if (desc1.isEmpty() || desc2.isEmpty()) return false;
        
        Set<String> set1 = tokenize(desc1);
        Set<String> set2 = tokenize(desc2);

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) return false;

        double similarity = (double) intersection.size() / union.size();
        return similarity >= MIN_KEYWORD_OVERLAP;
    }
    
    private Set<String> tokenize(String description) {
        String cleaned = description.replaceAll("[^a-z0-9\\s]", " "); // Remove punctuation
        String[] words = cleaned.split("\\s+"); // Split by whitespace
        
        Set<String> tokens = new HashSet<>();
        for (String word : words) {
            word = word.trim();
            if (!word.isEmpty() && word.length() > 2 && !STOP_WORDS.contains(word)) {
                tokens.add(word);
            }
        }
        return tokens;
    }

    public List<Match> listAllMatches() {
        return matchRepo.getAllMatches();
    }
    
    public Match getMatchById(int id) {
        return matchRepo.getMatchById(id);
    }
    
    public boolean updateMatchStatus(int id, String status) {
        return matchRepo.updateMatchStatus(id, status);
    }
}
