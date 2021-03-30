package pers.clare.core.message;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractMessageService implements MessageService {

    protected List<Runnable> connectedListener = new ArrayList<>();

    @Override
    public void onConnected(Runnable runnable) {
        connectedListener.add(runnable);
    }
}
