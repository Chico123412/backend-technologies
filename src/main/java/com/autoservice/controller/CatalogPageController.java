package com.autoservice.controller;

import com.autoservice.dto.ServiceRequest;
import com.autoservice.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;

@Controller
@RequestMapping("/services")
public class CatalogPageController {
    private final CatalogService service;

    public CatalogPageController(CatalogService service) {
        this.service = service;
    }

    @ModelAttribute("canEdit")
    public boolean canEdit(org.springframework.security.core.Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("services", service.findAll());
        return "catalog";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new ServiceRequest("", "", new BigDecimal("100.00"), 30));
        model.addAttribute("action", "/services");
        return "service-form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public String create(@Valid @ModelAttribute("form") ServiceRequest form, BindingResult errors,
                         Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            model.addAttribute("action", "/services");
            return "service-form";
        }
        service.create(form);
        redirect.addFlashAttribute("message", "Послугу додано");
        return "redirect:/services";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        var item = service.findById(id);
        model.addAttribute("form", new ServiceRequest(item.name(), item.description(), item.price(), item.durationMinutes()));
        model.addAttribute("action", "/services/" + id);
        return "service-form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}")
    public String update(@PathVariable long id, @Valid @ModelAttribute("form") ServiceRequest form,
                         BindingResult errors, Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            model.addAttribute("action", "/services/" + id);
            return "service-form";
        }
        service.update(id, form);
        redirect.addFlashAttribute("message", "Зміни збережено");
        return "redirect:/services";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes redirect) {
        service.delete(id);
        redirect.addFlashAttribute("message", "Послугу видалено");
        return "redirect:/services";
    }
}
