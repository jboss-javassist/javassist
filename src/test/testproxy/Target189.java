package testproxy;

import javassist.util.proxy.MethodHandler;
import java.lang.reflect.Method;

public class Target189 {
	public interface TestProxy {
	}

	public static class TestMethodHandler implements MethodHandler {

		int invoked = 0;

		public Object invoke(Object self, Method thisMethod, Method proceed,
				Object[] args) throws Throwable {
			invoked++;
			return proceed.invoke(self, args);
		}

		public boolean wasInvokedOnce() {
			return invoked == 1;
		}

		public void reset() {
			invoked = 0;
		}
	}

	public static class Issue {

		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	public static class PublishedIssue extends Issue {
	}

	public static abstract class Article {
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public abstract Issue getIssue();
	}

	public static class PublishedArticle extends Article {

		private PublishedIssue issue;

		@Override
		public PublishedIssue getIssue() {
			return issue;
		}

		public void setIssue(PublishedIssue issue) {
			this.issue = issue;
		}

	}

}
