package hudson.plugins.backlog;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.backlog.api.BacklogApiClient;
import hudson.plugins.backlog.api.entity.Issue;
import hudson.plugins.backlog.api.entity.Priority;
import hudson.plugins.backlog.api.entity.Project;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.MailSender;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.XmlRpcException;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Notifier that creates issue on Backlog project.
 * 
 * @author ikikko
 */
public class BacklogNotifier extends Notifier {

	private static final Log LOG = LogFactory.getLog(BacklogNotifier.class);

	// TODO move to projectProperty
	public final String projectId;

	public final String userId;

	public final String password;

	private static class MessageCreator extends MailSender {

		private AbstractBuild<?, ?> build;
		private BuildListener listener;

		public MimeMessage getMessage() throws MessagingException,
				InterruptedException {
			return getMail(build, listener);
		}

		public MessageCreator(AbstractBuild<?, ?> build, BuildListener listener) {
			super("", false, false); // dummy parameters
			this.build = build;
			this.listener = listener;
		}

	}

	@DataBoundConstructor
	public BacklogNotifier(String projectId, String userId, String password) {
		this.projectId = projectId;
		this.userId = userId;
		this.password = password;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {

		// FIXME 4 test
		// if (build.getResult() == Result.SUCCESS) {
		// return true;
		// }
		//
		// // notify when build is broken at first.
		// AbstractBuild<?, ?> pb = build.getPreviousBuild();
		// if (pb == null || pb.getResult() != Result.SUCCESS) {
		// return true;
		// }

		// FIXME modify space
		String space = "demo";

		try {
			BacklogApiClient client = new BacklogApiClient();
			// TODO error handling
			client.login(space, userId, password);

			Project project = client.getProject(projectId);
			MimeMessage message = new MessageCreator(build, listener)
					.getMessage();

			Issue newIssue = new Issue();
			newIssue.setSummary(message.getSubject());
			newIssue.setDescription(message.getContent().toString());
			newIssue.setPriority(Priority.HIGH);
			Issue issue = client.createIssue(project.getId(), newIssue);

			// TODO i18n
			listener.getLogger().println(
					"Created Issue is [" + issue.getKey() + "] : "
							+ issue.getUrl());
		} catch (XmlRpcException e) {
			e.printStackTrace(listener.error(e.getMessage()));
		} catch (MessagingException e) {
			e.printStackTrace(listener.error(e.getMessage()));
		}

		return true;
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Extension
	public static final class DescriptorImpl extends
			BuildStepDescriptor<Publisher> {

		@Override
		@SuppressWarnings("rawtypes")
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Create Issue on Backlog Project";
		}

	}

}
