/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.context.annotation;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.type.filter.*;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Parser for the {@code <context:component-scan/>} element.
 *
 * @author Mark Fisher
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @since 2.5
 */
public class ComponentScanBeanDefinitionParser implements BeanDefinitionParser {

    private static final String BASE_PACKAGE_ATTRIBUTE = "base-package";

    private static final String RESOURCE_PATTERN_ATTRIBUTE = "resource-pattern";

    private static final String USE_DEFAULT_FILTERS_ATTRIBUTE = "use-default-filters";

    private static final String ANNOTATION_CONFIG_ATTRIBUTE = "annotation-config";

    private static final String NAME_GENERATOR_ATTRIBUTE = "name-generator";

    private static final String SCOPE_RESOLVER_ATTRIBUTE = "scope-resolver";

    private static final String SCOPED_PROXY_ATTRIBUTE = "scoped-proxy";

    private static final String EXCLUDE_FILTER_ELEMENT = "exclude-filter";

    private static final String INCLUDE_FILTER_ELEMENT = "include-filter";

    private static final String FILTER_TYPE_ATTRIBUTE = "type";

    private static final String FILTER_EXPRESSION_ATTRIBUTE = "expression";


    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        // FIXME: 2017/11/8 这里具体的标签为 <context:component-scan  base-package="com.consult.action" />
        // FIXME: 2017/11/8 获取标签的属性 base-package的值
        String basePackage = element.getAttribute(BASE_PACKAGE_ATTRIBUTE);
        basePackage = parserContext.getReaderContext().getEnvironment().resolvePlaceholders(basePackage);

        // FIXME: 2017/11/8 配置多个包是以 逗号分隔的，所以这里要将以逗号分隔的包转化为数组
        String[] basePackages = StringUtils.tokenizeToStringArray(basePackage,
                ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);

        // Actually scan for bean definitions and register them.
        // 扫描bean定义并注册它们

        // FIXME: 2017/11/8  查看该方法 ,spring 默认过滤器注册过程，也说是让 spring 支持 Annotation 注解
        ClassPathBeanDefinitionScanner scanner = configureScanner(parserContext, element);

        // FIXME: 2017/11/9  查看该方法
        Set<BeanDefinitionHolder> beanDefinitions = scanner.doScan(basePackages);

        //查看该方法
        registerComponents(parserContext.getReaderContext(), beanDefinitions, element);

