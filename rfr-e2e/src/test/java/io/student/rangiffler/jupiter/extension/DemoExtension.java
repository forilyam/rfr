package io.student.rangiffler.jupiter.extension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterClassTemplateInvocationCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeClassTemplateInvocationCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.PreInterruptCallback;
import org.junit.jupiter.api.extension.PreInterruptContext;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstancePreConstructCallback;
import org.junit.jupiter.api.extension.TestInstancePreDestroyCallback;

public final class DemoExtension implements
        BeforeAllCallback,
        BeforeClassTemplateInvocationCallback,
        TestInstancePreConstructCallback,
        BeforeEachCallback,
        BeforeTestExecutionCallback,
        AfterTestExecutionCallback,
        AfterEachCallback,
        TestInstancePreDestroyCallback,
        AfterClassTemplateInvocationCallback,
        AfterAllCallback,
        PreInterruptCallback,
        InvocationInterceptor,
        ParameterResolver {
    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static final Set<String> SHOWN = ConcurrentHashMap.newKeySet();

    private final int id;
    private final AtomicBoolean announced;

    public DemoExtension() {
        this(COUNTER.incrementAndGet(), new AtomicBoolean());
    }

    private DemoExtension(int id, AtomicBoolean announced) {
        this.id = id;
        this.announced = announced;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        log("beforeAll", context);
    }

    @Override
    public void beforeClassTemplateInvocation(ExtensionContext context) {
        log("beforeClassTemplateInvocation", context);
    }

    @Override
    public void preConstructTestInstance(TestInstanceFactoryContext factory, ExtensionContext context) {
        log("preConstructTestInstance", context);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        log("beforeEach", context);
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        log("beforeTestExecution", context);
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        log("afterTestExecution", context);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        log("afterEach", context);
    }

    @Override
    public void preDestroyTestInstance(ExtensionContext context) {
        log("preDestroyTestInstance", context);
    }

    @Override
    public void afterClassTemplateInvocation(ExtensionContext context) {
        log("afterClassTemplateInvocation", context);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        log("afterAll", context);
    }

    @Override
    public void beforeThreadInterrupt(PreInterruptContext interrupt, ExtensionContext context) {
        log("beforeThreadInterrupt", context);
    }

    @Override
    public <T> T interceptTestClassConstructor(
            Invocation<T> invocation,
            ReflectiveInvocationContext<Constructor<T>> invocationContext,
            ExtensionContext context
    ) throws Throwable {
        log("▶ constructor " + invocationContext.getExecutable().getDeclaringClass().getSimpleName(), context);
        return invocation.proceed();
    }

    @Override
    public void interceptBeforeAllMethod(
            Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext context
    ) throws Throwable {
        log("▶ @BeforeAll " + invocationContext.getExecutable().getName(), context);
        invocation.proceed();
    }

    @Override
    public void interceptBeforeEachMethod(
            Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext context
    ) throws Throwable {
        log("▶ @BeforeEach " + invocationContext.getExecutable().getName(), context);
        invocation.proceed();
    }

    @Override
    public void interceptTestMethod(
            Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext context
    ) throws Throwable {
        log("▶ @Test " + invocationContext.getExecutable().getName(), context);
        invocation.proceed();
    }

    @Override
    public void interceptAfterEachMethod(
            Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext context
    ) throws Throwable {
        log("▶ @AfterEach " + invocationContext.getExecutable().getName(), context);
        invocation.proceed();
    }

    @Override
    public void interceptAfterAllMethod(
            Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext context
    ) throws Throwable {
        log("▶ @AfterAll " + invocationContext.getExecutable().getName(), context);
        invocation.proceed();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
        return parameter.getParameter().getType() == String.class;
    }

    @Override
    public String resolveParameter(ParameterContext parameter, ExtensionContext context) {
        log("resolveParameter " + parameter.getDeclaringExecutable().getName() + "#" + parameter.getIndex(), context);
        return "resolved by DemoExtension";
    }

    private void log(String callback, ExtensionContext context) {
        synchronized (DemoExtension.class) {
            Deque<ExtensionContext> chain = new ArrayDeque<>();
            for (ExtensionContext node = context; node.getParent().isPresent(); node = node.getParent().get()) {
                chain.push(node);
            }
            int depth = 0;
            for (ExtensionContext node : chain) {
                if (SHOWN.add(node.getUniqueId())) {
                    System.out.printf("%s▸ %s%n", "  ".repeat(depth), node.getDisplayName());
                }
                depth++;
            }
            if (announced.compareAndSet(false, true)) {
                System.out.printf("%s• DemoExtension#%d created%n", "  ".repeat(depth), id);
            }
            System.out.printf("%s· DemoExtension#%d %s%n", "  ".repeat(depth), id, callback);
        }
    }
}