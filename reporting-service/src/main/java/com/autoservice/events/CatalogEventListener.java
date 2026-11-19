package com.autoservice.events;

import com.autoservice.config.RabbitConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class CatalogEventListener {
    private final CatalogEventJournal journal;

    public CatalogEventListener(CatalogEventJournal journal) {
        this.journal = journal;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void receive(ServiceEvent event) {
        journal.accept(event);
    }
}
