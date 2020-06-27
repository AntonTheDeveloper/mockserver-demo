package local.sandbox.mockserver_demo.service

interface VoteService {
    fun checkGreeting()
    fun checkIsItVotingDayNow()
    fun voteYes(): String
    fun voteNo(): String
}
