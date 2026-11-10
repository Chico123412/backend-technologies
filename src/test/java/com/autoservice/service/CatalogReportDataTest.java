package com.autoservice.service;

import com.autoservice.dto.ServiceResponse;
import com.autoservice.repository.ServiceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogReportDataTest {
    @TempDir Path directory;

    @Test
    void exportWritesActualCatalogToJson() throws Exception {
        var catalog = mock(CatalogService.class);
        var json = new ObjectMapper();
        when(catalog.findAll()).thenReturn(List.of(
                new ServiceResponse(7L, "Діагностика", "Огляд", new BigDecimal("500"), 45)));
        var data = new CatalogReportData(catalog, mock(ServiceRepository.class), json,
                directory.toString(), 0);
        var id = UUID.randomUUID();

        data.export(id);

        var rows = json.readTree(directory.resolve(id + ".json").toFile());
        assertThat(rows.size()).isEqualTo(1);
        assertThat(rows.get(0).get("name").asText()).isEqualTo("Діагностика");
    }
}
