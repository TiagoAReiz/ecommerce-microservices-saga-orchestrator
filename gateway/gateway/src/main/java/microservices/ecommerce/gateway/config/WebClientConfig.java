package microservices.ecommerce.gateway.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Value("${app.services.products-url}")
    private String productsUrl;

    @Value("${app.services.inventory-url}")
    private String inventoryUrl;

    @Value("${app.services.cart-url}")
    private String cartUrl;

    @Value("${app.services.order-url}")
    private String orderUrl;

    @Value("${app.services.payment-url}")
    private String paymentUrl;

    @Value("${app.services.delivery-url}")
    private String deliveryUrl;

    @Value("${app.webclient.connect-timeout-ms}")
    private int connectTimeoutMs;

    @Value("${app.webclient.read-timeout-ms}")
    private int readTimeoutMs;

    private WebClient buildWebClient(String baseUrl) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(readTimeoutMs));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean("productsWebClient")
    public WebClient productsWebClient() {
        return buildWebClient(productsUrl);
    }

    @Bean("inventoryWebClient")
    public WebClient inventoryWebClient() {
        return buildWebClient(inventoryUrl);
    }

    @Bean("cartWebClient")
    public WebClient cartWebClient() {
        return buildWebClient(cartUrl);
    }

    @Bean("orderWebClient")
    public WebClient orderWebClient() {
        return buildWebClient(orderUrl);
    }

    @Bean("paymentWebClient")
    public WebClient paymentWebClient() {
        return buildWebClient(paymentUrl);
    }

    @Bean("deliveryWebClient")
    public WebClient deliveryWebClient() {
        return buildWebClient(deliveryUrl);
    }
}
