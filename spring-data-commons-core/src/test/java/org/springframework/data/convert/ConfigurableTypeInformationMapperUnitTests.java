/*
 * Copyright 2011-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.convert;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

/**
 * Unit tests for {@link ConfigurableTypeMapper}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigurableTypeInformationMapperUnitTests<T extends PersistentProperty<T>> {

	ConfigurableTypeInformationMapper mapper;
	@Mock
	ApplicationContext context;

	@Before
	public void setUp() {
		mapper = new ConfigurableTypeInformationMapper(Collections.singletonMap(String.class, "1"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullTypeMap() {
		new ConfigurableTypeInformationMapper((Map<? extends Class<?>, String>) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullMappingContext() {
		new ConfigurableTypeInformationMapper((MappingContext<?, ?>) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNonBijectionalMap() {
		Map<Class<?>, String> map = new HashMap<Class<?>, String>();
		map.put(String.class, "1");
		map.put(Object.class, "1");

		new ConfigurableTypeInformationMapper(map);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void extractsAliasInfoFromMappingContext() {

		AbstractMappingContext<BasicPersistentEntity<Object, T>, T> mappingContext = new AbstractMappingContext<BasicPersistentEntity<Object, T>, T>() {

			@Override
			protected <S> BasicPersistentEntity<Object, T> createPersistentEntity(TypeInformation<S> typeInformation) {
				return (BasicPersistentEntity<Object, T>) new BasicPersistentEntity<S, T>(typeInformation);
			}

			@Override
			protected T createPersistentProperty(Field field, PropertyDescriptor descriptor,
					BasicPersistentEntity<Object, T> owner, SimpleTypeHolder simpleTypeHolder) {
				return (T) new AnnotationBasedPersistentProperty<T>(field, descriptor, owner, simpleTypeHolder) {
					@Override
					protected Association<T> createAssociation() {
						return null;
					}
				};
			}
		};

		ContextRefreshedEvent event = new ContextRefreshedEvent(context);

		mappingContext.setInitialEntitySet(Collections.singleton(Entity.class));
		mappingContext.setApplicationContext(context);
		mappingContext.onApplicationEvent(event);

		mapper = new ConfigurableTypeInformationMapper(mappingContext);

		assertThat(mapper.createAliasFor(ClassTypeInformation.from(Entity.class)), is((Object) "foo"));
	}

	@Test
	public void writesMapKeyForType() {

		assertThat(mapper.createAliasFor(ClassTypeInformation.from(String.class)), is((Object) "1"));
		assertThat(mapper.createAliasFor(ClassTypeInformation.from(Object.class)), is(nullValue()));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void readsTypeForMapKey() {

		assertThat(mapper.resolveTypeFrom("1"), is((TypeInformation) ClassTypeInformation.from(String.class)));
		assertThat(mapper.resolveTypeFrom("unmapped"), is(nullValue()));
	}

	@TypeAlias("foo")
	class Entity {

	}
}
