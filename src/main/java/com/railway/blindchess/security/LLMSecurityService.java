package com.railway.blindchess.security;

import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class LLMSecurityService {

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("ignore\\s+(all\\s+)?previous\\s+instructions?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("you\\s+are\\s+now\\s+(in\\s+)?developer\\s+mode", Pattern.CASE_INSENSITIVE),
            Pattern.compile("system\\s+override", Pattern.CASE_INSENSITIVE),
            Pattern.compile("reveal\\s+(your\\s+)?prompt", Pattern.CASE_INSENSITIVE),
            Pattern.compile("show\\s+(me\\s+)?(your\\s+)?instructions?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("what\\s+(were|are)\\s+(your\\s+)?(exact\\s+)?instructions?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("repeat\\s+the\\s+text\\s+above", Pattern.CASE_INSENSITIVE),
            Pattern.compile("act\\s+as\\s+if\\s+you('re|\\s+are)\\s+not\\s+bound", Pattern.CASE_INSENSITIVE),
            Pattern.compile("bypass\\s+(all\\s+)?safety", Pattern.CASE_INSENSITIVE),
            Pattern.compile("jailbreak", Pattern.CASE_INSENSITIVE),
            Pattern.compile("DAN\\s+mode", Pattern.CASE_INSENSITIVE)
    );

    private static final List<String> FUZZY_KEYWORDS = List.of(
            "ignore", "bypass", "override", "reveal", "delete", "system", "prompt", "instruction"
    );

    private static final List<Pattern> OUTPUT_SUSPICIOUS_PATTERNS = List.of(
            Pattern.compile("SYSTEM\\s*[:]\\s*You\\s+are", Pattern.CASE_INSENSITIVE),
            Pattern.compile("API[_\\s]?KEY\\s*[:=]\\s*\\w+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("instructions?\\s*[:]\\s*\\d+\\.", Pattern.CASE_INSENSITIVE),
            Pattern.compile("password\\s*[:=]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("secret\\s*[:=]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("token\\s*[:=]", Pattern.CASE_INSENSITIVE)
    );

    private static final int MAX_INPUT_LENGTH = 100;
    private static final int MAX_OUTPUT_LENGTH = 500;

    public boolean detectInjection(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }

        if (containsBase64Injection(text)) {
            return true;
        }

        if (containsTypoglycemiaAttack(text)) {
            return true;
        }

        return false;
    }

    private boolean containsBase64Injection(String text) {
        String[] words = text.split("\\s+");
        for (String word : words) {
            if (word.length() > 20 && word.matches("^[A-Za-z0-9+/=]+$")) {
                try {
                    String decoded = new String(Base64.getDecoder().decode(word));
                    if (detectInjection(decoded)) {
                        return true;
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return false;
    }

    private boolean containsTypoglycemiaAttack(String text) {
        String[] words = text.toLowerCase().split("\\s+");
        for (String word : words) {
            for (String keyword : FUZZY_KEYWORDS) {
                if (isTypoglycemiaVariant(word, keyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isTypoglycemiaVariant(String word, String target) {
        if (word.length() != target.length() || word.length() < 4) {
            return false;
        }
        if (word.equals(target)) {
            return false;
        }
        if (word.charAt(0) != target.charAt(0) || word.charAt(word.length() - 1) != target.charAt(target.length() - 1)) {
            return false;
        }
        char[] wordMiddle = word.substring(1, word.length() - 1).toCharArray();
        char[] targetMiddle = target.substring(1, target.length() - 1).toCharArray();
        java.util.Arrays.sort(wordMiddle);
        java.util.Arrays.sort(targetMiddle);
        return java.util.Arrays.equals(wordMiddle, targetMiddle);
    }

    public String sanitizeInput(String text) {
        if (text == null) {
            return "";
        }

        String sanitized = text.replaceAll("\\s+", " ").trim();

        sanitized = sanitized.replaceAll("(.)\\1{3,}", "$1");

        sanitized = sanitized.replaceAll("[<>\"'`;|&$]", "");

        if (sanitized.length() > MAX_INPUT_LENGTH) {
            sanitized = sanitized.substring(0, MAX_INPUT_LENGTH);
        }

        return sanitized;
    }

    public boolean validateOutput(String output) {
        if (output == null || output.isEmpty()) {
            return true;
        }

        for (Pattern pattern : OUTPUT_SUSPICIOUS_PATTERNS) {
            if (pattern.matcher(output).find()) {
                return false;
            }
        }

        return true;
    }

    public String filterOutput(String output) {
        if (output == null) {
            return null;
        }

        if (!validateOutput(output)) {
            return null;
        }

        if (output.length() > MAX_OUTPUT_LENGTH) {
            output = output.substring(0, MAX_OUTPUT_LENGTH);
        }

        return output.trim();
    }

    public ValidationResult validateAndSanitize(String playerName, String playerMove) {
        if (detectInjection(playerName) || detectInjection(playerMove)) {
            return new ValidationResult(false, null, null);
        }

        String sanitizedName = sanitizeInput(playerName);
        String sanitizedMove = sanitizeInput(playerMove);

        return new ValidationResult(true, sanitizedName, sanitizedMove);
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String sanitizedPlayerName;
        private final String sanitizedPlayerMove;

        public ValidationResult(boolean valid, String sanitizedPlayerName, String sanitizedPlayerMove) {
            this.valid = valid;
            this.sanitizedPlayerName = sanitizedPlayerName;
            this.sanitizedPlayerMove = sanitizedPlayerMove;
        }

        public boolean isValid() {
            return valid;
        }

        public String getSanitizedPlayerName() {
            return sanitizedPlayerName;
        }

        public String getSanitizedPlayerMove() {
            return sanitizedPlayerMove;
        }
    }
}
