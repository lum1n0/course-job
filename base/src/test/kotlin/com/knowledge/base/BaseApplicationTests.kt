 package com.knowledge.base

 import org.junit.jupiter.api.Test
 import org.springframework.boot.test.context.SpringBootTest
 import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration // Импортируем это

@SpringBootTest
class BaseApplicationTests {

 	@Test
 	fun contextLoads() {
          println("TEST")
 	}

 }
