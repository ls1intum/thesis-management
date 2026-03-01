package de.tum.cit.aet.thesis.utility;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.tum.cit.aet.thesis.exception.request.ResourceInvalidParametersException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class TemplateValidatorTest {

	@Nested
	class ValidContent {

		@ParameterizedTest
		@NullAndEmptySource
		void acceptsNullAndEmpty(String content) {
			assertDoesNotThrow(() -> TemplateValidator.validateTemplateContent(content));
		}

		@Test
		void acceptsPlainText() {
			assertDoesNotThrow(() -> TemplateValidator.validateTemplateContent(
					"Dear Student, your thesis has been approved."));
		}

		@Test
		void acceptsSafeThymeleafVariables() {
			assertDoesNotThrow(() -> TemplateValidator.validateTemplateContent(
					"Hello ${studentName}, your thesis \"${thesisTitle}\" is due on ${deadline}."));
		}

		@Test
		void acceptsTextContainingThread() {
			assertDoesNotThrow(() -> TemplateValidator.validateTemplateContent(
					"Please see the email thread below for details."));
		}

		@Test
		void acceptsTextContainingSystem() {
			assertDoesNotThrow(() -> TemplateValidator.validateTemplateContent(
					"The University System requires all students to register."));
		}

		@Test
		void acceptsHtmlContent() {
			assertDoesNotThrow(() -> TemplateValidator.validateTemplateContent(
					"<p>Dear ${name},</p><br/><p>Your thesis <b>${title}</b> has been submitted.</p>"));
		}
	}

	@Nested
	class StructuralPatterns {

		@Test
		void rejectsThymeleafPreprocessing() {
			assertThrows(ResourceInvalidParametersException.class,
					() -> TemplateValidator.validateTemplateContent("__${something}__"));
		}

		@Test
		void rejectsSpelTypeOperator() {
			assertThrows(ResourceInvalidParametersException.class,
					() -> TemplateValidator.validateTemplateContent("T(java.lang.Runtime)"));
		}

		@Test
		void rejectsSpelTypeOperatorWithSpace() {
			assertThrows(ResourceInvalidParametersException.class,
					() -> TemplateValidator.validateTemplateContent("T (java.lang.Runtime)"));
		}

		@Test
		void rejectsSpelConstructor() {
			assertThrows(ResourceInvalidParametersException.class,
					() -> TemplateValidator.validateTemplateContent("${new java.io.File('/')}"));
		}

		@Test
		void rejectsSpelBeanReference() {
			assertThrows(ResourceInvalidParametersException.class,
					() -> TemplateValidator.validateTemplateContent("#{@myBean.method()}"));
		}

		@Test
		void rejectsSpelUtilityObject() {
			assertThrows(ResourceInvalidParametersException.class,
					() -> TemplateValidator.validateTemplateContent("${#ctx.getEnvironment()}"));
		}
	}

	@Nested
	class ExpressionKeywords {

		@ParameterizedTest
		@ValueSource(strings = {
				"${getClass().forName('java.lang.Runtime')}",
				"${obj.getClass()}",
				"${Runtime.getRuntime().exec('cmd')}",
				"${T(java.lang.ProcessBuilder)}",
				"${Class.forName('java.lang.Runtime')}",
				"${something.exec('whoami')}",
				"${java.lang.Thread.sleep(5000)}",
				"${java.io.File('/')}",
				"${java.net.URL('http://evil')}",
				"${javax.script.ScriptEngine}",
				"${org.springframework.context}",
				"${obj.invoke(method)}",
				"${System.getenv('PATH')}",
				"${ClassLoader.getSystemClassLoader()}",
				"${java.util.Collections}",
				"${obj.getMethod('exec')}",
				"${obj.getDeclaredMethod('exec')}",
				"${com.sun.management.Something}",
				"${jdk.internal.Something}",
				"${ProcessHandle.current()}",
				"${obj.class.forName('Runtime')}",
				"${URLClassLoader.newInstance()}",
				"${ScriptEngine.eval('code')}",
				"${MethodHandle.invokeExact()}",
				"${java.lang.reflect.Method}",
				"${Thread.currentThread()}",
				"${Unsafe.getUnsafe()}"
		})
		void rejectsDangerousExpressions(String content) {
			assertThrows(ResourceInvalidParametersException.class,
					() -> TemplateValidator.validateTemplateContent(content));
		}

		@Test
		void rejectsCaseInsensitiveExpressions() {
			assertThrows(ResourceInvalidParametersException.class,
					() -> TemplateValidator.validateTemplateContent("${RUNTIME.getRuntime()}"));
		}
	}
}
