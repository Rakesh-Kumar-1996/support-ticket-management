package com.supporttickets;

import com.supporttickets.domain.TicketStatusMachine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public TicketStatusMachine ticketStatusMachine() {
        return new TicketStatusMachine();
    }
}
