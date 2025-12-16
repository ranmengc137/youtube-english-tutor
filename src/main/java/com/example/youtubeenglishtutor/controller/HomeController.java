package com.example.youtubeenglishtutor.controller;

import com.example.youtubeenglishtutor.entity.Message;
import com.example.youtubeenglishtutor.repository.MessageRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {

    private final MessageRepository messageRepository;

    public HomeController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<Message> messages = messageRepository.findTop20ByOrderByCreatedAtDesc();
        messages.sort(Comparator.comparing(Message::getCreatedAt).reversed());
        model.addAttribute("messages", messages);
        return "home";
    }

    @PostMapping("/messages")
    public String postMessage(
            @RequestParam(value = "author", required = false) String author,
            @RequestParam("content") String content,
            RedirectAttributes redirectAttributes) {
        if (!StringUtils.hasText(content)) {
            redirectAttributes.addFlashAttribute("messageError", "Message cannot be empty.");
            return "redirect:/";
        }
        Message message = new Message();
        message.setAuthor(StringUtils.hasText(author) ? author.trim() : "Anonymous");
        message.setContent(content.trim());
        messageRepository.save(message);
        redirectAttributes.addFlashAttribute("messageSuccess", "Message posted.");
        return "redirect:/";
    }
}
