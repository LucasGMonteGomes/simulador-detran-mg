package com.github.lucasgmontegomes.simulador_detran_mg;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SimuladorDetranMgApplicationTests {

	@Test
	void contextLoads() {
	}

}
