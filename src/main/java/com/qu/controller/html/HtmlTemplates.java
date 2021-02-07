package com.qu.controller.html;

import com.qu.services.mail.params.AdminInviteParameters;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.api.CheckedTemplate;

public class HtmlTemplates {
    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance AdminRegistration(String token);
        public static native TemplateInstance AdminRegistrationSuccess(String userId);
        public static native TemplateInstance AdminRegistrationFail(String msg);
    }
}
