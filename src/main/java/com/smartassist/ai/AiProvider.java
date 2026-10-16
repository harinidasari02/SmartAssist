package com.smartassist.ai;

import com.smartassist.dto.ChatTurn;
import java.util.List;

/** One AI backend. The chat layer only knows this interface, so providers can be swapped or added. */
public interface AiProvider {
    String name();

    /** Sends the conversation so far (oldest first, ending with the new user message) and returns the reply. */
    String chat(List<ChatTurn> turns);
}
