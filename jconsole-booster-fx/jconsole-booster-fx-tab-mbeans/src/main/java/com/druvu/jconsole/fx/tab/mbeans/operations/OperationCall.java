package com.druvu.jconsole.fx.tab.mbeans.operations;

import com.druvu.jconsole.core.text.Messages;
import com.druvu.jconsole.fx.tab.mbeans.common.TabServices;
import com.druvu.jconsole.fx.tab.mbeans.viewers.ValueWindow;
import java.util.Optional;
import javax.management.MBeanOperationInfo;
import javax.management.ObjectName;

/**
 * Invokes an operation off the FX thread and shows the outcome: a notice for a void operation, the return value in a
 * window, a returned file through the download prompt, or the MBean's own error.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class OperationCall {

    private final TabServices services;
    private final ObjectName name;

    OperationCall(TabServices services, ObjectName name) {
        this.services = services;
        this.name = name;
    }

    void invoke(OperationForm form) {
        MBeanOperationInfo operation = form.operation();
        String[] signature = form.signature();
        Object[] entered = form.entered();
        services.load(
                "Invoke " + operation.getName() + " on " + name,
                () -> services.access()
                        .invoke(name, operation.getName(), ParameterValues.convert(signature, entered), signature),
                result -> {
                    form.remember();
                    show(operation, result);
                },
                Messages.PROBLEM_INVOKING + " " + operation.getName());
    }

    private void show(MBeanOperationInfo operation, Object result) {
        if (isVoid(operation.getReturnType())) {
            services.host().notice(Messages.METHOD_SUCCESSFULLY_INVOKED);
            return;
        }
        if (result instanceof byte[] bytes) {
            Optional<String> mime = MimeRules.mimeOf(operation.getDescription());
            if (mime.isPresent()) {
                ReturnedFile.offer(services, operation, mime.get(), bytes);
                return;
            }
        }
        ValueWindow.show(services.host(), Messages.OPERATION_RETURN_VALUE, result);
    }

    static boolean isVoid(String returnType) {
        return returnType == null
                || "void".equals(returnType)
                || Void.class.getName().equals(returnType);
    }
}
