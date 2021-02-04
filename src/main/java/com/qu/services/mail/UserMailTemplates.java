package com.qu.services.mail;

import com.qu.services.mail.params.AdminInviteParameters;
import io.quarkus.mailer.MailTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.api.CheckedTemplate;

public class UserMailTemplates {
    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance adminInvite(AdminInviteParameters params);
    }
}