        return null;
    }

    protected ClassPathBeanDefinitionScanner configureScanner(ParserContext parserContext, Element element) {
        // FIXME: 2017/11/8 使用默认的过滤器，默认值为 true
        boolean useDefaultFilters = true;
        // FIXME: 2017/11/8  当没有该属性 use-default-filters 时，就使用默认值，默认值为 true
        if (element.hasAttribute(USE_DEFAULT_FILTERS_ATTRIBUTE)) {
            useDefaultFilters = Boolean.valueOf(element.getAttribute(USE_DEFAULT_FILTERS_ATTRIBUTE));
        }

        // Delegate bean definition registration to scanner class.
        // FIXME: 2017/11/8 创建 扫描器，扫描包下的所有文件 ，同时也是spring @Controller、@Service 的注册支持，查看该方法
        ClassPathBeanDefinitionScanner scanner = createScanner(parserContext.getReaderContext(), useDefaultFilters);
        scanner.setResourceLoader(parserContext.getReaderContext().getResourceLoader());
        scanner.setEnvironment(parserContext.getReaderContext().getEnvironment());
        scanner.setBeanDefinitionDefaults(parserContext.getDelegate().getBeanDefinitionDefaults());
        scanner.setAutowireCandidatePatterns(parserContext.getDelegate().getAutowireCandidatePatterns());

        if (element.hasAttribute(RESOURCE_PATTERN_ATTRIBUTE)) {
            scanner.setResourcePattern(element.getAttribute(RESOURCE_PATTERN_ATTRIBUTE));
        }

        try {
            parseBeanNameGenerator(element, scanner);
        } catch (Exception ex) {
            parserContext.getReaderContext().error(ex.getMessage(), parserContext.extractSource(element), ex.getCause());
        }

        try {
            parseScope(element, scanner);
        } catch (Exception ex) {
            parserContext.getReaderContext().error(ex.getMessage(), parserContext.extractSource(element), ex.getCause());
        }

        /**
         * 当xml 配置文件中配置了 use-default-filters="false" 时就会获取 两个子元素 include-filter 和 exclude-filter
         *
         * 这个方法解析完毕后，就要回到 ComponentScanBeanDefinitionParser 的 parse 方法中
         */
        parseTypeFilters(element, scanner, parserContext);

        return scanner;
    }

    protected ClassPathBeanDefinitionScanner createScanner(XmlReaderContext readerContext, boolean useDefaultFilters) {
        // FIXME: 2017/11/9 查看该方法
        return new ClassPathBeanDefinitionScanner(readerContext.getRegistry(), useDefaultFilters);
    }

    protected void registerComponents(
            XmlReaderContext readerContext, Set<BeanDefinitionHolder> beanDefinitions, Element element) {

        Object source = readerContext.extractSource(element);
        CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(element.getTagName(), source);

        for (BeanDefinitionHolder beanDefHolder : beanDefinitions) {
            compositeDef.addNestedComponent(new BeanComponentDefinition(beanDefHolder));
        }

        // Register annotation config processors, if necessary.
        boolean annotationConfig = true;

        // FIXME: 2017/11/10  // 这里主要是与 annotation-config 标签的兼容,也就是说 配置了 component-scan 就不需要配置了annotation-config了         
        // FIXME: 2017/11/10  //如何做到兼容的。在 component-scan标签上可以配置 这个属性 annotation-config，这个属性默认为true
        if (element.hasAttribute(ANNOTATION_CONFIG_ATTRIBUTE)) {
            annotationConfig = Boolean.valueOf(element.getAttribute(ANNOTATION_CONFIG_ATTRIBUTE));
        }
        if (annotationConfig) {
            Set<BeanDefinitionHolder> processorDefinitions =
                    // 查看该方法，很重要
                    AnnotationConfigUtils.registerAnnotationConfigProcessors(readerContext.getRegistry(), source);
            for (BeanDefinitionHolder processorDefinition : processorDefinitions) {
                compositeDef.addNestedComponent(new BeanComponentDefinition(processorDefinition));
            }
        }

        readerContext.fireComponentRegistered(compositeDef);
    }

    protected void parseBeanNameGenerator(Element element, ClassPathBeanDefinitionScanner scanner) {
        if (element.hasAttribute(NAME_GENERATOR_ATTRIBUTE)) {
            BeanNameGenerator beanNameGenerator = (BeanNameGenerator) instantiateUserDefinedStrategy(
                    element.getAttribute(NAME_GENERATOR_ATTRIBUTE), BeanNameGenerator.class,
                    scanner.getResourceLoader().getClassLoader());
            scanner.setBeanNameGenerator(beanNameGenerator);
        }
    }

    protected void parseScope(Element element, ClassPathBeanDefinitionScanner scanner) {
        // Register ScopeMetadataResolver if class name provided.
        if (element.hasAttribute(SCOPE_RESOLVER_ATTRIBUTE)) {
            if (element.hasAttribute(SCOPED_PROXY_ATTRIBUTE)) {
                throw new IllegalArgumentException(
                        "Cannot define both 'scope-resolver' and 'scoped-proxy' on <component-scan> tag");
            }
            ScopeMetadataResolver scopeMetadataResolver = (ScopeMetadataResolver) instantiateUserDefinedStrategy(
                    element.getAttribute(SCOPE_RESOLVER_ATTRIBUTE), ScopeMetadataResolver.class,
                    scanner.getResourceLoader().getClassLoader());
            scanner.setScopeMetadataResolver(scopeMetadataResolver);
        }

        if (element.hasAttribute(SCOPED_PROXY_ATTRIBUTE)) {
            String mode = element.getAttribute(SCOPED_PROXY_ATTRIBUTE);
            if ("targetClass".equals(mode)) {
                scanner.setScopedProxyMode(ScopedProxyMode.TARGET_CLASS);
            } else if ("interfaces".equals(mode)) {
                scanner.setScopedProxyMode(ScopedProxyMode.INTERFACES);
            } else if ("no".equals(mode)) {
                scanner.setScopedProxyMode(ScopedProxyMode.NO);
            } else {
                throw new IllegalArgumentException("scoped-proxy only supports 'no', 'interfaces' and 'targetClass'");
            }
        }
    }

    protected void parseTypeFilters(Element element, ClassPathBeanDefinitionScanner scanner, ParserContext parserContext) {
        // Parse exclude and include filter elements.
        ClassLoader classLoader = scanner.getResourceLoader().getClassLoader();
        NodeList nodeList = element.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {

                // FIXME: 2017/11/9 //获取子元素的名子
                String localName = parserContext.getDelegate().getLocalName(node);
                try {
                    if (INCLUDE_FILTER_ELEMENT.equals(localName)) { // FIXME: 2017/11/9  //include-filter 元素的解析
                        TypeFilter typeFilter = createTypeFilter((Element) node, classLoader, parserContext);

                        // FIXME: 2017/11/9 // 将 该 type 属性类型加入到扫描器中
                        scanner.addIncludeFilter(typeFilter);
                    } else if (EXCLUDE_FILTER_ELEMENT.equals(localName)) { // FIXME: 2017/11/9 // exclude-filter 元素的解析
                        TypeFilter typeFilter = createTypeFilter((Element) node, classLoader, parserContext);
                        scanner.addExcludeFilter(typeFilter);
                    }
                } catch (Exception ex) {
                    parserContext.getReaderContext().error(
                            ex.getMessage(), parserContext.extractSource(element), ex.getCause());
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected TypeFilter createTypeFilter(Element element, ClassLoader classLoader, ParserContext parserContext) {
        String filterType = element.getAttribute(FILTER_TYPE_ATTRIBUTE);
        String expression = element.getAttribute(FILTER_EXPRESSION_ATTRIBUTE);
        expression = parserContext.getReaderContext().getEnvironment().resolvePlaceholders(expression);
        try {
            if ("annotation".equals(filterType)) {
                return new AnnotationTypeFilter((Class<Annotation>) classLoader.loadClass(expression));
            } else if ("assignable".equals(filterType)) {
                return new AssignableTypeFilter(classLoader.loadClass(expression));
            } else if ("aspectj".equals(filterType)) {
                return new AspectJTypeFilter(expression, classLoader);
            } else if ("regex".equals(filterType)) {
                return new RegexPatternTypeFilter(Pattern.compile(expression));
            } else if ("custom".equals(filterType)) {
                Class<?> filterClass = classLoader.loadClass(expression);
                if (!TypeFilter.class.isAssignableFrom(filterClass)) {
                    throw new IllegalArgumentException(
                            "Class is not assignable to [" + TypeFilter.class.getName() + "]: " + expression);
                }
                return (TypeFilter) BeanUtils.instantiateClass(filterClass);
            } else {
                throw new IllegalArgumentException("Unsupported filter type: " + filterType);
            }
        } catch (ClassNotFoundException ex) {
            throw new FatalBeanException("Type filter class not found: " + expression, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Object instantiateUserDefinedStrategy(String className, Class<?> strategyType, ClassLoader classLoader) {
        Object result;
        try {
            result = classLoader.loadClass(className).newInstance();
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException("Class [" + className + "] for strategy [" +
                    strategyType.getName() + "] not found", ex);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to instantiate class [" + className + "] for strategy [" +
                    strategyType.getName() + "]: a zero-argument constructor is required", ex);
        }

        if (!strategyType.isAssignableFrom(result.getClass())) {
            throw new IllegalArgumentException("Provided class name must be an implementation of " + strategyType);
        }
        return result;
    }

}
