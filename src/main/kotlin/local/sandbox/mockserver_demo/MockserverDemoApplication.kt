package local.sandbox.mockserver_demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MockserverDemoApplication

fun main(args: Array<String>) {
    runApplication<MockserverDemoApplication>(*args)
}
