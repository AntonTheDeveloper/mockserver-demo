package local.sandbox.mockserver_demo.service

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class VoteServiceImpl(
    private val restTemplate: RestTemplate,
    private val externalVotingServiceProperties: ExternalVotingServiceProperties
) : VoteService {

    companion object {
        val STRING_RETURN_TYPE = ParameterizedTypeReference.forType<String>(String::class.java)
        val BOOLEAN_RETURN_TYPE = ParameterizedTypeReference.forType<Boolean>(Boolean::class.java)
        val VOTE_YES = Vote(true)
        val VOTE_NO = Vote(false)
    }

    override fun checkGreeting() {
        val greeting = exchangeExternalService(
            "/api/greeting",
            HttpMethod.GET,
            STRING_RETURN_TYPE
        )

        val expectedGreeting = "Glory to Arstotzka!"
        if (!greeting.equals(expectedGreeting)) {
            throw IllegalStateException("Greeting error, response was = $greeting")
        }
    }

    override fun checkIsItVotingDayNow() {
        val isItVotingDay = exchangeExternalService(
            "/api/is-it-voting-day",
            HttpMethod.POST,
            BOOLEAN_RETURN_TYPE
        )
        if (!isItVotingDay) {
            throw IllegalStateException("Today is not voting day")
        }
    }

    override fun voteYes() = this.voteAndGetResponse(VOTE_YES)

    override fun voteNo() = this.voteAndGetResponse(VOTE_NO)

    private fun voteAndGetResponse(vote: Vote) =
        this.exchangeExternalService(
            "/api/vote",
            HttpMethod.POST,
            STRING_RETURN_TYPE,
            HttpEntity(vote)
        )

    private fun <T> exchangeExternalService(
        apiUrl: String,
        httpMethod: HttpMethod,
        expectedReturnType: ParameterizedTypeReference<T>,
        payload: HttpEntity<*> = HttpEntity.EMPTY
    ): T {
        val host = this.externalVotingServiceProperties.host
        val response = this.restTemplate
            .exchange(
                "$host$apiUrl",
                httpMethod,
                payload,
                expectedReturnType
            )

        return this.getResult<T>(response, httpMethod, apiUrl)
    }

    private fun <T> getResult(
        response: ResponseEntity<T>,
        httpMethod: HttpMethod,
        apiUrl: String
    ): T {
        val statusCode = response.statusCode
        val body = response.body

        if (statusCode.isError) {
            throw IllegalStateException(
                "Request $httpMethod $apiUrl has failed with error $statusCode ${response.statusCodeValue} $body"
            )
        }

        if (body == null || (body is String && body.isBlank())) {
            throw IllegalStateException(
                "Request $httpMethod $apiUrl has returned empty body"
            )
        }

        return body
    }
}

data class Vote(var agree: Boolean)