package pers.clare.demo.session;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pers.clare.core.message.AbstractGenericMessageService;
import pers.clare.core.message.MessageService;

@Service
public class SessionNotifyService extends AbstractGenericMessageService<String> {

    public SessionNotifyService(
            MessageService messageService
            , @Value("${session.notify.topic:default}"
    ) String topic) {
        super(messageService, topic);
    }
}
