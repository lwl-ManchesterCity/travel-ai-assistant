package com.lwl.travelassistant.agent;

import com.lwl.travelassistant.config.TravelLoopProperties;
import com.lwl.travelassistant.model.AgentTrace;
import com.lwl.travelassistant.model.LoopCritique;
import com.lwl.travelassistant.model.LoopExecutionSummary;
import com.lwl.travelassistant.model.LoopIterationTrace;
import com.lwl.travelassistant.model.TripPlan;
import com.lwl.travelassistant.model.TripPlanReflection;
import com.lwl.travelassistant.model.TripPlanRequest;
import com.lwl.travelassistant.service.PlannerInputBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class LoopPlanningAgent {

    private final PlannerInputBuilder plannerInputBuilder;
    private final PlannerAgent plannerAgent;
    private final ReflectionAgent reflectionAgent;
    private final LlmCriticAgent llmCriticAgent;
    private final LoopDecisionExecutor loopDecisionExecutor;
    private final TravelLoopProperties travelLoopProperties;

    public LoopPlanningAgent(PlannerInputBuilder plannerInputBuilder,
                             PlannerAgent plannerAgent,
                             ReflectionAgent reflectionAgent,
                             LlmCriticAgent llmCriticAgent,
                             LoopDecisionExecutor loopDecisionExecutor,
                             TravelLoopProperties travelLoopProperties) {
        this.plannerInputBuilder = plannerInputBuilder;
        this.plannerAgent = plannerAgent;
        this.reflectionAgent = reflectionAgent;
        this.llmCriticAgent = llmCriticAgent;
        this.loopDecisionExecutor = loopDecisionExecutor;
        this.travelLoopProperties = travelLoopProperties;
    }

    public TripPlan plan(TripPlanRequest originalRequest) {
        if (!travelLoopProperties.isEnabled()) {
            TripPlan tripPlan = plannerAgent.buildPlan(plannerInputBuilder.build(originalRequest));
            appendLoopResult(tripPlan, List.of(), List.of(), 1, "loop-disabled");
            return tripPlan;
        }

        TripPlanRequest workingRequest = copyRequest(originalRequest);
        List<AgentTrace> loopTraces = new ArrayList<>();
        List<LoopIterationTrace> loopIterations = new ArrayList<>();
        Set<String> seenRequestSignatures = new HashSet<>();
        TripPlan bestPlan = null;
        double bestScore = Double.MAX_VALUE;
        double previousScore = Double.MAX_VALUE;
        int bestRound = 1;
        int noImprovementRounds = 0;

        for (int round = 1; round <= Math.max(1, travelLoopProperties.getMaxRounds()); round++) {
            TripPlan candidatePlan = plannerAgent.buildPlan(plannerInputBuilder.build(workingRequest));
            loopTraces.add(new AgentTrace(
                    "LoopPlanningAgent",
                    "第 " + round + " 轮生成初稿",
                    buildRequestSummary(workingRequest),
                    "总费用=" + candidatePlan.getBudget().getTotal() + "，剩余预算=" + candidatePlan.getBudget().getRemainingBudget(),
                    "success"
            ));

            TripPlanReflection reflection = reflectionAgent.reflectTripPlan(
                    candidatePlan,
                    new ReflectionAgent.TripPlanRequestView(workingRequest.getBudget())
            );
            double candidateScore = scorePlan(candidatePlan, reflection);
            loopTraces.add(new AgentTrace(
                    "ReflectionAgent",
                    "评估第 " + round + " 轮整份方案",
                    "总费用=" + candidatePlan.getBudget().getTotal() + "，总通勤=" + calculateTotalRouteMinutes(candidatePlan) + " 分钟",
                    "建议动作=" + reflection.getRecommendedAction() + "，原因=" + reflection.getReason() + "，评分=" + reflection.getOverallScore(),
                    reflection.isAcceptable() ? "accept" : "review"
            ));

            if (candidateScore < bestScore) {
                bestPlan = candidatePlan;
                bestScore = candidateScore;
                bestRound = round;
            }
            if (round == 1 || candidateScore < previousScore) {
                noImprovementRounds = 0;
            } else {
                noImprovementRounds++;
            }
            previousScore = candidateScore;

            if (reflection.isAcceptable()) {
                loopIterations.add(buildIterationTrace(round, workingRequest, candidatePlan, reflection, null, List.of(), round == bestRound));
                markSelectedRound(loopIterations, round);
                appendLoopResult(candidatePlan, loopTraces, loopIterations, round, "第 " + round + " 轮通过规则评估，直接停止");
                return candidatePlan;
            }

            LoopCritique critique = llmCriticAgent.critique(workingRequest, candidatePlan, reflection);
            loopTraces.add(new AgentTrace(
                    "LlmCriticAgent",
                    "评估第 " + round + " 轮方案",
                    "预算=" + candidatePlan.getBudget().getTotal() + "，天数=" + candidatePlan.getDays().size(),
                    "动作=" + critique.getAction() + "，原因=" + critique.getReason() + "，来源=" + critique.getSource(),
                    critique.isShouldReplan() ? "replan" : "accept"
            ));
            loopIterations.add(buildIterationTrace(round, workingRequest, candidatePlan, reflection, critique, List.of(), round == bestRound));

            if (!critique.isShouldReplan()) {
                markSelectedRound(loopIterations, round);
                appendLoopResult(candidatePlan, loopTraces, loopIterations, round, "第 " + round + " 轮达到可接受状态");
                return candidatePlan;
            }

            LoopDecisionExecutor.ExecutionResult executionResult = loopDecisionExecutor.apply(workingRequest, candidatePlan, critique);
            loopIterations.set(loopIterations.size() - 1, buildIterationTrace(
                    round,
                    workingRequest,
                    candidatePlan,
                    reflection,
                    critique,
                    executionResult.getChangeNotes(),
                    round == bestRound
            ));
            if (noImprovementRounds >= Math.max(1, travelLoopProperties.getMaxNoImprovementRounds())) {
                markSelectedRound(loopIterations, bestRound);
                appendLoopResult(
                        bestPlan == null ? candidatePlan : bestPlan,
                        loopTraces,
                        loopIterations,
                        bestRound,
                        "连续 " + noImprovementRounds + " 轮没有改善，提前停止并保留当前最优方案"
                );
                return bestPlan == null ? candidatePlan : bestPlan;
            }
            if (executionResult.getChangeNotes().isEmpty()) {
                markSelectedRound(loopIterations, bestRound);
                appendLoopResult(bestPlan == null ? candidatePlan : bestPlan, loopTraces, loopIterations, bestRound, "批评已给出，但未生成新的可执行调整");
                return bestPlan == null ? candidatePlan : bestPlan;
            }

            TripPlanRequest nextRequest = executionResult.getUpdatedRequest();
            String requestSignature = buildRequestSignature(nextRequest);
            loopTraces.add(new AgentTrace(
                    "LoopDecisionExecutor",
                    "生成第 " + (round + 1) + " 轮输入",
                    critique.getNotes() == null || critique.getNotes().isEmpty() ? critique.getReason() : String.join("；", critique.getNotes()),
                    String.join("；", executionResult.getChangeNotes()),
                    "success"
            ));
            if (!seenRequestSignatures.add(requestSignature)) {
                markSelectedRound(loopIterations, bestRound);
                appendLoopResult(bestPlan == null ? candidatePlan : bestPlan, loopTraces, loopIterations, bestRound, "检测到循环调整重复，保留当前最优方案");
                return bestPlan == null ? candidatePlan : bestPlan;
            }
            workingRequest = nextRequest;
        }

        markSelectedRound(loopIterations, bestRound);
        appendLoopResult(bestPlan, loopTraces, loopIterations, bestRound, "达到最大循环轮次，保留当前最优方案");
        return bestPlan;
    }

    private void appendLoopResult(TripPlan tripPlan,
                                  List<AgentTrace> loopTraces,
                                  List<LoopIterationTrace> loopIterations,
                                  int bestRound,
                                  String loopSummary) {
        List<String> planningNotes = tripPlan.getPlanningNotes() == null
                ? new ArrayList<>()
                : new ArrayList<>(tripPlan.getPlanningNotes());
        planningNotes.add("LoopPlanningAgent 已完成生成-评估-反思-修正闭环");
        planningNotes.add("LoopPlanningAgent 选定第 " + bestRound + " 轮作为当前最优方案");
        planningNotes.add(loopSummary);
        tripPlan.setPlanningNotes(planningNotes);

        List<AgentTrace> mergedTraces = tripPlan.getAgentTraces() == null
                ? new ArrayList<>()
                : new ArrayList<>(tripPlan.getAgentTraces());
        mergedTraces.addAll(0, loopTraces);
        tripPlan.setAgentTraces(mergedTraces);
        tripPlan.setLoopSummary(new LoopExecutionSummary(
                travelLoopProperties.isEnabled(),
                loopIterations.size(),
                bestRound,
                loopSummary
        ));
        tripPlan.setLoopIterations(loopIterations);
    }

    private LoopIterationTrace buildIterationTrace(int round,
                                                   TripPlanRequest request,
                                                   TripPlan candidatePlan,
                                                   TripPlanReflection reflection,
                                                   LoopCritique critique,
                                                   List<String> appliedChanges,
                                                   boolean selectedAsBest) {
        return new LoopIterationTrace(
                round,
                buildRequestSummary(request),
                candidatePlan.getBudget() == null ? 0 : candidatePlan.getBudget().getTotal(),
                candidatePlan.getBudget() == null ? 0 : candidatePlan.getBudget().getRemainingBudget(),
                calculateTotalRouteMinutes(candidatePlan),
                reflection == null ? 0 : reflection.getOverallScore(),
                reflection == null ? 0 : reflection.getBudgetSeverity(),
                reflection == null ? 0 : reflection.getRouteSeverity(),
                reflection == null ? 0 : reflection.getWeatherSeverity(),
                reflection == null ? 0 : reflection.getExperienceSeverity(),
                candidatePlan.getBudget() != null && candidatePlan.getBudget().isWithinBudget(),
                critique != null && critique.isShouldReplan(),
                reflection == null ? null : reflection.getRecommendedAction(),
                reflection == null ? null : reflection.getReason(),
                reflection == null ? null : reflection.getReflectionNotes(),
                critique == null ? null : critique.getSource(),
                critique == null ? null : critique.getAction(),
                critique == null ? null : critique.getReason(),
                critique == null ? null : critique.getNotes(),
                appliedChanges,
                selectedAsBest
        );
    }

    private void markSelectedRound(List<LoopIterationTrace> loopIterations, int selectedRound) {
        for (LoopIterationTrace loopIteration : loopIterations) {
            loopIteration.setSelectedAsBest(loopIteration.getRound() == selectedRound);
        }
    }

    private double scorePlan(TripPlan tripPlan, TripPlanReflection reflection) {
        if (reflection != null) {
            return reflection.getOverallScore();
        }
        int overflow = tripPlan.getBudget() == null || tripPlan.getBudget().isWithinBudget()
                ? 0
                : Math.abs(tripPlan.getBudget().getRemainingBudget());
        int totalDurationMinutes = calculateTotalRouteMinutes(tripPlan);
        return overflow * 10.0 + totalDurationMinutes;
    }

    private int calculateTotalRouteMinutes(TripPlan tripPlan) {
        int totalDurationMinutes = 0;
        if (tripPlan.getDays() != null) {
            for (var day : tripPlan.getDays()) {
                if (day.getRoutePlan() != null) {
                    totalDurationMinutes += day.getRoutePlan().getTotalDurationMinutes();
                }
            }
        }
        return totalDurationMinutes;
    }

    private String buildRequestSummary(TripPlanRequest request) {
        return "城市=" + request.getCity()
                + "，预算=" + request.getBudget()
                + "，住宿=" + request.getAccommodation()
                + "，交通=" + (request.getTransportation() == null ? "智能推荐" : request.getTransportation());
    }

    private String buildRequestSignature(TripPlanRequest request) {
        return request.getCity() + "|"
                + request.getBudget() + "|"
                + request.getAccommodation() + "|"
                + request.getTransportation() + "|"
                + request.getExtraRequirements() + "|"
                + request.getPreferences();
    }

    private TripPlanRequest copyRequest(TripPlanRequest request) {
        return new TripPlanRequest(
                request.getCity(),
                request.getStartDate(),
                request.getEndDate(),
                request.getBudget(),
                request.getPreferences() == null ? new ArrayList<>() : new ArrayList<>(request.getPreferences()),
                request.getTransportation(),
                request.getAccommodation(),
                request.getExtraRequirements()
        );
    }
}
