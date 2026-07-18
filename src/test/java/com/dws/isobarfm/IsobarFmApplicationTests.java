package com.dws.isobarfm;

import com.dws.isobarfm.application.port.out.LoadBandsPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class IsobarFmApplicationTests {

    @MockBean
    LoadBandsPort loadBandsPort;

    @Test
    void contextLoads() {
    }
}
