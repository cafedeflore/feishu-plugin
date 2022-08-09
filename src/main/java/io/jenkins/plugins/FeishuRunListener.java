package io.jenkins.plugins;

import edu.umd.cs.findbugs.annotations.NonNull;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Job;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import lombok.extern.log4j.Log4j;

/**
 * 所有项目触发
 */
@Log4j
@Extension
public class FeishuRunListener extends RunListener<AbstractBuild<?, ?>> {

  @Override
  public void onStarted(AbstractBuild<?, ?> run, TaskListener listener) {
    Job<?, ?> job = run.getParent();
    FeishuJobProperty property = job.getProperty(FeishuJobProperty.class);
    if (property == null) {
      return;
    }
    FeishuService service = new FeishuServiceImpl(property.getSendUrlList(), listener, run);
    service.sendMsg(service.buildFeishuMsg());
  }

  @Override
  public void onCompleted(AbstractBuild<?, ?> run, @NonNull TaskListener listener) {
    Job<?, ?> job = run.getParent();
    FeishuJobProperty property = job.getProperty(FeishuJobProperty.class);
    if (property == null) {
      return;
    }
    FeishuService service = new FeishuServiceImpl(property.getSendUrlList(), listener, run);
    service.sendMsg(service.buildFeishuMsg());
  }
}
