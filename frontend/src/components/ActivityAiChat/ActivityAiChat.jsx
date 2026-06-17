import React, { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { FiSend, FiTrash2, FiCpu } from "react-icons/fi";
import AiService from "../../api/AiService";
import {
  ChatSection,
  ChatHeader,
  ChatTitle,
  ClearButton,
  MessageList,
  EmptyState,
  Bubble,
  TypingIndicator,
  SuggestionRow,
  SuggestionChip,
  InputRow,
  ChatInput,
  SendButton,
  ErrorText,
} from "./ActivityAiChatStyles";

/**
 * Claude-powered conversational analysis of a single activity. Loads any prior conversation,
 * lets the athlete ask follow-up questions, and shows a typing indicator while Claude replies.
 */
const ActivityAiChat = ({ activityId }) => {
  const { t } = useTranslation();
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  const [error, setError] = useState(null);
  const listRef = useRef(null);

  useEffect(() => {
    let active = true;
    AiService.getActivityChat(activityId)
      .then((res) => {
        if (active) setMessages(res.data?.messages || []);
      })
      .catch(() => {
        /* No prior conversation / not yet configured — start empty. */
      });
    return () => {
      active = false;
    };
  }, [activityId]);

  useEffect(() => {
    if (listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight;
    }
  }, [messages, sending]);

  const send = async (text) => {
    const trimmed = (text ?? input).trim();
    if (!trimmed || sending) return;

    setError(null);
    setSending(true);
    // Optimistically show the user's message immediately.
    setMessages((prev) => [...prev, { role: "USER", content: trimmed }]);
    setInput("");

    try {
      const res = await AiService.sendActivityChatMessage(activityId, trimmed);
      setMessages(res.data?.messages || []);
    } catch (err) {
      // Roll the optimistic message back and surface the failure.
      setMessages((prev) => prev.filter((m) => m.content !== trimmed || m.role !== "USER"));
      setError(t("aiChat.error"));
    } finally {
      setSending(false);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    send();
  };

  const handleClear = async () => {
    if (sending || messages.length === 0) return;
    try {
      await AiService.clearActivityChat(activityId);
      setMessages([]);
      setError(null);
    } catch (err) {
      setError(t("aiChat.error"));
    }
  };

  const showSuggestions = messages.length === 0 && !sending;

  return (
    <ChatSection data-testid="activity-ai-chat">
      <ChatHeader>
        <ChatTitle>
          <FiCpu aria-hidden="true" />
          {t("aiChat.title")}
        </ChatTitle>
        <ClearButton
          type="button"
          onClick={handleClear}
          disabled={sending || messages.length === 0}
          aria-label={t("aiChat.clear")}
          data-testid="ai-chat-clear"
        >
          <FiTrash2 aria-hidden="true" />
          {t("aiChat.clear")}
        </ClearButton>
      </ChatHeader>

      <MessageList ref={listRef}>
        {messages.length === 0 && !sending && (
          <EmptyState>{t("aiChat.empty")}</EmptyState>
        )}
        {messages.map((m, i) => (
          <Bubble key={i} $role={m.role} data-testid={`chat-bubble-${m.role}`}>
            {m.content}
          </Bubble>
        ))}
        {sending && (
          <TypingIndicator data-testid="chat-typing" aria-label={t("aiChat.thinking")}>
            <span />
            <span />
            <span />
          </TypingIndicator>
        )}
      </MessageList>

      {showSuggestions && (
        <SuggestionRow>
          <SuggestionChip type="button" disabled={sending} onClick={() => send(t("aiChat.suggestions.analyze"))}>
            {t("aiChat.suggestions.analyze")}
          </SuggestionChip>
          <SuggestionChip type="button" disabled={sending} onClick={() => send(t("aiChat.suggestions.pacing"))}>
            {t("aiChat.suggestions.pacing")}
          </SuggestionChip>
          <SuggestionChip type="button" disabled={sending} onClick={() => send(t("aiChat.suggestions.recovery"))}>
            {t("aiChat.suggestions.recovery")}
          </SuggestionChip>
        </SuggestionRow>
      )}

      <InputRow onSubmit={handleSubmit}>
        <ChatInput
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder={t("aiChat.placeholder")}
          disabled={sending}
          aria-label={t("aiChat.placeholder")}
        />
        <SendButton type="submit" disabled={sending || !input.trim()} aria-label={t("aiChat.send")}>
          <FiSend aria-hidden="true" />
        </SendButton>
      </InputRow>

      {error && <ErrorText role="alert">{error}</ErrorText>}
    </ChatSection>
  );
};

export default ActivityAiChat;
