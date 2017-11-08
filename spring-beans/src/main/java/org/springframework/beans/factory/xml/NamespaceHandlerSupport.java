/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory.xml;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.Map;

/**
 * Support class for implementing custom {@link NamespaceHandler NamespaceHandlers}.
 * Parsing and decorating of individual {@link Node Nodes} is done via {@link BeanDefinitionParser}
 * and {@link BeanDefinitionDecorator} strategy interfaces, respectively.
 * <p>
 * <p>Provides the {@link #registerBeanDefinitionParser} and {@link #registerBeanDefinitionDecorator}
 * methods for registering a {@link BeanDefinitionParser} or {@link BeanDefinitionDecorator}
 * to handle a specific element.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see #registerBeanDefinitionParser(String, BeanDefinitionParser)
 * @see #registerBeanDefinitionDecorator(String, BeanDefinitionDecorator)
 * @since 2.0
 */
public abstract class NamespaceHandlerSupport implements NamespaceHandler {

    /**
     * Stores the {@link BeanDefinitionParser} implementations keyed by the
     * local name of the {@link Element Elements} they handle.
     */
    private final Map<String, BeanDefinitionParser> parsers =
            new HashMap<String, BeanDefinitionParser>();

    /**
     * Stores the {@link BeanDefinitionDecorator} implementations keyed by the
     * local name of the {@link Element Elements} they handle.
     */
    private final Map<String, BeanDefinitionDecorator> decorators =
            new HashMap<String, BeanDefinitionDecorator>();

    /**
     * Stores the {@link BeanDefinitionDecorator} implementations keyed by the local
     * name of the {@link Attr Attrs} they handle.
     */
    private final Map<String, BeanDefinitionDecorator> attributeDecorators =
            new HashMap<String, BeanDefinitionDecorator>();


    /**
     * Parses the supplied {@link Element} by delegating to the {@link BeanDefinitionParser} that is
     * registered for that {@link Element}.
     */
    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        // FIXME: 2017/11/8  查看该方法
        // FIXME: 2017/11/8 分析结果：通过 标签名字 element 获取到对应的标签解析类
        return findParserForElement(element, parserContext).parse(element, parserContext);
        /**
         * parse 方法分析：
         * 通过 findParserForElement(element, parserContext) 就获取了 标签对应的解析类
         * 例如：如果这时 component-scan 标签的解析类 ComponentScanBeanDefinitionParser 类，那么调用的 parse方法
         * 就是调用 component-scan 标签对应解析类的 parse 方法
         */
    }

    /**
     * Locates the {@link BeanDefinitionParser} from the register implementations using
     * the local name of the supplied {@link Element}.
     */
    private BeanDefinitionParser findParserForElement(Element element, ParserContext parserContext) {
        // FIXME: 2017/11/8 // 获得 标签的名字
        String localName = parserContext.getDelegate().getLocalName(element);
        /**
         * 从 this.parsers 也就是 从map 对象中 获取 标签的具体解析类，
         * 因为之前解析命名空间时，就已经将 命名空间下的标签解析类放入map中了，
         *
         * 所以这里可以通过 标签名字获取到对应的标签解析类
         */
        BeanDefinitionParser parser = this.parsers.get(localName);
        if (parser == null) {
            parserContext.getReaderContext().fatal(
                    "Cannot locate BeanDefinitionParser for element [" + localName + "]", element);
        }
        return parser;
    }

    /**
     * Decorates the supplied {@link Node} by delegating to the {@link BeanDefinitionDecorator} that
     * is registered to handle that {@link Node}.
     */
    @Override
    public BeanDefinitionHolder decorate(
            Node node, BeanDefinitionHolder definition, ParserContext parserContext) {

        return findDecoratorForNode(node, parserContext).decorate(node, definition, parserContext);
    }

    /**
     * Locates the {@link BeanDefinitionParser} from the register implementations using
     * the local name of the supplied {@link Node}. Supports both {@link Element Elements}
     * and {@link Attr Attrs}.
     */
    private BeanDefinitionDecorator findDecoratorForNode(Node node, ParserContext parserContext) {
        BeanDefinitionDecorator decorator = null;
        String localName = parserContext.getDelegate().getLocalName(node);
        if (node instanceof Element) {
            decorator = this.decorators.get(localName);
        } else if (node instanceof Attr) {
            decorator = this.attributeDecorators.get(localName);
        } else {
            parserContext.getReaderContext().fatal(
                    "Cannot decorate based on Nodes of type [" + node.getClass().getName() + "]", node);
        }
        if (decorator == null) {
            parserContext.getReaderContext().fatal("Cannot locate BeanDefinitionDecorator for " +
                    (node instanceof Element ? "element" : "attribute") + " [" + localName + "]", node);
        }
        return decorator;
    }


    /**
     * Subclasses can call this to register the supplied {@link BeanDefinitionParser} to
     * handle the specified element. The element name is the local (non-namespace qualified)
     * name.
     */
    protected final void registerBeanDefinitionParser(String elementName, BeanDefinitionParser parser) {
        this.parsers.put(elementName, parser);
    }

    /**
     * Subclasses can call this to register the supplied {@link BeanDefinitionDecorator} to
     * handle the specified element. The element name is the local (non-namespace qualified)
     * name.
     */
    protected final void registerBeanDefinitionDecorator(String elementName, BeanDefinitionDecorator dec) {
        this.decorators.put(elementName, dec);
    }

    /**
     * Subclasses can call this to register the supplied {@link BeanDefinitionDecorator} to
     * handle the specified attribute. The attribute name is the local (non-namespace qualified)
     * name.
     */
    protected final void registerBeanDefinitionDecoratorForAttribute(String attrName, BeanDefinitionDecorator dec) {
        this.attributeDecorators.put(attrName, dec);
    }

}
