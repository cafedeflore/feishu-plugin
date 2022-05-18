package io.jenkins.plugins;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.model.Cause.UserIdCause;
import jenkins.model.Jenkins;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder.Default;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class FeishuServiceImpl implements FeishuService {

    private Logger logger = LoggerFactory.getLogger(FeishuService.class);

    private TaskListener listener;

    private AbstractBuild build;

    private String[] urlList;

    public FeishuServiceImpl(String sendUrlList, TaskListener listener, AbstractBuild build) {
        this.listener = listener;
        this.build = build;
        this.urlList = parseSendList(sendUrlList);
    }

    public static final MediaType JSON_FORMAT = MediaType.get("application/json; charset=utf-8");

    OkHttpClient client = new OkHttpClient();

    String post(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(json, JSON_FORMAT);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }

    private String[] parseSendList(String sendList) {
        String[] urlLis = null;
        try {
            urlLis = sendList.split(";");
        } catch (Exception e) {
            logger.error("parse list error", e);
        }
        return urlLis;
    }

    private String getBuildUrl() {
        String getRootUrl = getDefaultURL();
        if (getRootUrl.endsWith("/")) {
            return getRootUrl + build.getUrl();
        } else {
            return getRootUrl + "/" + build.getUrl();
        }
    }

    private String getJobUrl() {
        String getRootUrl = getDefaultURL();
        if (getRootUrl.endsWith("/")) {
            return getRootUrl + build.getProject().getUrl();
        } else {
            return getRootUrl + "/" + build.getProject().getUrl();
        }
    }

    private String getDefaultURL() {
        Jenkins instance = Jenkins.get();
        return instance.getRootUrl() != null ? instance.getRootUrl() : "http://127.0.0.1:8080/jenkins";
    }

    @Data
    @Builder
    public static class Btn {
        @Default
        private String tag = "button";
        @Default
        private String type = "primary";
        private Text text;
        private String url;
    }

    @Data
    @Builder
    public static class Text {
        @Default
        private String tag = "plain_text";
        private String content;
    }

    @Data
    @Builder
    public static class MarkdownElement {
        @Default
        private String tag = "markdown";
        private String content;
    }

    @Data
    @Builder
    public static class ActionElement {
        @Default
        private String tag = "action";
        private ArrayList<Btn> actions;
    }

    @Data
    @Builder
    public static class Card {
        private ArrayList elements;
    }

    @Data
    @Builder
    public static class Message {
        @Default
        private String msg_type = "interactive";
        private Card card;
    }

    private String getUser(Run<?, ?> run, TaskListener listener) {
        UserIdCause userIdCause = run.getCause(UserIdCause.class);
        // 执行人信息
        User user = null;
        String executorName;
        if (userIdCause != null && userIdCause.getUserId() != null) {
            user = User.getById(userIdCause.getUserId(), false);
        }

        if (user == null) {
            executorName = run.getCauses().stream().map(Cause::getShortDescription)
                    .collect(Collectors.joining());
        } else {
            executorName = user.getDisplayName();
        }
        return executorName;
    }

    public String buildFeishuMsg() {
        String buildStatus = "";
        if (build.getResult() != null) {
            switch (build.getResult().toString()) {
                case "SUCCESS":
                    buildStatus = "成功";
                    break;
                case "FAILURE":
                    buildStatus = "失败";
                    break;
                case "ABORTED":
                    buildStatus = "中断";
                    break;
                case "UNSTABLE":
                    buildStatus = "不稳定";
                    break;
                default:
                    buildStatus = "未知";
            }
        } else {
            buildStatus = "开始";
        }
        String content = String.join("\n", Arrays.asList(
                String.format("# [%s](%s)", build.getProject().getName(), getJobUrl()),
                "---",
                String.format("- 任务：[%s](%s)", build.getDisplayName(), getBuildUrl()),
                String.format("- 状态：%s", buildStatus),
                String.format("- 持续时间：%s", build.getDurationString()),
                String.format("- 执行人：%s", getUser(build, listener))));

        logger.info("content: {}", content);

        ArrayList<Btn> actions = new ArrayList<>();
        actions.add(
                Btn.builder().text(Text.builder().content("更改记录").build()).url(getBuildUrl() + "changes").build());
        actions.add(
                Btn.builder().text(Text.builder().content("控制台").build()).url(getBuildUrl() + "console").build());
        ArrayList elements = new ArrayList();
        elements.add(MarkdownElement.builder().content(content).build());
        elements.add(ActionElement.builder().actions(actions).build());
        Message message = Message.builder().card(Card.builder().elements(elements).build()).build();
        logger.info(JSON.toJSONString(message));
        return JSON.toJSONString(message);
    }

    public void sendMsg(String msg) {
        if (urlList == null) {
            return;
        }

        for (String url : urlList) {
            if (!url.trim().isEmpty()) {
                try {
                    this.post(url, msg);
                } catch (IOException e) {
                    logger.error("send msg error", e);
                }
            }
        }
    }

}
