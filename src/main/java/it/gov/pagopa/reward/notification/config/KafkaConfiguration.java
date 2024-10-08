package it.gov.pagopa.reward.notification.config;

import jakarta.annotation.PostConstruct;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "spring.cloud")
@Getter
@Setter
public class KafkaConfiguration {
    private Stream stream;
    @Getter
    @Setter
    public static class Stream {
        private Map<String, KafkaInfoDTO> bindings;
        private Map<String,Binders> binders;
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    public static class BaseKafkaInfoDTO {
        private String destination;
        private String group;
        private String type;
        private String brokers;
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    public static class KafkaInfoDTO extends BaseKafkaInfoDTO{
        private String binder;
    }
    @PostConstruct
    public void enrichKafkaInfoDTO(){
        stream.getBindings().forEach((k,v) -> {
            v.setType(getTypeForBinder(v.getBinder()));
            v.setBrokers(getBrokersForBinder(v.getBinder()));
        });
    }

    @Getter
    @Setter
    public static class Binders{
        private String type;
        private Environment environment;
    }

    @Getter
    @Setter
    public static class Environment {
        private Spring spring;
    }
    @Getter
    @Setter
    public static class Spring{
        private Cloud cloud;
    }

    @Getter
    @Setter
    public  static class Cloud{
        private StreamBinder stream;
    }

    @Getter
    @Setter
    public static class StreamBinder{
        private Kafka kafka;
    }

    @Getter
    @Setter
    public static class Kafka{
        private Binder binder;
    }

    @Getter
    @Setter
    public static  class Binder{
        private String brokers;
    }

    public String getTypeForBinder(String binderName) {
        if (stream != null && stream.getBinders() != null) {
            Binders binders = stream.getBinders().get(binderName);
            if (binders != null) {
                return binders.getType();
            }
        }
        return null;
    }
    public String getBrokersForBinder(String binderName) {
        if (stream != null && stream.getBinders() != null) {
            Binders binders = stream.getBinders().get(binderName);
            if(binders != null && (binders.getEnvironment() != null)) {
                return binders.getEnvironment().getSpring().getCloud().getStream().getKafka().getBinder().getBrokers();
            }
        }
        return null;
    }

}