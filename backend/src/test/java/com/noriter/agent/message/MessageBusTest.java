package com.noriter.agent.message;

import com.noriter.domain.AgentMessageEntity;
import com.noriter.domain.enums.AgentRole;
import com.noriter.domain.enums.MessageType;
import com.noriter.infrastructure.sse.SseEmitterService;
import com.noriter.repository.AgentMessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageBusTest {

    @InjectMocks
    private MessageBus messageBus;

    @Mock
    private AgentMessageRepository messageRepository;

    @Mock
    private SseEmitterService sseEmitterService;

    @Test
    @DisplayName("HANDOFF 메시지를 DB에 저장하고 SSE로 전달한다")
    void send_handoff_savesAndBroadcasts() {
        when(messageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AgentMessageEntity result = messageBus.send("prj_test",
                AgentRole.PLANNING, AgentRole.CTO,
                MessageType.HANDOFF, "기획서 완료", "art_001");

        assertThat(result.getFromAgent()).isEqualTo(AgentRole.PLANNING);
        assertThat(result.getToAgent()).isEqualTo(AgentRole.CTO);
        assertThat(result.getType()).isEqualTo(MessageType.HANDOFF);

        verify(messageRepository).save(any());
        verify(sseEmitterService).sendEvent(any(), any());
    }

    @Test
    @DisplayName("BUG_REPORT 메시지를 전송할 수 있다")
    void send_bugReport() {
        when(messageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AgentMessageEntity result = messageBus.send("prj_test",
                AgentRole.QA, AgentRole.CTO,
                MessageType.BUG_REPORT, "CRITICAL 버그 발견", null);

        assertThat(result.getType()).isEqualTo(MessageType.BUG_REPORT);
        assertThat(result.getArtifactRef()).isNull();
    }
}
