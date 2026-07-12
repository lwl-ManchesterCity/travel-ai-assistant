package com.lwl.travelassistant.service;

import com.lwl.travelassistant.model.TripPlan;
import com.lwl.travelassistant.model.TripPlanRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class RequestOptimizationStrategyService {

    public TripPlanRequest copyRequest(TripPlanRequest request) {
        return new TripPlanRequest(
                request.getCity(),
                request.getStartDate(),
                request.getEndDate(),
                request.getBudget(),
                request.getPreferences() == null ? new ArrayList<>() : new ArrayList<>(request.getPreferences()),
                request.getTransportation(),
                request.getAccommodation(),
                sanitizePersistedExtraRequirements(request.getExtraRequirements())
        );
    }

    public void applyAction(TripPlanRequest updatedRequest,
                            TripPlan plan,
                            String action,
                            List<String> changeNotes) {
        switch (normalizeAction(action)) {
            case "REDUCE_BUDGET" -> applyReduceBudget(updatedRequest, plan, changeNotes);
            case "COMPRESS_ROUTE" -> applyCompressRoute(updatedRequest, changeNotes);
            case "REPLACE_OUTDOOR_WITH_INDOOR" -> applyIndoorReplacement(updatedRequest, changeNotes);
            case "UPGRADE_EXPERIENCE" -> applyUpgradeExperience(updatedRequest, changeNotes);
            default -> {
            }
        }
    }

    public void mergeExtraRequirement(TripPlanRequest request, String newRequirement) {
        if (newRequirement == null || newRequirement.isBlank()) {
            return;
        }
        String existing = request.getExtraRequirements();
        if (existing == null || existing.isBlank()) {
            request.setExtraRequirements(newRequirement);
            return;
        }
        if (!existing.contains(newRequirement)) {
            request.setExtraRequirements(existing + "；" + newRequirement);
        }
    }

    public String normalizeAction(String action) {
        return action == null ? "STOP" : action.trim().toUpperCase(Locale.ROOT);
    }

    public String downgradeAccommodation(String accommodation) {
        if (accommodation == null || accommodation.isBlank()) {
            return "经济型酒店";
        }
        if (accommodation.contains("高端") || accommodation.contains("豪华")) {
            return "舒适型酒店";
        }
        if (accommodation.contains("舒适")) {
            return "经济型酒店";
        }
        if (accommodation.contains("民宿") || accommodation.contains("客栈")) {
            return "经济型酒店";
        }
        return accommodation;
    }

    public String upgradeAccommodation(String accommodation) {
        if (accommodation == null || accommodation.isBlank()) {
            return "舒适型酒店";
        }
        if (accommodation.contains("经济") || accommodation.contains("便捷")) {
            return "舒适型酒店";
        }
        if (accommodation.contains("舒适")) {
            return "高端酒店";
        }
        return accommodation;
    }

    private void applyReduceBudget(TripPlanRequest updatedRequest, TripPlan plan, List<String> changeNotes) {
        String downgraded = downgradeAccommodation(updatedRequest.getAccommodation());
        if (!downgraded.equals(updatedRequest.getAccommodation())) {
            updatedRequest.setAccommodation(downgraded);
            changeNotes.add("住宿偏好下调为 " + downgraded);
        }
        if (!"公共交通".equals(updatedRequest.getTransportation())) {
            updatedRequest.setTransportation("公共交通");
            changeNotes.add("交通方式调整为公共交通");
        }
        mergeExtraRequirement(updatedRequest, "整体控制预算，优先保留高价值景点");
        mergeExtraRequirement(updatedRequest, "减少高门票和远距离景点");
        if (plan != null && plan.getBudget() != null && !plan.getBudget().isWithinBudget()) {
            changeNotes.add("已加入预算收紧约束");
        }
    }

    private void applyCompressRoute(TripPlanRequest updatedRequest, List<String> changeNotes) {
        mergeExtraRequirement(updatedRequest, "尽量压缩跨区通勤，优先安排同片区景点");
        mergeExtraRequirement(updatedRequest, "减少折返路线");
        changeNotes.add("已加入压缩路线约束");
    }

    private void applyIndoorReplacement(TripPlanRequest updatedRequest, List<String> changeNotes) {
        mergeExtraRequirement(updatedRequest, "如遇降雨优先安排室内备选景点");
        mergeExtraRequirement(updatedRequest, "优先博物馆、美术馆、商圈等低天气暴露景点");
        changeNotes.add("已加入雨天室内备选约束");
    }

    private void applyUpgradeExperience(TripPlanRequest updatedRequest, List<String> changeNotes) {
        String upgraded = upgradeAccommodation(updatedRequest.getAccommodation());
        if (!upgraded.equals(updatedRequest.getAccommodation())) {
            updatedRequest.setAccommodation(upgraded);
            changeNotes.add("住宿偏好上调为 " + upgraded);
        }
        mergeExtraRequirement(updatedRequest, "预算充足时优先提升体验质量和景点丰富度");
        changeNotes.add("已加入体验升级约束");
    }

    private String sanitizePersistedExtraRequirements(String extraRequirements) {
        List<String> cleanedRequirements = extractResidualRequirements(extraRequirements);
        return cleanedRequirements.isEmpty() ? null : String.join("；", cleanedRequirements);
    }

    private List<String> extractResidualRequirements(String prompt) {
        List<String> residualRequirements = new ArrayList<>();
        if (prompt == null || prompt.isBlank()) {
            return residualRequirements;
        }
        String[] clauses = prompt.split("[；;\\n]");
        for (String clause : clauses) {
            String trimmed = clause.trim();
            if (trimmed.isBlank() || isAdjustmentInstruction(trimmed)) {
                continue;
            }
            residualRequirements.add(trimmed);
        }
        return residualRequirements;
    }

    private boolean isAdjustmentInstruction(String text) {
        String normalized = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return false;
        }
        return normalized.contains("预算")
                || normalized.contains("交通")
                || normalized.contains("自驾")
                || normalized.contains("地铁")
                || normalized.contains("公交")
                || normalized.contains("打车")
                || normalized.contains("酒店")
                || normalized.contains("住宿")
                || normalized.contains("民宿")
                || normalized.contains("客栈")
                || normalized.contains("经济型")
                || normalized.contains("高端")
                || normalized.contains("舒适型")
                || normalized.contains("便捷")
                || normalized.contains("自然风景")
                || normalized.contains("人文历史")
                || normalized.contains("城市漫游")
                || normalized.contains("轻松节奏")
                || normalized.contains("美食")
                || normalized.contains("节奏")
                || normalized.contains("慢一点")
                || normalized.contains("多一点")
                || normalized.contains("少一点");
    }
}
