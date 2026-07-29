package com.example.demo.config;

import org.springframework.amqp.core.*;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "ai.processing.rabbitmq", havingValue = "true")
public class RabbitMQConfig {

    public static final String TEXT_QUEUE = "ai.text.queue";
    public static final String IMAGE_QUEUE = "ai.image.queue";
    public static final String VIDEO_QUEUE = "ai.video.queue";
    public static final String PROMPT_QUEUE = "prompt.suggestion.queue";
    public static final String EXCHANGE = "ai.jobs.exchange";
    public static final String TEXT_ROUTING_KEY = "ai.text";
    public static final String IMAGE_ROUTING_KEY = "ai.image";
    public static final String VIDEO_ROUTING_KEY = "ai.video";
    public static final String PROMPT_ROUTING_KEY = "prompt.suggestion";

    @Bean public Queue textQueue() { return QueueBuilder.durable(TEXT_QUEUE).build(); }
    @Bean public Queue imageQueue() { return QueueBuilder.durable(IMAGE_QUEUE).build(); }
    @Bean public Queue videoQueue() { return QueueBuilder.durable(VIDEO_QUEUE).build(); }
    @Bean public Queue promptQueue() { return QueueBuilder.durable(PROMPT_QUEUE).build(); }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean public Binding textBinding(
            @Qualifier("textQueue") Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(TEXT_ROUTING_KEY);
    }

    @Bean public Binding imageBinding(
            @Qualifier("imageQueue") Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(IMAGE_ROUTING_KEY);
    }

    @Bean public Binding videoBinding(
            @Qualifier("videoQueue") Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(VIDEO_ROUTING_KEY);
    }

    @Bean public Binding promptBinding(
            @Qualifier("promptQueue") Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(PROMPT_ROUTING_KEY);
    }

    @Bean
    public MessageConverter converter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper, "com.example.demo.dto");
    }

    @Bean
    public RabbitTemplate template(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}
