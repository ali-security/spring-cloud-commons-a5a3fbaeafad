/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.context.environment;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.springframework.cloud.context.environment.WritableEnvironmentEndpointWebExtension.VALID_KEYS_REGEX_PROPERTY;

public class WritableEnvironmentEndpointWebExtensionTests {

	@Test
	public void writeRejectedWhenValidKeysRegexNotSet() {
		MockEnvironment environment = new MockEnvironment();
		WritableEnvironmentEndpointWebExtension extension = extension(environment);

		assertThatIllegalArgumentException().isThrownBy(() -> extension.write("spring.datasource.url", "jdbc:evil"))
				.withMessageContaining("Invalid key spring.datasource.url");
		then(environment.getProperty("spring.datasource.url")).isNull();
	}

	@Test
	public void writeRejectedWhenValidKeysRegexBlank() {
		MockEnvironment environment = new MockEnvironment().withProperty(VALID_KEYS_REGEX_PROPERTY, "  ");
		WritableEnvironmentEndpointWebExtension extension = extension(environment);

		assertThatIllegalArgumentException().isThrownBy(() -> extension.write("message", "Foo"))
				.withMessageContaining("Invalid key message");
		then(environment.getProperty("message")).isNull();
	}

	@Test
	public void writeRejectedWhenKeyDoesNotFullyMatch() {
		MockEnvironment environment = new MockEnvironment().withProperty(VALID_KEYS_REGEX_PROPERTY, "message");
		WritableEnvironmentEndpointWebExtension extension = extension(environment);

		assertThatIllegalArgumentException().isThrownBy(() -> extension.write("xmessage", "Foo"))
				.withMessageContaining("Invalid key xmessage");
		assertThatIllegalArgumentException().isThrownBy(() -> extension.write("message.suffix", "Foo"))
				.withMessageContaining("Invalid key message.suffix");
		assertThatIllegalArgumentException().isThrownBy(() -> extension.write(VALID_KEYS_REGEX_PROPERTY, ".*"))
				.withMessageContaining("Invalid key " + VALID_KEYS_REGEX_PROPERTY);
		then(environment.getProperty("xmessage")).isNull();
		then(environment.getProperty("message.suffix")).isNull();
		then(environment.getProperty(VALID_KEYS_REGEX_PROPERTY)).isEqualTo("message");
	}

	@Test
	public void writeAllowedWhenKeyMatches() {
		MockEnvironment environment = new MockEnvironment().withProperty(VALID_KEYS_REGEX_PROPERTY, "message");
		WritableEnvironmentEndpointWebExtension extension = extension(environment);

		then(extension.write("message", "Foo")).isEqualTo(Collections.singletonMap("message", "Foo"));
		then(environment.getProperty("message")).isEqualTo("Foo");
	}

	private WritableEnvironmentEndpointWebExtension extension(MockEnvironment environment) {
		EnvironmentManager environmentManager = new EnvironmentManager(environment);
		environmentManager.setApplicationEventPublisher(mock(ApplicationEventPublisher.class));
		return new WritableEnvironmentEndpointWebExtension(new WritableEnvironmentEndpoint(environment),
				environmentManager);
	}

}
