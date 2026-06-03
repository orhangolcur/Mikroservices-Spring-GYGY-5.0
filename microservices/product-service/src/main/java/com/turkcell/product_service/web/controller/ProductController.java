package com.turkcell.product_service.web.controller;

import java.time.Instant;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkcell.product_service.entity.OutboxEvent;
import com.turkcell.product_service.entity.OutboxStatus;
import com.turkcell.product_service.event.TestEvent;
import com.turkcell.product_service.repository.OutboxRepository;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public ProductController(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello Product-Service";
    }

    @GetMapping
    public String sendTestEvent(@RequestParam String message) {
        UUID id = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        var event = new TestEvent(eventId, message, id);

            // KAFKAYA bir event gidecekse, önce kayıt altına alınacak.
        // Outbox -> XEvent,XTarihi,XTopic,XPayload


        // Daha sonra bir mekanizma bu kayıtları okuyacak ve kafkaya gönderecek.
        // POLLING ->
        // POLLING -> Belirli aralıklarla veritabanaına bak, gönderilecek bir event var mı?
        // Her 20 snde => SElect * from outbox where status = 'PENDING' and retryCount < 3
        // CDC (Change Data Capture)
        // Debezium gibi bir mekanizma

        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setId(eventId);
        outboxEvent.setAggregateType("Product");
        outboxEvent.setAggregateId(id.toString());
        outboxEvent.setEventType("testEvent");
        outboxEvent.setPayload(toJson(event));
        outboxEvent.setStatus(OutboxStatus.PENDING);
        outboxEvent.setCreatedAt(Instant.now());

        outboxRepository.save(outboxEvent);

        outboxRepository.save(outboxEvent);

        return "Başarılı";
    }
     private String toJson(Object o)
    {
        try { return objectMapper.writeValueAsString(o);}
        catch(Exception e) { throw new RuntimeException(e); }
    }
}
 
