package com.ruoyi.web.controller.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class AgentTools {

    private Consumer<String> onToolStart;
    private Consumer<String> onToolEnd;

    /** 每次请求前由 Controller 注入回调，用完置空 */
    public void setCallbacks(Consumer<String> onStart, Consumer<String> onEnd) {
        this.onToolStart = onStart;
        this.onToolEnd = onEnd;
    }

    public void clearCallbacks() {
        this.onToolStart = null;
        this.onToolEnd = null;
    }

    @Tool(description = "获取当前日期和时间")
    public String getCurrentDateTime() {
        notify("获取当前时间");
        return java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss EEEE"));
    }

    @Tool(description = "查询指定城市的实时天气")
    public String getWeather(@ToolParam(description = "城市名称") String city) {
        notify("查询 " + city + " 的天气");
        // TODO: 接真实天气接口，这里先返回模拟值
        return city + " 今天晴，25℃";
    }

    @Tool(description = "执行数学计算")
    public String calculate(@ToolParam(description = "数学表达式，如 3*7+5") String expression) {
        notify("计算 " + expression);
        try {
            // 简单安全计算，只允许数字和 +-*/()
            if (!expression.matches("[\\d+\\-*/().\\s]+")) {
                return "表达式不合法";
            }
            javax.script.ScriptEngine engine = new javax.script.ScriptEngineManager()
                    .getEngineByName("JavaScript");
            Object result = engine.eval(expression);
            return String.valueOf(result);
        } catch (Exception e) {
            return "计算失败: " + e.getMessage();
        }
    }

    @Tool(description = "联网搜索实时信息")
    public String webSearch(@ToolParam(description = "搜索关键词") String query) {
        notify("搜索 \"" + query + "\"");
        // TODO: 接真实搜索 API
        return "（搜索功能待接入，关键词：" + query + "）";
    }

    private void notify(String desc) {
        if (onToolStart != null) onToolStart.accept(desc);
        if (onToolEnd != null) onToolEnd.accept(desc);
    }
}