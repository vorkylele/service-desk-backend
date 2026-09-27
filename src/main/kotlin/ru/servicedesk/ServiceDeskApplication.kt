package ru.servicedesk

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class ServiceDeskApplication

fun main(args: Array<String>) {
    runApplication<ServiceDeskApplication>(*args)
}
