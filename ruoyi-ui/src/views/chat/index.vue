<template>
  <div class="chat-page">
    <header class="header">
      <span class="dot" :class="{ active: loading }" />
      AI 助手
    </header>

    <div ref="msgBox" class="messages">
      <div v-for="(m, i) in messages" :key="i" :class="['row', m.role]">
        <div class="bubble">
          {{ m.content }}<span v-if="m.streaming" class="cursor" />
        </div>
      </div>
    </div>

    <footer class="footer">
      <div class="inputbar">
        <textarea
          v-model="input"
          rows="1"
          placeholder="输入消息，Enter 发送，Shift+Enter 换行"
          @keydown.enter.exact.prevent="send"
        />
        <button v-if="!loading" :disabled="!input.trim()" @click="send">
          发送
        </button>
        <button v-else class="stop" @click="stop">停止</button>
      </div>
      <p class="hint">Enter 发送 · Shift+Enter 换行</p>
    </footer>
  </div>
</template>

<script>
import request from "@/utils/request";
import { getToken } from "@/utils/auth";

// 与后端 concatWith(Flux.just("[DONE]")) 保持一致
const DONE_FLAG = "[DONE]";

export default {
  name: "Chat",
  data() {
    return {
      input: "",
      loading: false,
      abortCtl: null,
      messages: [
        {
          role: "ai",
          content: "你好！我是 AI 助手，有什么可以帮你？",
          streaming: false,
        },
      ],
    };
  },
  beforeDestroy() {
    this.stop();
  },
  methods: {
    scrollBottom() {
      this.$nextTick(() => {
        const box = this.$refs.msgBox;
        if (box) box.scrollTop = box.scrollHeight;
      });
    },

    buildHeaders() {
      const headers = { "Content-Type": "application/json" };
      const token = getToken();
      if (token) {
        // 若你的后端不是 Bearer 形式，照现有正常接口的请求头改
        headers.Authorization = "Bearer " + token;
      }
      return headers;
    },

    async send() {
      const text = this.input.trim();
      if (!text || this.loading) return;

      this.messages.push({ role: "user", content: text, streaming: false });
      this.input = "";
      this.loading = true;

      const aiMsg = { role: "ai", content: "", streaming: true };
      this.messages.push(aiMsg);
      this.scrollBottom();

      const controller = new AbortController();
      this.abortCtl = controller;

      const base = (request.defaults && request.defaults.baseURL) || "";
      let buffer = "";
      let finished = false;

      try {
        const res = await fetch(base + "/chat/stream", {
          method: "POST",
          headers: this.buildHeaders(),
          body: JSON.stringify({ message: text }),
          signal: controller.signal,
        });

        if (!res.ok) throw new Error("HTTP " + res.status);
        if (!res.body) throw new Error("响应不支持流式读取");

        const reader = res.body.getReader();
        const decoder = new TextDecoder("utf-8");

        while (!finished) {
          const { value, done } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });

          // 1) 先按 \n\n 切出完整事件帧
          let idx;
          while ((idx = buffer.indexOf("\n\n")) !== -1) {
            const frame = buffer.slice(0, idx);
            buffer = buffer.slice(idx + 2);

            // 2) 一个事件可能含多行，逐行判断，只取 data: 开头的行
            const lines = frame.split("\n");
            for (let k = 0; k < lines.length; k++) {
              const line = lines[k];
              if (!line) continue;

              // 兼容 "data:xxx" 与 "data: xxx" 两种写法
              let chunk;
              if (line.startsWith("data:")) {
                chunk = line.slice(5);
                if (chunk.startsWith(" ")) chunk = chunk.slice(1); // 只去掉一个空格，保留内容缩进
              } else {
                continue; // id: / event: / 其他行，忽略
              }

              // 3) 结束哨兵：立即收尾
              if (chunk.trim() === DONE_FLAG) {
                finished = true;
                break;
              }
              if (chunk) {
                aiMsg.content += chunk;
              }
            }
            if (finished) break;
          }

          this.scrollBottom();
        }

        try {
          reader.releaseLock();
        } catch (e) {
          /* ignore */
        }
      } catch (err) {
        const canceled = err && err.name === "AbortError";
        if (!canceled && !aiMsg.content) {
          aiMsg.content = "请求失败：" + (err.message || err);
        }
      } finally {
        aiMsg.streaming = false;
        this.loading = false;
        this.abortCtl = null;
        this.scrollBottom();
      }
    },

    stop() {
      if (this.abortCtl) {
        this.abortCtl.abort();
        this.abortCtl = null;
      }
    },
  },
};
</script>

<style scoped>
.chat-page {
  height: calc(
    100vh - 84px
  ); /* 减去若依 Layout 的 navbar(50) + tags-view(34) */
  display: flex;
  flex-direction: column;
  max-width: 860px;
  margin: 0 auto;
  color: #e8eaf0;
  background: #0f1115;
  border-radius: 4px;
}
.header {
  padding: 16px 24px;
  font-size: 17px;
  font-weight: 600;
  border-bottom: 1px solid #262a35;
  display: flex;
  align-items: center;
  gap: 10px;
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 8px #34d399;
}
.dot.active {
  background: #f5a623;
  box-shadow: 0 0 8px #f5a623;
}
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}
.row {
  display: flex;
  margin-bottom: 20px;
}
.row.user {
  justify-content: flex-end;
}
.bubble {
  max-width: 78%;
  padding: 12px 16px;
  border-radius: 14px;
  line-height: 1.7;
  font-size: 15px;
  white-space: pre-wrap;
  word-break: break-word;
}
.row.ai .bubble {
  background: #1c2029;
  border: 1px solid #2a2f3c;
  border-top-left-radius: 4px;
}
.row.user .bubble {
  background: #3b6ef0;
  border-top-right-radius: 4px;
}
.cursor {
  display: inline-block;
  width: 8px;
  height: 16px;
  background: #7aa2ff;
  vertical-align: -2px;
  animation: blink 0.8s infinite;
  margin-left: 2px;
}
@keyframes blink {
  50% {
    opacity: 0;
  }
}
.footer {
  padding: 16px 24px 20px;
  border-top: 1px solid #262a35;
}
.inputbar {
  display: flex;
  gap: 12px;
  background: #1c2029;
  border: 1px solid #2a2f3c;
  border-radius: 14px;
  padding: 8px 8px 8px 18px;
}
textarea {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  resize: none;
  color: #e8eaf0;
  font-size: 15px;
  font-family: inherit;
  line-height: 1.6;
  max-height: 160px;
}
button {
  background: #3b6ef0;
  color: #fff;
  border: none;
  border-radius: 10px;
  padding: 0 22px;
  font-size: 15px;
  cursor: pointer;
  transition: 0.15s;
}
button:hover:not(:disabled) {
  background: #4d7dff;
}
button:disabled {
  background: #2a2f3c;
  color: #6b7280;
  cursor: not-allowed;
}
button.stop {
  background: #c0392b;
}
button.stop:hover {
  background: #e74c3c;
}
.hint {
  text-align: center;
  color: #565d6e;
  font-size: 12px;
  margin-top: 10px;
}
</style>
