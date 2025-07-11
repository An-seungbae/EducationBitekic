package email_connector.implementation;

import com.mendix.core.Core;
import com.mendix.datahub.connector.email.service.IMendixWorker;
import com.mendix.datahub.connector.email.utils.EmailConnectorException;
import com.mendix.datahub.connector.email.utils.Error;
import email_connector.proxies.EmailAccount;
import email_connector.proxies.microflows.Microflows;

public class OAuthTokenWorker implements IMendixWorker<String> {
    EmailAccount emailAccount;

    public OAuthTokenWorker(EmailAccount emailAccount) {
        this.emailAccount = emailAccount;
    }

    @Override
    public String doWork(Object... params) throws EmailConnectorException {
        if (emailAccount == null)
            throw new EmailConnectorException(Error.EMPTY_EMAIL_ACCOUNT.getMessage());
        var context = Core.createSystemContext();
        return Microflows.aCT_EmailAccount_GetOrRenewTokenJavaAction(context, emailAccount);
    }
}
