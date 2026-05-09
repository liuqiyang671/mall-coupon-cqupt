package com.mall.cqupt.lqy.distribution.service.basics;

import com.mall.cqupt.framework.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DistributionStrategyChooseTest {

    @Test
    void runRegistersStrategiesAndChoosesByMark() throws Exception {
        DistributionStrategyChoose chooser = new DistributionStrategyChoose();
        EchoStrategy strategy = new EchoStrategy("ECHO");
        chooser.setApplicationContext(applicationContext(Map.of("echoStrategy", strategy)));

        chooser.run();

        assertSame(strategy, chooser.choose("ECHO"));
        assertEquals("PING", chooser.chooseAndExecuteResp("ECHO", "ping"));
        chooser.chooseAndExecute("ECHO", "event");
        assertTrue(strategy.executed);
    }

    @Test
    void chooseRejectsUnknownStrategyMark() {
        DistributionStrategyChoose chooser = new DistributionStrategyChoose();

        assertThrows(ServiceException.class, () -> chooser.choose("MISSING"));
    }

    @Test
    void runRejectsDuplicateStrategyMark() {
        DistributionStrategyChoose chooser = new DistributionStrategyChoose();
        Map<String, DistributionExecuteStrategy> strategies = new LinkedHashMap<>();
        strategies.put("first", new EchoStrategy("ECHO"));
        strategies.put("second", new EchoStrategy("ECHO"));
        chooser.setApplicationContext(applicationContext(strategies));

        assertThrows(ServiceException.class, () -> chooser.run());
    }

    private ApplicationContext applicationContext(Map<String, DistributionExecuteStrategy> strategies) {
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBeansOfType(DistributionExecuteStrategy.class)).thenReturn(strategies);
        return context;
    }

    private static class EchoStrategy implements DistributionExecuteStrategy<String, String> {
        private final String mark;
        private boolean executed;

        EchoStrategy(String mark) {
            this.mark = mark;
        }

        @Override
        public String mark() {
            return mark;
        }

        @Override
        public void execute(String requestParam) {
            executed = true;
        }

        @Override
        public String executeResp(String requestParam) {
            return requestParam.toUpperCase();
        }
    }
}
