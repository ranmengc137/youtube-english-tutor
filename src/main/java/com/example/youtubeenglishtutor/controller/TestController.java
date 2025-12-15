package com.example.youtubeenglishtutor.controller;

import com.example.youtubeenglishtutor.entity.Question;
import com.example.youtubeenglishtutor.entity.Test;
import com.example.youtubeenglishtutor.entity.WrongQuestion;
import com.example.youtubeenglishtutor.service.DifficultyLevel;
import com.example.youtubeenglishtutor.service.ObservabilityService;
import com.example.youtubeenglishtutor.service.RagService;
import com.example.youtubeenglishtutor.service.TestService;
import com.example.youtubeenglishtutor.web.LearnerContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.regex.Pattern;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/tests")
public class TestController {

    private final TestService testService;
    private final RagService ragService;
    private final ObservabilityService observabilityService;
    private final LearnerContext learnerContext;

    public TestController(TestService testService, RagService ragService, ObservabilityService observabilityService, LearnerContext learnerContext) {
        this.testService = testService;
        this.ragService = ragService;
        this.observabilityService = observabilityService;
        this.learnerContext = learnerContext;
    }

    @GetMapping
    public String listTests(Model model) {
        List<Test> tests = testService.listTests().stream()
                .sorted(Comparator.comparing(Test::getCreatedAt).reversed())
                .collect(Collectors.toList());
        model.addAttribute("tests", tests);
        return "history";
    }

    @GetMapping("/new")
    public String newTestForm() {
        return "new-test";
    }

    @PostMapping
    public String createTest(
            @RequestParam("videoUrl") String videoUrl,
            @RequestParam(value = "downloadPath", required = false) String downloadPath,
            @RequestParam(value = "useDefaultPath", defaultValue = "false") boolean useDefaultPath,
            Model model) {
        try {
            Test test = testService.createTest(videoUrl, downloadPath, useDefaultPath);
            return "redirect:/tests/" + test.getId();
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "new-test";
        }
    }

    @GetMapping("/{id}")
    public String takeTest(@PathVariable("id") Long id, Model model) {
        Test test = testService.getTest(id);
        if (test == null) {
            return "redirect:/tests";
        }
        model.addAttribute("test", test);
        return "take-test";
    }

    @PostMapping("/{id}/submit")
    public String submitAnswers(@PathVariable("id") Long id, HttpServletRequest request, Model model) {
        Map<Long, List<String>> answers = extractAnswers(request);
        Test test = testService.submitAnswers(id, answers);
        if (test == null) {
            return "redirect:/tests";
        }
        populateResultModel(test, answers, model);
        return "result";
    }

    @GetMapping("/{id}/result")
    public String viewResult(@PathVariable("id") Long id, Model model) {
        Test test = testService.getTest(id);
        if (test == null) {
            return "redirect:/tests";
        }
        populateResultModel(test, null, model);
        return "result";
    }

    @PostMapping("/{id}/feedback")
    public String submitFeedback(
            @PathVariable("id") Long id,
            @RequestParam(value = "rating", required = false) String rating,
            @RequestParam(value = "comment", required = false) String comment) {
        String learnerId = learnerContext.getCurrentLearnerId();
        String combined = (rating != null ? rating + " " : "") + (comment != null ? comment : "");
        observabilityService.logFeedback(learnerId, id, null, combined.trim(), rating);
        return "redirect:/tests/" + id + "/result";
    }

    @PostMapping("/{id}/flag")
    public String flagQuestion(
            @PathVariable("id") Long id,
            @RequestParam("questionId") Long questionId,
            @RequestParam(value = "reason", required = false) String reason) {
        String learnerId = learnerContext.getCurrentLearnerId();
        observabilityService.logFeedback(learnerId, id, questionId, reason, "QUESTION_FLAG");
        return "redirect:/tests/" + id + "/result";
    }

