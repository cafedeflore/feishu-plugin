package io.jenkins.plugins;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import lombok.NoArgsConstructor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * 任务配置页面添加飞书配置
 */
@NoArgsConstructor
public class FeishuJobProperty extends JobProperty<Job<?, ?>> {

	private String sendUrlList;

	public String getSendUrlList() {
		return sendUrlList;
	}

	@DataBoundConstructor
	public FeishuJobProperty(String sendUrlList) {
		this.sendUrlList = sendUrlList;
	}

	@Extension
	public static class FeishuJobPropertyDescriptor extends JobPropertyDescriptor {

		@Override
		public boolean isApplicable(Class<? extends Job> jobType) {
			return super.isApplicable(jobType);
		}
	}
}
