package local.sandbox.mockserver_demo.web

import local.sandbox.mockserver_demo.service.VoteService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vote")
class VoteForwardController(
    private val voteService: VoteService
) {
    @PostMapping("/yes")
    fun voteYes() = doChecksBeforeVoting().voteYesAndReturnStatus()

    @PostMapping("/no")
    fun voteNo() = doChecksBeforeVoting().voteNoAndReturnStatus()

    private val doChecksBeforeVoting = {
        apply {
            this.voteService.checkGreeting()
            this.voteService.checkIsItVotingDayNow()
        }
    }

    private val voteYesAndReturnStatus = { let { this.voteService.voteYes() } }

    private val voteNoAndReturnStatus = { let { this.voteService.voteNo() } }
}