    @PostMapping("/{id}/regenerate")
    public String regenerate(
            @PathVariable("id") Long id,
            @RequestParam("difficulty") String difficulty) {
        DifficultyLevel level = DifficultyLevel.NORMAL;
        if ("EASIER".equalsIgnoreCase(difficulty)) {
            level = DifficultyLevel.EASIER;
        } else if ("HARDER".equalsIgnoreCase(difficulty)) {
            level = DifficultyLevel.HARDER;
        }
        Test regenerated = testService.regenerateTest(id, level);
        if (regenerated == null) {
            return "redirect:/tests";
        }
        String learnerId = learnerContext.getCurrentLearnerId();
        observabilityService.logFeedback(learnerId, id, null, "Regenerated with " + level, "REGENERATE");
        return "redirect:/tests/" + regenerated.getId();
    }

    @GetMapping("/{id}/wrong-questions")
    public String reviewWrongQuestions(@PathVariable("id") Long id, Model model) {
        Test test = testService.getTest(id);
        if (test == null) {
            return "redirect:/tests";
        }
        List<WrongQuestion> wrongQuestions = new ArrayList<>(test.getWrongQuestions());
        model.addAttribute("test", test);
        model.addAttribute("wrongQuestions", wrongQuestions);
        return "review-wrong-questions";
    }

    private Map<Long, List<String>> extractAnswers(HttpServletRequest request) {
        Map<Long, List<String>> answers = new HashMap<>();
        request.getParameterMap().forEach((paramName, values) -> {
            if (paramName.startsWith("q_")) {
                try {
                    Long questionId = Long.parseLong(paramName.substring(2));
                    answers.put(questionId, values != null ? List.of(values) : List.of());
                } catch (NumberFormatException ignored) {
                }
            }
        });
        return answers;
    }

    private void populateResultModel(Test test, Map<Long, List<String>> answers, Model model) {
        Map<Long, String> wrongAnswers = test.getWrongQuestions().stream()
                .collect(Collectors.toMap(wq -> wq.getQuestion().getId(), WrongQuestion::getUserAnswer));
        Map<Long, String> userAnswers = new HashMap<>();
        for (Question question : test.getQuestions()) {
            Long qid = question.getId();
            if (answers != null && answers.containsKey(qid)) {
                userAnswers.put(qid, String.join(";", answers.get(qid)));
            } else if (wrongAnswers.containsKey(qid)) {
                userAnswers.put(qid, wrongAnswers.get(qid));
            } else {
                userAnswers.put(qid, "Correct");
            }
        }
        List<Long> wrongQuestionIds = test.getWrongQuestions().stream()
                .map(wq -> wq.getQuestion().getId())
                .toList();
        Map<Long, String> snippets = buildSnippets(test);
        model.addAttribute("test", test);
        model.addAttribute("userAnswers", userAnswers);
        model.addAttribute("wrongQuestionIds", wrongQuestionIds);
        model.addAttribute("snippets", snippets);
    }

    private Map<Long, String> buildSnippets(Test test) {
        Map<Long, String> snippets = new HashMap<>();
        for (Question question : test.getQuestions()) {
            String snippet = ragService.findBestSnippet(test, question);
            snippets.put(question.getId(), highlight(snippet, question));
        }
        return snippets;
    }

    private String highlight(String text, Question question) {
        if (text == null || text.isBlank() || question == null) {
            return text;
        }
        List<String> keywords = question.getCorrectAnswerList();
        String highlighted = applyHighlights(text, keywords);
        if (!highlighted.equals(text)) {
            return highlighted;
        }
        // Fallback: highlight tokens (length>=4) from answers and question text.
        List<String> tokens = Stream.concat(
                        keywords.stream().flatMap(k -> Stream.of(k.split("\\s+"))),
                        question.getText() != null ? Stream.of(question.getText().split("\\s+")) : Stream.empty())
                .filter(token -> token != null && token.length() >= 4)
                .toList();
        return applyHighlights(text, tokens);
    }

    private String applyHighlights(String text, List<String> terms) {
        String result = text;
        for (String term : terms) {
            if (term == null || term.isBlank()) {
                continue;
            }
            String escaped = Pattern.quote(term.trim());
            result = result.replaceAll("(?i)(" + escaped + ")", "<mark>$1</mark>");
        }
        return result;
    }
}
