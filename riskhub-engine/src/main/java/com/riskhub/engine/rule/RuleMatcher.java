package com.riskhub.engine.rule;

import com.fasterxml.jackson.databind.JsonNode;
import com.riskhub.common.dto.AuditSubmitRequest;
import com.riskhub.store.entity.RiskRuleEntity;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 规则匹配器：根据条件类型执行不同的匹配逻辑
 */
public class RuleMatcher {

    /**
     * 执行单条规则匹配
     */
    public static RuleHitResult match(RiskRuleEntity rule, AuditSubmitRequest request, JsonNode conditionNode) {
        String type = rule.getConditionType();
        boolean hit = switch (type) {
            case "keyword" -> matchKeyword(conditionNode, request);
            case "regex" -> matchRegex(conditionNode, request);
            case "behavior_threshold" -> matchBehaviorThreshold(conditionNode, request);
            case "composite" -> matchComposite(conditionNode, request);
            default -> false;
        };

        if (hit) {
            return new RuleHitResult(
                    rule.getRuleId(),
                    rule.getRiskTopic(),
                    rule.getRiskLevel(),
                    rule.getActionHint(),
                    buildEvidence(type, rule.getRuleName()),
                    rule.getVersion()
            );
        }
        return null;
    }

    private static boolean matchKeyword(JsonNode node, AuditSubmitRequest request) {
        String text = getAllText(request);
        if (text == null || text.isEmpty()) return false;

        JsonNode keywords = node.get("keywords");
        if (keywords == null || !keywords.isArray()) return false;

        for (JsonNode kw : keywords) {
            if (text.contains(kw.asText())) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchRegex(JsonNode node, AuditSubmitRequest request) {
        String text = getAllText(request);
        if (text == null || text.isEmpty()) return false;

        JsonNode patternNode = node.get("pattern");
        if (patternNode == null) return false;

        try {
            Pattern pattern = Pattern.compile(patternNode.asText());
            return pattern.matcher(text).find();
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean matchBehaviorThreshold(JsonNode node, AuditSubmitRequest request) {
        Map<String, Object> behavior = request.getBehaviorFeatures();
        if (behavior == null || behavior.isEmpty()) return false;

        String field = node.has("field") ? node.get("field").asText() : null;
        String operator = node.has("operator") ? node.get("operator").asText() : null;
        double threshold = node.has("threshold") ? node.get("threshold").asDouble() : 0;

        if (field == null || operator == null) return false;

        Object value = behavior.get(field);
        if (value == null) return false;

        double numValue;
        if (value instanceof Number) {
            numValue = ((Number) value).doubleValue();
        } else {
            try {
                numValue = Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return switch (operator) {
            case ">=" -> numValue >= threshold;
            case ">" -> numValue > threshold;
            case "<=" -> numValue <= threshold;
            case "<" -> numValue < threshold;
            case "==" -> numValue == threshold;
            default -> false;
        };
    }

    private static boolean matchComposite(JsonNode node, AuditSubmitRequest request) {
        String logic = node.has("logic") ? node.get("logic").asText() : "AND";
        JsonNode conditions = node.get("conditions");
        if (conditions == null || !conditions.isArray()) return false;

        String text = getAllText(request);
        if (text == null || text.isEmpty()) return false;

        if ("AND".equalsIgnoreCase(logic)) {
            for (JsonNode cond : conditions) {
                if (!matchSubCondition(cond, text, request)) return false;
            }
            return true;
        } else {
            for (JsonNode cond : conditions) {
                if (matchSubCondition(cond, text, request)) return true;
            }
            return false;
        }
    }

    private static boolean matchSubCondition(JsonNode cond, String text, AuditSubmitRequest request) {
        String type = cond.has("type") ? cond.get("type").asText() : "";
        if ("keyword".equals(type)) {
            JsonNode keywords = cond.get("keywords");
            if (keywords != null && keywords.isArray()) {
                for (JsonNode kw : keywords) {
                    if (text.contains(kw.asText())) return true;
                }
            }
            return false;
        }
        return false;
    }

    private static String getAllText(AuditSubmitRequest request) {
        StringBuilder sb = new StringBuilder();
        if (request.getContentText() != null) {
            sb.append(request.getContentText());
        }
        List<String> evidenceList = request.getChatEvidenceList();
        if (evidenceList != null) {
            for (String e : evidenceList) {
                sb.append(" ").append(e);
            }
        }
        return sb.toString();
    }

    private static String buildEvidence(String type, String ruleName) {
        return "命中规则[" + ruleName + "], 类型=" + type;
    }
}
