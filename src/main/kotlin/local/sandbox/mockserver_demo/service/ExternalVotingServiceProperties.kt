package local.sandbox.mockserver_demo.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class ExternalVotingServiceProperties {
    @Value("\${external-service.voting.host}")
    lateinit var host: String
}