package email_connector.implementation;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.core.CoreRuntimeException;
import com.mendix.datahub.connector.email.model.Message;
import com.mendix.datahub.connector.email.model.NotificationState;
import com.mendix.datahub.connector.email.service.NewEmailNotificationListener;
import com.mendix.datahub.connector.email.utils.EmailConnectorException;
import com.mendix.datahub.connector.eventtracking.Metrics;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import email_connector.proxies.EmailAccount;
import email_connector.proxies.constants.Constants;

import java.util.List;

import static com.mendix.core.Core.microflowCall;

public class NotificationListener implements NewEmailNotificationListener {

    private static final ILogNode log = Core.getLogger(Constants.getLogNode());
    private final String emailReceivedMicroflow;
    private final String subscriptionStateChangedMicroflow;
    private final EmailAccount emailAccount;

    public NotificationListener(String emailReceivedMicroflow, String subscriptionStateChangedMicroflow, EmailAccount emailAccount) {
        this.emailReceivedMicroflow = emailReceivedMicroflow;
        this.subscriptionStateChangedMicroflow = subscriptionStateChangedMicroflow;
        this.emailAccount = emailAccount;
    }

    @Override
    public void onEmailNotificationReceived(List<Message> messageList) {
        IContext context = null;
        try {
            Metrics.createCounter("dnl_connectors_ec_receive_email")
                    .addTag("email_protocol", this.emailAccount.getIncomingEmailConfiguration_EmailAccount().getIncomingProtocol().name())
                    .setDescription("App receives email")
                    .build()
                    .increment(messageList.size());
        } catch (CoreException e) {
            log.error(e);
        }


        try {
            context = Core.createSystemContext();
            context.startTransaction();
            var emails = MxMailMapper.mapEmails(emailAccount, messageList, context);
            emails = Core.commit(context, emails);
            context.endTransaction();
            microflowCall(emailReceivedMicroflow)
                    .withParam("EmailMessageList", emails)
                    .execute(context);
        } catch (CoreRuntimeException | EmailConnectorException | CoreException e) {
            log.error(e.getMessage(), e);
            if (context != null && context.isInTransaction())
                context.rollbackTransaction();
        }
    }

    @Override
    public void onEmailNotificationStateChanged(NotificationState notificationState, String comment) {
        microflowCall(subscriptionStateChangedMicroflow)
                .withParam("State", notificationState.name())
                .withParam("Comment", comment)
                .execute(Core.createSystemContext());
    }
}
