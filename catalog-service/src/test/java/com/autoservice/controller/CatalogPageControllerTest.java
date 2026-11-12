package com.autoservice.controller;

import com.autoservice.dto.ServiceRequest;
import com.autoservice.dto.ServiceResponse;
import com.autoservice.service.CatalogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogPageControllerTest {
    @Mock CatalogService service;
    @Mock BindingResult errors;
    @InjectMocks CatalogPageController controller;
    private final ServiceRequest request = new ServiceRequest("Діагностика", "Огляд", new BigDecimal("300.00"), 30);
    private final ServiceResponse response = new ServiceResponse(7L, "Діагностика", "Огляд", new BigDecimal("300.00"), 30);

    @Test
    void canEdit_adminRole_returnsTrue() {
        // Arrange
        var authentication = new UsernamePasswordAuthenticationToken("manager", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        // Act
        boolean result = controller.canEdit(authentication);
        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void canEdit_userRole_returnsFalse() {
        // Arrange
        var authentication = new UsernamePasswordAuthenticationToken("client", "", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        // Act
        boolean result = controller.canEdit(authentication);
        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void canEdit_missingAuthentication_returnsFalse() {
        // Arrange / Act
        boolean result = controller.canEdit(null);
        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void list_existingServices_addsServicesToModel() {
        // Arrange
        var model = new ExtendedModelMap();
        when(service.findAll()).thenReturn(List.of(response));
        // Act
        String view = controller.list(model);
        // Assert
        assertThat(view).isEqualTo("catalog");
        assertThat(model.get("services")).isEqualTo(List.of(response));
        verify(service).findAll();
    }

    @Test
    void newForm_initialRequest_addsDefaultsAndCreateAction() {
        // Arrange
        var model = new ExtendedModelMap();
        // Act
        String view = controller.newForm(model);
        // Assert
        assertThat(view).isEqualTo("service-form");
        assertThat(model.get("action")).isEqualTo("/services");
        var form = (ServiceRequest) model.get("form");
        assertThat(form.name()).isEmpty();
        assertThat(form.price()).isEqualByComparingTo("100.00");
        assertThat(form.durationMinutes()).isEqualTo(30);
        verifyNoInteractions(service);
    }

    @Test
    void create_validForm_createsServiceAndRedirectsWithMessage() {
        // Arrange
        var model = new ExtendedModelMap();
        var redirect = new RedirectAttributesModelMap();
        // Act
        String view = controller.create(request, errors, model, redirect);
        // Assert
        assertThat(view).isEqualTo("redirect:/services");
        assertThat(redirect.getFlashAttributes().get("message")).isEqualTo("Послугу додано");
        verify(service).create(request);
    }

    @Test
    void create_invalidForm_returnsFormWithoutSaving() {
        // Arrange
        var model = new ExtendedModelMap();
        var redirect = new RedirectAttributesModelMap();
        when(errors.hasErrors()).thenReturn(true);
        // Act
        String view = controller.create(request, errors, model, redirect);
        // Assert
        assertThat(view).isEqualTo("service-form");
        assertThat(model.get("action")).isEqualTo("/services");
        assertThat(redirect.getFlashAttributes()).isEmpty();
        verifyNoInteractions(service);
    }

    @Test
    void editForm_existingService_addsCurrentValuesAndUpdateAction() {
        // Arrange
        var model = new ExtendedModelMap();
        when(service.findById(7L)).thenReturn(response);
        // Act
        String view = controller.editForm(7L, model);
        // Assert
        assertThat(view).isEqualTo("service-form");
        assertThat(model.get("action")).isEqualTo("/services/7");
        assertThat(model.get("form")).isEqualTo(request);
        verify(service).findById(7L);
    }

    @Test
    void update_validForm_updatesServiceAndRedirectsWithMessage() {
        // Arrange
        var model = new ExtendedModelMap();
        var redirect = new RedirectAttributesModelMap();
        // Act
        String view = controller.update(7L, request, errors, model, redirect);
        // Assert
        assertThat(view).isEqualTo("redirect:/services");
        assertThat(redirect.getFlashAttributes().get("message")).isEqualTo("Зміни збережено");
        verify(service).update(7L, request);
    }

    @Test
    void update_invalidForm_returnsFormWithoutUpdating() {
        // Arrange
        var model = new ExtendedModelMap();
        var redirect = new RedirectAttributesModelMap();
        when(errors.hasErrors()).thenReturn(true);
        // Act
        String view = controller.update(7L, request, errors, model, redirect);
        // Assert
        assertThat(view).isEqualTo("service-form");
        assertThat(model.get("action")).isEqualTo("/services/7");
        assertThat(redirect.getFlashAttributes()).isEmpty();
        verifyNoInteractions(service);
    }

    @Test
    void delete_existingService_deletesAndRedirectsWithMessage() {
        // Arrange
        var redirect = new RedirectAttributesModelMap();
        // Act
        String view = controller.delete(7L, redirect);
        // Assert
        assertThat(view).isEqualTo("redirect:/services");
        assertThat(redirect.getFlashAttributes().get("message")).isEqualTo("Послугу видалено");
        verify(service).delete(7L);
    }
}
