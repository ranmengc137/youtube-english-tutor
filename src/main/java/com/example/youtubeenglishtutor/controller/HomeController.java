package com.example.youtubeenglishtutor.controller;

import com.example.youtubeenglishtutor.entity.Message;
import com.example.youtubeenglishtutor.repository.MessageRepository;
import com.example.youtubeenglishtutor.web.LearnerContext;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {

    private final MessageRepository messageRepository;
    private final LearnerContext learnerContext;

    public HomeController(MessageRepository messageRepository, LearnerContext learnerContext) {
        this.messageRepository = messageRepository;
        this.learnerContext = learnerContext;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<Message> messages = messageRepository.findTop20ByDeletedFalseOrderByCreatedAtDesc();
        messages.sort(Comparator.comparing(Message::getCreatedAt).reversed());
        model.addAttribute("messages", messages);
        model.addAttribute("currentLearnerId", learnerContext.getCurrentLearnerId());
        return "home";
    }

    @PostMapping("/messages")
    public String postMessage(
            @RequestParam(value = "author", required = false) String author,
            @RequestParam("content") String content,
            RedirectAttributes redirectAttributes) {
        String learnerId = learnerContext.getCurrentLearnerId();
        if (!StringUtils.hasText(content)) {
            redirectAttributes.addFlashAttribute("messageError", "Message cannot be empty.");
            return "redirect:/";
        }
        try {
            String sanitizedContent = content.trim();
            // De-dupe accidental double submits within same learner/content.
            Message existing = messageRepository.findTop1ByLearnerIdAndContentAndDeletedFalseOrderByCreatedAtDesc(learnerId, sanitizedContent);
            if (existing != null) {
                redirectAttributes.addFlashAttribute("messageSuccess", "Message already posted.");
                return "redirect:/";
            }
            Message message = new Message();
            message.setAuthor(StringUtils.hasText(author) ? author.trim() : "Anonymous");
            message.setContent(sanitizedContent);
            message.setLearnerId(learnerId);
            messageRepository.save(message);
            redirectAttributes.addFlashAttribute("messageSuccess", "Message posted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("messageError", "Could not post message. Please try again.");
        }
        return "redirect:/";
    }

    @PostMapping("/messages/{id}/edit")
    public String editMessage(
            @PathVariable("id") Long id,
            @RequestParam("content") String content,
            RedirectAttributes redirectAttributes) {
        String learnerId = learnerContext.getCurrentLearnerId();
        if (!StringUtils.hasText(content)) {
            redirectAttributes.addFlashAttribute("messageError", "Message cannot be empty.");
            return "redirect:/";
        }
        Message message = messageRepository.findByIdAndLearnerIdAndDeletedFalse(id, learnerId);
        if (message == null) {
            redirectAttributes.addFlashAttribute("messageError", "Message not found or not yours.");
            return "redirect:/";
        }
        message.setContent(content.trim());
        messageRepository.save(message);
        redirectAttributes.addFlashAttribute("messageSuccess", "Message updated.");
        return "redirect:/";
    }

    @PostMapping("/messages/{id}/delete")
    public String deleteMessage(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {
        String learnerId = learnerContext.getCurrentLearnerId();
        Message message = messageRepository.findByIdAndLearnerIdAndDeletedFalse(id, learnerId);
        if (message == null) {
            redirectAttributes.addFlashAttribute("messageError", "Message not found or not yours.");
            return "redirect:/";
        }
        message.setDeleted(true); // soft delete
        messageRepository.save(message);
        redirectAttributes.addFlashAttribute("messageSuccess", "Message removed.");
        return "redirect:/";
    }
}
