package com.autoservice.controller;

import com.autoservice.events.CatalogEventJournal;
import com.autoservice.events.ServiceEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class CatalogJournalController {
    private final CatalogEventJournal journal;

    public CatalogJournalController(CatalogEventJournal journal) {
        this.journal = journal;
    }

    @GetMapping("/api/catalog-events")
    public List<ServiceEvent> recent() {
        return journal.recent();
    }
}
